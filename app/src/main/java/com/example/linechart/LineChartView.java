package com.example.linechart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

@SuppressLint("DrawAllocation")
public class LineChartView extends View {
    private static float ACCEPTED_FINGER_DIFF = 120f;
    private static float SCROLL_SPEED = 10f; //Lower value equals faster scroll
    private static float LINE_INFO_SPACING = 35f;
    int LINE_LENGTH = 50;
    int LINE_SPACEING = 20;
    ArrayList<Line> Lines = new ArrayList<Line>();
    ArrayList<MarkedData> markedData = null;
    Paint GridPaint = new Paint();
    Paint markerColor = new Paint();
    Paint TouchMarkerPaint = new Paint();
    Paint dummyBitmapPaint = new Paint();
    Paint TextPaint = new Paint();
    SelectionOrientation selOri = SelectionOrientation.Horizontal;
    GestureDetector gestureDetector;
    //The layers we are going to draw
    Bitmap lines;
    Bitmap grid;
    Bitmap info;
    boolean drawGrid = false;
    boolean hasDataChanged = false;
    boolean isTouching = false;
    boolean hasShadow = false;
    boolean isZoomed = false;
    boolean isMultiTouching = false;
    int zoomStart = 0;
    int zoomStop = 0;
    float verticalPos = 0.0f;
    float secVerticalPos = 0.0f;
    float horizontalPos = 0.0f;
    @SuppressWarnings("unused")
    private String TAG = "LineViewChart";
    private OnDataPointMarkedListener mListener;
    private OnInfoNeedParseListener mParserList;
    private float SCROLL_START_POS = 0f;
    private boolean SCROLL_ENABLE = false;
    private boolean INFO_VISIBLE = false;
    private boolean markedUpdated = false;
    private float highestMultiplication = 1f;
    private float topLineLength = 0; //Datapoint count of the line with the most datapoints.
    /**
     * A simple load-animation that lets the chart "grow" from a straight line.
     * Just for testing, noticed that it's terrible for lines with like 1000+ datapoints.
     */
    Runnable loadAnim = new Runnable() {

        @Override
        public void run() {
            highestMultiplication -= 1f;

            if (topLineLength > 500) {
                highestMultiplication = 1f;
            }

            if (highestMultiplication < 1.1f) { //Error correction. We don't want to get stuck in this loop.
                highestMultiplication = 1f;
            }

            if (highestMultiplication > 1f)
                postDelayed(this, 25); //Restarts the runnable with a given delay, kind of like a loop that doesn't block the ui-thread.

            hasDataChanged = true; //Need to flag the data as changes for the view to actually redraw it.
            invalidate();
        }

    };
    private float pointsXDistance = 0f; //The distance between each point
    private float pointXOffset = 0f;

    public LineChartView(Context context) {
        super(context);
        GridPaint.setColor(Color.BLACK);
        TouchMarkerPaint.setColor(Color.DKGRAY);
        TouchMarkerPaint.setStrokeWidth(Dip(1));
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public LineChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        GridPaint.setColor(Color.BLACK);
        TouchMarkerPaint.setColor(Color.DKGRAY);
        TouchMarkerPaint.setStrokeWidth(Dip(1));

        TextPaint.setColor(Color.BLACK);
        TextPaint.setStyle(Paint.Style.FILL);
        TextPaint.setStrokeWidth(Dip(1));
        TextPaint.setAntiAlias(true);
        TextPaint.setTextSize(Dip(11));

        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public OnDataPointMarkedListener getOnDataPointMarkedListener() {
        return this.mListener;
    }

    public void setOnDataPointMarkedListener(OnDataPointMarkedListener listener) {
        this.mListener = listener;
    }

    public OnInfoNeedParseListener getOnInfoNeedParseListener() {
        return this.mParserList;
    }

    public void setOnInfoNeedParseListener(OnInfoNeedParseListener list) {
        this.mParserList = list;
    }

    public void addShadow() {
        if (Build.VERSION.SDK_INT >= 11) {
            this.setLayerType(View.LAYER_TYPE_SOFTWARE, markerColor);
            //this.setLayerType(View.LAYER_TYPE_SOFTWARE, GridPaint);
            if (Lines != null)
                for (Line l : Lines) {
                    this.setLayerType(View.LAYER_TYPE_SOFTWARE, l.getColor());
                    l.getColor().setShadowLayer(5.0f, 0.0f, 2.0f, Color.DKGRAY);
                }
        }
        //GridPaint.setShadowLayer(2.0f, 3.0f, 3.0f, Color.GRAY);
        markerColor.setShadowLayer(7.0f, 1.0f, 2.0f, Color.BLACK);
        hasDataChanged = true;
        hasShadow = true;
        invalidate();
    }

    public void removeShadow() {
        if (Build.VERSION.SDK_INT >= 11) {
            this.setLayerType(View.LAYER_TYPE_SOFTWARE, markerColor);
            this.setLayerType(View.LAYER_TYPE_SOFTWARE, GridPaint);
            if (Lines != null)
                for (Line l : Lines) {
                    this.setLayerType(View.LAYER_TYPE_SOFTWARE, l.getColor());
                    l.getColor().setShadowLayer(0.0f, 0.0f, 2.0f, Color.DKGRAY);
                }
        }
        GridPaint.setShadowLayer(0.0f, 3.0f, 3.0f, Color.GRAY);
        markerColor.setShadowLayer(0.0f, 1.0f, 2.0f, Color.BLACK);
        hasDataChanged = true;
        hasShadow = false;
        invalidate();
    }

    public void showInfo(boolean value) {
        this.INFO_VISIBLE = value;
        hasDataChanged = true;
        invalidate();
    }

    public boolean isInfoVisible() {
        return this.INFO_VISIBLE;
    }

    /**
     * Gets the current orientation of datapoint selection
     *
     * @return Returns SelectionOrientation.Horizontal or SelectionOrientation.Vertical
     */
    public SelectionOrientation getSelectionOrientation() {
        return selOri;
    }

    /**
     * Set the orientation of datapoint selection
     * Let's you choose in what orientation you want to be able to select datapoints.
     * <p>
     * <p>
     * Default is horizontal
     *
     * @param so SelectionOrientation.Horizontal or SelectionOrientation.Vertical
     */
    public void setSelectionOrientation(SelectionOrientation so) {
        selOri = so;
    }

    private float Dip(float value) {
        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, r.getDisplayMetrics());
        return px;

    }

    private void DrawDataPointMarker(float CenterX, float CenterY, Canvas canvas, Paint centerColor) {
        markerColor.setAntiAlias(true);
        markerColor.setStyle(Paint.Style.FILL_AND_STROKE);
        markerColor.setColor(Color.BLACK);
        //    canvas.drawCircle(CenterX + 1.0F, CenterY + 1.0F, Dip(4), markerColor);
        canvas.drawCircle(CenterX + pointXOffset, CenterY, Dip(4), centerColor);
    }

    /**
     * Get if there is a point marker at the supplied X-cordinate
     *
     * @param line      Line object cointaining the points
     * @param xDistance The calculated distance between each point on the X-axes
     * @param h         Height of the view, with own margin added
     * @param highest   The highest value of any given datapoint
     * @return Returns true of there is a marker at that x-cordinate, else false
     */
    private boolean hasMarker(Line line, float xDistance, int h, float highest) {
        int forTo = isZoomed ? (line.getPoints().size() >= (zoomStop + 1) ? zoomStop + 1 : line.getPoints().size()) : line.getPoints().size();
        int forStart = isZoomed ? (line.getPoints().size() >= zoomStart ? (zoomStart > 0 ? zoomStart - 1 : 0) : 0) : 0;

        for (int i = forStart; i < forTo; i++) {
            if (selOri == SelectionOrientation.Vertical) {
                if ((xDistance * (isZoomed ? i - zoomStart : i)) < (verticalPos + Dip(1f)) && (xDistance * (isZoomed ? i - zoomStart : i)) > (verticalPos - Dip(1f))) {
                    return true;
                }
            } else if (selOri == SelectionOrientation.Horizontal) {
                if ((((line.getPoints().get(i).getPoint() * h) / highest) < (horizontalPos + Dip(2f))) && (((line.getPoints().get(i).getPoint() * h) / highest) > (horizontalPos - Dip(2f)))) {
                    return true;
                }
            }

        }

        return false;
    }

    private void getSelectedZoom(float xDistance, float maxPoints) {
        if (secVerticalPos == -1)
            return; //We've single-touched the screen. So, no zooming going on.

        //First, we need to get our most right and most left touch points
        float right = verticalPos > secVerticalPos ? verticalPos : secVerticalPos;
        float left = verticalPos < secVerticalPos ? verticalPos : secVerticalPos;
        int tempStart = 0;
        int tempStop = 0;

        //Now that we've fixed that. Then it's time to find closes datapoint so we can zoom in on them
        for (int i = 0; i < maxPoints; i++) {
            if (left >= ((xDistance * i) + (xDistance * 0.1f)) && left <= ((xDistance * i + 1) + (xDistance * 0.1f))) {
                tempStart = zoomStart + i + 1;
            } else if (left <= ((xDistance * i) + (xDistance * 0.1f)) && left >= ((xDistance * i) - (xDistance * 0.9f))) {
                tempStart = zoomStart + i;
            }


            if (right >= ((xDistance * i) - (xDistance * 0.1f)) && right <= ((xDistance * i) + (xDistance * 0.9f))) {
                tempStop = zoomStart + i;
            } else if (right > ((xDistance * i) + (xDistance * 0.9f)) && right < ((xDistance * (i + 1) - (xDistance * 0.1f)))) {
                tempStop = zoomStart + i + 1;
            }
        }
        if (tempStop - tempStart < 3) {
            //This is the limit to the zoom.
            //This will produce a completely gray widget.
            return;
        }
        zoomStart = tempStart;
        zoomStop = tempStop;
        isZoomed = true;
        hasDataChanged = true;
        secVerticalPos = -1;

    }

    /**
     * Gets the Y-cordinates for each data point at the X-cordinate that the user is currently touching
     *
     * @param l         The line to get the markers from
     * @param xDistance Distance between each point
     * @param highest   Highest value of any points added
     * @return Returns an array of floating points
     */
    private ArrayList<PointF> getMarkerPoints(Line l, float xDistance, float highest, int h) {
        ArrayList<PointF> points = new ArrayList<PointF>();
        int forTo = isZoomed ? (l.getPoints().size() >= (zoomStop + 1) ? zoomStop + 1 : l.getPoints().size()) : l.getPoints().size();
        int forStart = isZoomed ? (l.getPoints().size() >= zoomStart ? (zoomStart > 0 ? zoomStart - 1 : 0) : 0) : 0;

        for (int i = forStart; i < forTo; i++) {
            if (selOri == SelectionOrientation.Vertical) {
                if ((xDistance * (isZoomed ? i - zoomStart : i)) < (verticalPos + Dip(1f)) && (xDistance * (isZoomed ? i - zoomStart : i)) > (verticalPos - Dip(1f))) {
                    PointF p = new PointF(xDistance * (isZoomed ? i - zoomStart : i), (l.getPoints().get(i).getPoint() * (float) h) / (float) highest);
                    points.add(p);
                }
            } else if (selOri == SelectionOrientation.Horizontal) {
                if ((((l.getPoints().get(i).getPoint() * h) / highest) < (horizontalPos + Dip(2f))) && (((l.getPoints().get(i).getPoint() * h) / highest) > (horizontalPos - Dip(2f)))) {
                    PointF p = new PointF(xDistance * (isZoomed ? i - zoomStart : i), (l.getPoints().get(i).getPoint() * (float) h) / (float) highest);
                    points.add(p);
                }
            }
        }

        return points;
    }

    /**
     * Converts Y-cordinates back to their original data-values
     *
     * @param Y       Y-cordinates
     * @param highest Highest value of any points added
     * @return Returns the back-converterd data value
     */
    private float convertYCordToData(float Y, float highest, int h) {
        return ((Y * highest) / h);
    }

    /**
     * Fixed cases when the double data might be null.
     *
     * @param value Incomming data
     * @return Fixed data (if needed)
     */
    private float fixNull(double value) {
        if (!Double.isNaN(value)) {
            return (float) value;
        } else {
            return 0f;
        }
    }

    /**
     * Rounds a number by the supplied multiplier
     * <p>
     * e.i: num = 12, multi = 7 then the closes value with 7 as multiplier will be 14
     *
     * @param num        Value to round
     * @param multipleOf Multiplier to round to
     * @return Returns the rounded value
     */
    private double round(double num, int multipleOf) {
        return Math.floor((num + multipleOf / 2) / multipleOf) * multipleOf;
    }

    /**
     * Returns the values closes 10-based value by which you can divide the supplied value without it reaching > 10
     * <p>
     * e.i: 14820 would return 1000 because 14820 / 1000 = 14.8 so we can't add more 10's without the result dropping below 10.
     * <p>
     * I made this function as it was useful when scaling the lines to fit the view better, as it will automatically adapt to any value, no matter how big.
     * e.i with 14820 (returns 1000), it would give me a top margin at, at least:  height - (highest * height) / (highest + ≈5%) + 1000
     *
     * @param value Base value
     * @param last  This is only for simplicity, will let the function run it self with increes untill we've divided enough so the input value will drop below 10. You should always set this to 0, as the function will handle the rest it self.
     * @return Returns a 10-based value from 10 to infinity (the first number will always be one, and will always be followed by zeros).
     */
    private int findTopTen(double value, int last) {
        if (value <= 0)
            return 0;

        if ((value / 10) > 10)
            return findTopTen(value / 10, last == 0 ? 10 : last * 10);
        else if (last == 0)
            return 10;
        else
            return last;
    }

    /**
     * This is just simple methods so we save some lines of code.
     * We're just recycling the bitmaps if they aren't already.
     * Then we create new bitmap to redraw the gird and the lines on.
     */

    private void createNewInfoBitmap() {
        if (info != null && !info.isRecycled()) {
            info.recycle();
            info = null;
        }
        info = Bitmap.createBitmap(getWidth(), (int) Dip(LINE_INFO_SPACING), Config.ARGB_8888);
    }

    private void createNewGridBitmap() {
        //Recycle the old bitmaps, and release the instance
        if (grid != null && !grid.isRecycled()) {
            grid.recycle();
            grid = null;
        }

        if (INFO_VISIBLE) {
            grid = Bitmap.createBitmap(getWidth(), getHeight() - (int) Dip(LINE_INFO_SPACING), Config.ARGB_8888);
        } else {
            grid = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
        }

    }

    private void createNewLineBitmap() {
        //Recycle the old bitmaps, and release the instance
        if (lines != null && !lines.isRecycled()) {
            lines.recycle();
            lines = null;
        }

        if (INFO_VISIBLE) {
            lines = Bitmap.createBitmap(getWidth(), getHeight() - (int) Dip(LINE_INFO_SPACING), Config.ARGB_8888);
        } else {
            lines = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
        }


    }

    /**
     * Update the LineChart
     *
     * @param lines Lines of data to be displayed
     * @param grid  Draw the background grid
     */
    public void UpdateChart(ArrayList<Line> lines, boolean grid, boolean animation) {
        Lines = lines;
        topLineLength = 0;

        //We want to keep the shadow when we update the lines :)
        if (hasShadow) {
            if (Build.VERSION.SDK_INT >= 11) {
                if (Lines != null)
                    for (Line l : Lines) {
                        this.setLayerType(View.LAYER_TYPE_SOFTWARE, l.getColor());
                        l.getColor().setShadowLayer(5.0f, 0.0f, 2.0f, Color.DKGRAY);
                    }
            }
        }

        drawGrid = grid;
        hasDataChanged = true;
        zoomStart = 0;
        zoomStop = 0;
        isZoomed = false;
        invalidate();
        removeCallbacks(loadAnim);
        if (animation) {
            highestMultiplication = 20f;
            post(loadAnim);
        }

    }

    private void drawGrid(float max, int points, int h, int w) {
        if (grid != null && !grid.isRecycled())
            createNewGridBitmap();

        Canvas can = new Canvas(grid);

        double gridSizeH = 0;
        double gridSizeW = 0;

        //Calculate grid-spacing
        //Need something new here..
        double top = (int) (max / findTopTen(max, 0)) / 2;

        if (max < 100) {
            top = 10;
        }
        gridSizeH = h / top;
        gridSizeW = w / 10;


        Rect bounds = new Rect();
        bounds.left = 0;
        bounds.bottom = 0;
        bounds.right = w - 1;
        bounds.top = h - 1;

        GridPaint.setStyle(Style.STROKE);
        can.drawRect(bounds, GridPaint);

        //Vertical line
        for (int i = 0; i < (w / gridSizeW); i++) {
            float StartX = (float) gridSizeW * i;
            float StopX = (float) gridSizeW * i;
            can.drawLine(StartX, h, StopX, h - h, GridPaint);
            if (i == Math.round(w / gridSizeW))
                break;
        }

        //Horizontal line
        for (int i = 0; i < (h / gridSizeH); i++) {
            float StartY = (float) gridSizeH * i;
            float StopY = (float) gridSizeH * i;
            can.drawLine(0F, h - StartY, w, h - StopY, GridPaint);
            if ((i - 10) == Math.round(h / gridSizeH))
                break;
        }

    }

    private void drawInfo() {
        if (info != null && !info.isRecycled())
            createNewInfoBitmap();

        Canvas can = new Canvas(info);
        int offset = LINE_SPACEING;
        //Draw the line-name and color-sample
        for (Line l : Lines) {
            can.drawLine(Dip(offset - 5), Dip(5), Dip((offset - 5) + LINE_LENGTH), Dip(5), l.getColor());
            can.drawText(l.getName(), Dip(offset - 5), Dip(15), TextPaint);
            offset += (LINE_LENGTH + LINE_SPACEING);
        }

        //Draw info-text about the selected datapoints
        if (markedData != null) {
            if (mParserList != null) {
                //Callback to the context, that holds the view, for custom text formating
                String data = mParserList.onCustomParse(markedData);
                //Draw the text
                can.drawText(data, Dip(15), Dip(30), TextPaint);
            }
        }

    }

    private void drawLines(float xDistance, ArrayList<DataPoints> Points, int h, float highest) {
        //We'll create a new grid bitmap-layer if needed
        if (lines != null && !lines.isRecycled())
            createNewLineBitmap();

        Canvas canvas = new Canvas(lines);

        for (Line l : Lines) {
            Points = l.getPoints();
            //Indicate that last visible datapoint when zoomed
            int forTo = isZoomed ? (Points.size() >= zoomStop + 2 ? zoomStop + 2 : Points.size()) : Points.size();
            int forStart = isZoomed ? (Points.size() > zoomStart ? (zoomStart > 1 ? zoomStart - 2 : 0) : 0) : 0;
            //If we're zoomed in, we only draw the visible datapoints
            for (int i = forStart; i < forTo; i++) {
                float StartX = 0;
                float StartY = 0;
                float StopX = 0;
                float StopY = 0;
                //We don't want the values to be null. We can't work with null as value.
                StartX = fixNull((xDistance * (isZoomed ? i - zoomStart : i)) + pointXOffset); //The X-cordinate. We just move it one step forward for each point.
                StartY = fixNull((Points.get(i).getPoint() * h) / (highest * highestMultiplication)); //The Y-cordinate. Value times the height divided by the highest number (so it's to scale)

                if (i < Points.size() - 1) {
                    //Again, we can't work with null as value!
                    StopX = fixNull((xDistance * ((isZoomed ? i - zoomStart : i) + 1)) + pointXOffset);
                    StopY = fixNull((Points.get(i + 1).getPoint() * h) / (highest * highestMultiplication));
                } else {
                    StopX = StartX;
                    StopY = StartY;
                }
                //Remember that in Java-canvas, point 0 is not the bottom, it's the top of the canvas
                canvas.drawLine(StartX + Dip(1), h - StartY, StopX + Dip(1), h - StopY, l.getColor());
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight() - (INFO_VISIBLE ? (int) Dip(LINE_INFO_SPACING) : 0);
        float highest = 0f;
        ArrayList<DataPoints> Points = null;
        //Find highest value. We'll scale the chart according to that.
        //We also want to know largets amount of points that any of the lines hold.

        for (Line l : Lines) {
            Points = l.getPoints();
            int forTo = isZoomed ? (Points.size() >= zoomStop + 2 ? zoomStop + 2 : Points.size()) : Points.size();
            int forStart = isZoomed ? (Points.size() >= zoomStart ? (zoomStart > 0 ? zoomStart - 1 : 0) : 0) : 0;

            for (int i = forStart; i < forTo; i++) {
                if (highest < Points.get(i).getPoint())
                    highest = Points.get(i).getPoint();
            }

            if (topLineLength < (Points.size() - 1))
                topLineLength = Points.size() - 1;

        }
        //This is so that we don't get a ArithmeticException because we divid by zero.
        //Both in the running app, but also in the GUI-designer.
        if (highest < 1)
            return;
        if (topLineLength < 1)
            return;

        //fix the highest value, so we're not maxing out our gird. Now we'll have a bit of space above the highest datapoint
        int rounded = (int) round(highest, Math.round(highest / 2) + 10);
        rounded += findTopTen(highest, 0);
        highest = rounded < highest ? highest : rounded;

        //This calculates the x-distans between the datapoints.
        pointsXDistance = (float) (w) / (isZoomed ? (zoomStop - zoomStart) : topLineLength);

        /**
         * This new way of rendering the lines and the grid (as seperate layers)
         * let's me only redraw the touch-marker and the DataPoint markers when
         * the users is touching.
         *
         * That saves A LOT(!!) of processing power when shadow is on, or/and when the
         * lines has many datapoints.
         *
         * This way, we only redraw the lines and the grid when the lines has changed
         */
        if (hasDataChanged && drawGrid)
            drawGrid(highest, (int) (isZoomed ? (zoomStop - zoomStart) : topLineLength), h, w);
        canvas.drawBitmap(grid, 0, 0, dummyBitmapPaint);

        if (hasDataChanged)
            drawLines(pointsXDistance, Points, h, highest);
        canvas.drawBitmap(lines, 0, 0, dummyBitmapPaint);

        //Time to find markers if user is touching the view
        if (isTouching && !SCROLL_ENABLE) {

            //horizontalPos
            //Selected area to zoom into..ops, you're not suppose to know that yet ;)
            if (isMultiTouching)
                canvas.drawLine(secVerticalPos, h, secVerticalPos, 0, TouchMarkerPaint);

            //As I've added orientation of the touch, we need to keep track of what line to draw.
            if (selOri == SelectionOrientation.Vertical || isMultiTouching) {
                canvas.drawLine(verticalPos, h, verticalPos, 0, TouchMarkerPaint);
            } else if (selOri == SelectionOrientation.Horizontal && !isMultiTouching) {
                canvas.drawLine(0F, h - horizontalPos, getWidth(), h - horizontalPos, TouchMarkerPaint);
            }

            //Reset the array
            if (markedData != null)
                markedData.clear();

            markedData = null;

            if (!isMultiTouching) // We don't draw any markers when selecting zoom-area
                if (Lines != null)
                    for (Line l : Lines) {
                        if (hasMarker(l, pointsXDistance, h, highest)) {
                            if (markedData == null)
                                markedData = new ArrayList<MarkedData>();

                            ArrayList<PointF> points = getMarkerPoints(l, pointsXDistance, highest, h);
                            ArrayList<Float> markedValues = new ArrayList<Float>(); // Might cause performance issue, but should not be a problem unless you've got A LOT(!) of lines.

                            for (PointF fl : points) {
                                markedValues.add(convertYCordToData(fl.y, highest, h));
                                DrawDataPointMarker(fl.x + Dip(1), h - fl.y, canvas, l.getColor());
                            }
                            points.clear(); // Save some memory :)

                            markedData.add(new MarkedData(markedValues, l)); // Same as new ArrayList<Float>() above (performance issue thingy)

                            System.gc(); //Hint to the Garbage collector that we have some data for it to collect. Let's hope it has time to collect it :)

                        }
                    }
            if (markedData != null)
                markedUpdated = true;

            //Callback for the selected values for this line.
            if (mListener != null && markedData != null) { //If markedData is null, then there was no data-points where you user touched. No need to make a empty callback to the activity.
                mListener.onDataPointsMarked(markedData);

            }

        }
        if (INFO_VISIBLE) {//Only draw if it set to be visible
            //Update the line-info if data changes
            if (hasDataChanged || markedUpdated) {
                drawInfo();
            }

            if (info != null) {
                canvas.drawBitmap(info, 0, h, dummyBitmapPaint);
            }
        }

        //We don't need to change them to false if they already are false.
        //e.i If it's not broken, don't fix it
        if (hasDataChanged)
            hasDataChanged = false;
        if (markedUpdated)
            markedUpdated = false;

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        createNewGridBitmap();
        createNewLineBitmap();
        createNewInfoBitmap();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_UP) {
            //Not touching. Let's update the state and redraw the view
            isMultiTouching = false;
            isTouching = false;
            SCROLL_ENABLE = false;
            //SCROLL_START_POS = 0f;
            getSelectedZoom(pointsXDistance, topLineLength);
            invalidate();
        } else if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            //Touching! Set the points and redraw
            int pointerCount = event.getPointerCount();
            int action = event.getAction();
            float xFirst = event.getX(0);
            float xSec = 0f;

            isTouching = true;

            if (pointerCount == 2) {
                isMultiTouching = true;

                xSec = event.getX(1);
                //Testing out a simple scroll function. Requres two fingers that is vertially with each other.
                //Might add a scroll-bar under the chart. Maybe as an external widget?..hm
                if (action == MotionEvent.ACTION_DOWN) {
                    if (isScrolling(xFirst, xSec) && !SCROLL_ENABLE) {
                        SCROLL_START_POS = (xFirst + xSec) / 2f;
                        SCROLL_ENABLE = true;
                    }
                } else if (action == MotionEvent.ACTION_MOVE) {
                    xFirst = event.getX();
                    xSec = event.getX(1);
                    SCROLL_ENABLE = isScrolling(xFirst, xSec);

                    if (SCROLL_ENABLE) {
                        int scrollDirection = SCROLL_START_POS - ((xFirst + xSec) / 2f) > 0 ? 1 : 2; //Direction of the scroll

                        if (scrollDirection == 2) { //Scroll left
                            if (((xFirst + xSec) / 2f) - SCROLL_START_POS >= SCROLL_SPEED && zoomStart > 0) {
                                //We added a offset to the datapoints, so we can give it a bit smoother scrolling.
                                //When the offset is the same as the distans between 2 points, then we "move" one datapoint in the scrolling-direction
                                //then zoom-area and resets the offset.
                                if (pointXOffset >= pointsXDistance || pointsXDistance < 20) {
                                    zoomStart--;
                                    zoomStop--;
                                    pointXOffset = 0f;
                                } else {
                                    pointXOffset += (pointsXDistance / 2);
                                }
                                SCROLL_START_POS = (xFirst + xSec) / 2f;
                                hasDataChanged = true;
                            }
                        } else if (scrollDirection == 1) { //Scroll right
                            if (SCROLL_START_POS - ((xFirst + xSec) / 2f) >= SCROLL_SPEED && zoomStop < topLineLength) {
                                if ((pointXOffset * (-1)) >= pointsXDistance || pointsXDistance < 20) {
                                    zoomStart++;
                                    zoomStop++;
                                    pointXOffset = 0f;
                                } else {
                                    pointXOffset -= (pointsXDistance / 2);
                                }

                                SCROLL_START_POS = (xFirst + xSec) / 2f;
                                hasDataChanged = true;
                            }
                        }
                    }
                }

            } else {
                isMultiTouching = false;
                SCROLL_ENABLE = false;
                //pointXOffset = 0f;
            }

            if (selOri == SelectionOrientation.Vertical || isMultiTouching && !SCROLL_ENABLE) {
                verticalPos = event.getX();
                if (event.getPointerCount() > 1)
                    secVerticalPos = event.getX(1);
                else
                    secVerticalPos = -1;
            } else if (selOri == SelectionOrientation.Horizontal && !isMultiTouching && !SCROLL_ENABLE) {
                horizontalPos = getHeight() - event.getY(); //We need this because in java, 0,0 on the canvas is not bottom left. It's actually top left. So we need to correct for that :)
                secVerticalPos = -1;
            }

            invalidate();


        } else if (event.getAction() == MotionEvent.ACTION_POINTER_2_UP) {
            isMultiTouching = false;
            isTouching = false;
            getSelectedZoom(pointsXDistance, topLineLength);
            invalidate();
        }

        return gestureDetector.onTouchEvent(event);

    }

    /**
     * Check if touches in within the accepted distance of each other
     *
     * @param x1 X-cordinate of the first pointer
     * @param x2 X-cordinate of the second pointer
     * @return Return true or false
     */
    public boolean isScrolling(float x1, float x2) {
        if (x1 > x2 && ((x1 - x2) < ACCEPTED_FINGER_DIFF)) {
            return true;
        }

        if (x2 > x1 && ((x2 - x1) < ACCEPTED_FINGER_DIFF)) {
            return true;
        }


        return false;
    }

    //Orientations
    public enum SelectionOrientation {
        Horizontal,
        Vertical
    }


    //Our own beautiful listener
    public interface OnDataPointMarkedListener {
        void onDataPointsMarked(ArrayList<MarkedData> data);
    }

    public interface OnInfoNeedParseListener {
        String onCustomParse(ArrayList<MarkedData> data);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {

            return true;
        }


        // event when double tap occurs
        @Override
        public boolean onDoubleTap(MotionEvent e) {

            isMultiTouching = false;
            isTouching = false;
            isZoomed = false;
            hasDataChanged = true;
            secVerticalPos = -1;
            zoomStart = 0;
            zoomStop = 0;
            LineChartView.this.invalidate();

            return true;
        }
    }

}