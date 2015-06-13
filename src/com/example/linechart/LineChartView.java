package com.example.linechart;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class LineChartView extends View {
	
	public static float ACCEPTED_FINGER_DIFF = 120f;
	public static float SCROLL_SPEED = 5f; //Lower value equals faster scroll 
	private float SCROLL_START_POS = 0f;
	private boolean SCROLL_ENABLE = false;
	
	
	private String TAG = "LineViewChart";
	ArrayList<Line> Lines = new ArrayList<Line>();
	
    Paint GridPaint = new Paint();
    Paint TouchMarkerPaint = new Paint();
    Paint markerColor = new Paint();
    Paint dummyBitmapPaint = new Paint();
    
    SelectionOrientation selOri = SelectionOrientation.Horizontal;
    GestureDetector gestureDetector;
    private OnDataPointMarkedListener mListener;
    
    //The layers we are going to draw
    Bitmap lines;
    Bitmap grid;
    
    
	boolean drawGrid = false;
	boolean hasDataChanged = false;
	boolean isTouching = false;
	boolean hasShadow = false;
	boolean isZoomed = false;
	boolean isMultiTouching = false;
	
	int zoomStart = 0;
	int zoomStop = 0;
	private float highestMultiplication = 20f;
	private float topLineLength = 0; //Datapoint count of the line with the most datapoints.
	private float pointsXDistance = 0f; //The distance between each point
	private float pointXOffset = 0f;
	
	float verticalPos = 0.0f;
	float secVerticalPos = 0.0f;
	float horizontalPos = 0.0f;
	
	
	
	
	public LineChartView(Context context){
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
		gestureDetector = new GestureDetector(context, new GestureListener());
	}
	
	public void setOnDataPointMarkedListener(OnDataPointMarkedListener listener) {
		this.mListener = listener;
	}
	
	public OnDataPointMarkedListener getOnDataPointMarkedListener() {
		return this.mListener;
	}
	
	public void addShadow() {
		if (Build.VERSION.SDK_INT >= 11) {
			this.setLayerType(View.LAYER_TYPE_SOFTWARE, markerColor);
			this.setLayerType(View.LAYER_TYPE_SOFTWARE, GridPaint);
			if (Lines != null)
				for (Line l : Lines) {
					this.setLayerType(View.LAYER_TYPE_SOFTWARE, l.getColor());
					l.getColor().setShadowLayer(5.0f, 0.0f, 2.0f, Color.DKGRAY);
				}
		}
		GridPaint.setShadowLayer(2.0f, 3.0f, 3.0f, Color.GRAY);
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
	
	/**
	 * Set the orientation of datapoint selection
	 * Let's you choose in what orientation you want to be able to select datapoints.
	 * 
	 * 
	 * Default is horizontal
	 * 
	 * @param so SelectionOrientation.Horizontal or SelectionOrientation.Vertical
	 */
	public void setSelectionOrientation(SelectionOrientation so) {
		selOri = so;
	}
	
	/**
	 * Gets the current orientation of datapoint selection
	 * 
	 * @return Returns SelectionOrientation.Horizontal or SelectionOrientation.Vertical
	 */
	public SelectionOrientation getSelectionOrientation() {
		return selOri;
	}
	
	
    private float Dip(int value) {
    	Resources r = getResources();
    	float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, r.getDisplayMetrics());
    	return  px;

    }
	
    
	private void DrawDataPointMarker(float CenterX, float CenterY, Canvas canvas, Paint centerColor){
	    markerColor.setAntiAlias(true);
	    markerColor.setStyle(Paint.Style.FILL_AND_STROKE);
	    markerColor.setColor(Color.BLACK);
		
	//    canvas.drawCircle(CenterX + 1.0F, CenterY + 1.0F, Dip(4), markerColor);
	    canvas.drawCircle(CenterX + 1.0F, CenterY + 1.0F, Dip(3), centerColor);
	}
	
	
	/**
	 * Get if there is a point marker at the supplied X-cordinate
	 * 
	 * @param line Line object cointaining the points
	 * @param xDistance The calculated distance between each point on the X-axes
	 * @param h Height of the view, with own margin added
	 * @param highest The highest value of any given datapoint
	 * @return Returns true of there is a marker at that x-cordinate, else false
	 */
	private boolean hasMarker(Line line, float xDistance, int h, float highest) {
		int forTo = isZoomed ? (line.getPoints().size() >= (zoomStop +1) ? zoomStop +1 : line.getPoints().size()) : line.getPoints().size();
		int forStart = isZoomed ? (line.getPoints().size() >= zoomStart ? (zoomStart > 0 ? zoomStart - 1 : 0) : 0) : 0;
		
		for (int i = forStart; i< forTo; i++) {
			if (selOri == SelectionOrientation.Vertical) {
				if ((xDistance * (isZoomed ? i - zoomStart : i)) < (verticalPos + Dip(1)) && (xDistance * (isZoomed ? i - zoomStart : i)) > (verticalPos - Dip(1))) {
					return true;
				}
			} else if (selOri == SelectionOrientation.Horizontal) {
				if ((((line.getPoints().get(i).getPoint() *h) / highest) < (horizontalPos + 2)) && (((line.getPoints().get(i).getPoint() *h) / highest) > (horizontalPos - 2))) {
					return true;
				}
			}
			
		}
		
		return false;
	}
	
	private void getSelectedZoom(float xDistance,float maxPoints) {
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
			
			
			if (right >= ((xDistance * i) - (xDistance * 0.1f)) && right <= ((xDistance * i) + (xDistance * 0.9f)) ) {
				tempStop = zoomStart + i;
			} else if (right > ((xDistance * i) + (xDistance * 0.9f)) && right < ((xDistance * (i + 1) - (xDistance * 0.1f))) ) {
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
	 * @param l The line to get the markers from
	 * @param xDistance Distance between each point
	 * @param highest Highest value of any points added
	 * @return Returns an array of floating points
	 */
	private ArrayList<PointF> getMarkerPoints(Line l, float xDistance, float highest) {
		ArrayList<PointF> points = new ArrayList<PointF>();
		int h = -24 + getHeight();
		int forTo = isZoomed ? (l.getPoints().size() >= (zoomStop +1) ? zoomStop + 1 : l.getPoints().size()) : l.getPoints().size();
		int forStart = isZoomed ? (l.getPoints().size() >= zoomStart ? (zoomStart > 0 ? zoomStart - 1 : 0) : 0) : 0;
		
		for (int i = forStart; i< forTo; i++) {
			if (selOri == SelectionOrientation.Vertical) {
				if ((xDistance * (isZoomed ? i - zoomStart : i)) < (verticalPos + Dip(1)) && (xDistance * (isZoomed ? i - zoomStart : i)) > (verticalPos - Dip(1))) {
					PointF p = new PointF(xDistance * (isZoomed ? i - zoomStart : i) , (l.getPoints().get(i).getPoint() * (float)h) / (float)highest);
					points.add(p);
				}
			} else if (selOri == SelectionOrientation.Horizontal) {
				if ((((l.getPoints().get(i).getPoint() *h) / highest) < (horizontalPos + 2)) && (((l.getPoints().get(i).getPoint() *h) / highest) > (horizontalPos - 2))) {
					PointF p = new PointF(xDistance * (isZoomed ? i - zoomStart : i) , (l.getPoints().get( i).getPoint() * (float)h) / (float)highest);
					points.add(p);
				}
			}
		}
		
		
		return points;
	}
	
	/**
	 * Converts Y-cordinates back to their original data-values
	 * 
	 * @param Y Y-cordinates
	 * @param highest Highest value of any points added
	 * @return Returns the back-converterd data value
	 */
	private float convertYCordToData(float Y, float highest) {
		int h = -24 + getHeight();
		return ((Y *highest) / h);
	}
	
	/**
	 * Fixed cases when the double data might be null.
	 * 
	 * @param value Incomming data
	 * @return Fixed data (if needed)
	 */
	private float fixNull(double value) {
		if (!Double.isNaN(value)) {
			return (float)value;
		} else {
			return 0f;
		}
	}
	
	/**
	 * Rounds a number by the supplied multiplier
	 * 
	 * e.i: num = 12, multi = 7 then the closes value with 7 as multiplier will be 14
	 * 
	 * @param num Value to round
	 * @param multipleOf Multiplier to round to
	 * @return Returns the rounded value
	 */
	private double round( double num, int multipleOf) {
		  return Math.floor((num + multipleOf/2) / multipleOf) * multipleOf;
	}
	
	/**
	 * Returns the values closes 10-based value by which you can divide the supplied value without it reaching > 10
	 * 
	 * e.i: 14820 would return 1000 because 14820 / 1000 = 14.8 so we can't add more 10's without the result dropping below 10.
	 *
	 * I made this function as it was useful when scaling the lines to fit the view better, as it will automatically adapt to any value, no matter how big.
	 * e.i with 14820 (returns 1000), it would give me a top margin at, at least:  height - (highest * height) / (highest + ≈5%) + 1000 
	 * 
	 * @param value Base value
	 * @param last This is only for simplicity, will let the function run it self with increes untill we've divided enough so the input value will drop below 10. You should always set this to 0, as the function will handle the rest it self.
	 * @return Returns a 10-based value from 10 to infinity (the first number will always be one, and will always be followed by zeros).
	 */
	private int findTopTen(double value, int last) {
		if (value <= 0)
			return 0;
		
		if ((value / 10) > 10)
			return findTopTen(value / 10, last == 0 ? 10 : last * 10);
		else
			if (last == 0)
				return 10;
			else
				return last;
	}
	
	
	/**
	 * This is just simple methods so we save some lines of code.
	 * We're just recycling the bitmaps if they aren't already.
	 * Then we create new bitmap to redraw the gird and the lines on.
	 */
	
	private void createNewGridBitmap() {
		//Recycle the old bitmaps, and release the instance
		if (grid != null && !grid.isRecycled()) {
			grid.recycle(); grid = null;
		}
		//Create the new bitmaps
		grid = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
	}
	
	private void createNewLineBitmap() {
		//Recycle the old bitmaps, and release the instance
		if (lines != null && !lines.isRecycled()) {
			lines.recycle(); lines = null;
		}
		//Create the new bitmaps
		lines = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
	}
	
	/**
	 * Update the LineChart
	 * @param lines Lines of data to be displayed
	 * @param grid  Draw the background grid
	 */
	  public void UpdateChart(ArrayList<Line> lines, boolean grid)
	  {
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
	    highestMultiplication = 20f;
	    invalidate();
        removeCallbacks(loadAnim);
        post(loadAnim);
	    
	  }

	  private void DrawGrid(float max, int datapoints)
	  {
		//We'll create a new grid bitmap-layer if needed
		if (grid != null && !grid.isRecycled())
			createNewGridBitmap();
		  
		Canvas canvas = new Canvas(grid);
		
	    int w = getWidth();
	    int h = -24 + getHeight();
	    double gridSizeH = 0;
	    double gridSizeW = 0;
	    

	    //Calculate gridline spacing
	    if ((max > 60) && (max < 1200))
	    {
	    	
	    	gridSizeH = h / (max / 5);
	    } else {
	        gridSizeH = 30.0D;
	    }
	    
	    if (datapoints > 10 && datapoints < 70) {
	    	gridSizeW = w / (datapoints / findTopTen(datapoints,0)) ;
	    } else {
	        gridSizeW = 30.0D;
	    }
	    
	  //  gridSizeH = (h / (Math.round(max) / 5));
	 //   gridSizeW = w / (datapoints / findTopTen(datapoints,0));
	    
	    //Vertical line
	    for (int i = 0; i < (w / gridSizeW); i++) {
	        float StartX = (float)gridSizeW * i;
	        float StopX = (float)gridSizeW * i;
	        canvas.drawLine( 10 + StartX, -12 + getHeight(), 10 + StopX, -12 + getHeight() - h, GridPaint);
	        if (i == Math.round(w / gridSizeW)) 
	        	break;
	    }
	    
	    //Horizontal line
	    for (int i = 0; i < (h/ gridSizeH); i++) {
		      float StartY = (float)gridSizeH * i;
		      float StopY = (float)gridSizeH * i;
		      canvas.drawLine(10.0F, -12 + getHeight() - StartY, w, -12 + getHeight() - StopY, GridPaint);
		      if ((i -10 ) == Math.round(h / gridSizeH)) 
		    	  break;
	    }
	    
	  }
	  
	  private void DrawLines(float xDistance, ArrayList<DataPoints> Points, int h, float highest) {
		  
		  //We'll create a new grid bitmap-layer if needed
		  if (lines != null && !lines.isRecycled())
			  createNewLineBitmap();
			  
		  Canvas canvas = new Canvas(lines);

		  
		  for (Line l : Lines ) {
			  Points = l.getPoints();
			  //Indicate that last visible datapoint when zoomed
			  int forTo = isZoomed ? (Points.size() >= zoomStop +1 ? zoomStop + 1 : Points.size()) : Points.size();
			  int forStart = isZoomed ? (Points.size() >= zoomStart ? (zoomStart > 0 ? zoomStart - 1 : 0) : 0) : 0;
			  //If we're zoomed in, we only draw the visible datapoints
			  for (int i = forStart; i < forTo; i++) {
				  float StartX = 0;
				  float StartY = 0;
				  float StopX = 0;
				  float StopY = 0;
				  //We don't want the values to be null. We can't work with null as value.
				  StartX = fixNull((xDistance * (isZoomed ? i - zoomStart : i)) + pointXOffset); //The X-cordinate. We just move it one step forward for each point.
				  StartY = fixNull((Points.get(i).getPoint() *h) / (highest * highestMultiplication)); //The Y-cordinate. Value times the height divided by the highest number (so it's to scale)
		        			
				  if (i < Points.size() -1) {
					  //Again, we can't work with null as value!
					  StopX = fixNull((xDistance * ((isZoomed ? i - zoomStart : i) + 1)) + pointXOffset);
					  StopY = fixNull((Points.get(i + 1).getPoint() *h) / (highest * highestMultiplication));
				  } else {
					  StopX = StartX;
					  StopY = StartY;
				  }
		        		
				  //Remember that in Java-canvas, point 0 is not the bottom, it's the top of the canvas		
				  canvas.drawLine( 10 + StartX, -12 + getHeight() - StartY, 10 + StopX, -12 + getHeight() - StopY, l.getColor());
			  }
		  }
	  }
	  
	  
		@Override
	    protected void onDraw(Canvas canvas) {
	        super.onDraw(canvas);
	        int w = getWidth();
	        int h = -24 + getHeight();
	        float highest = 0f;
	        ArrayList<DataPoints> Points = null;
	        //Find highest value. We'll scale the chart according to that.
	        //We also want to know largets amount of points that any of the lines hold. 
	        
	        for (Line l: Lines) {
	        	Points = l.getPoints();
	    		int forTo = isZoomed ? (Points.size() >= zoomStop + 2 ? zoomStop + 2 : Points.size()) : Points.size();
	    		int forStart = isZoomed ? (Points.size() >= zoomStart ? (zoomStart > 0 ? zoomStart - 1 : 0) : 0) : 0;
	        	
	        	
	        	for (int i = forStart; i < forTo; i++) {
	        		if (highest < Points.get(i).getPoint())
	        			highest = Points.get(i).getPoint();
	        	}
	        	
	        	if (topLineLength < (Points.size() -1 ))
	        		topLineLength = Points.size() -1;
	        	
	        }
	        //This is so that we don't get a ArithmeticException because we divid by zero.
	        //Both in the running app, but also in the GUI-designer.
	        if (highest < 1)
	        	return;
	        if (topLineLength < 1)
	        	return;
	        
	        //fix the highest value, so we're not maxing out our gird. Now we'll have a bit of space above the highest datapoint
	        int rounded = (int)round(highest, Math.round(highest / 2) + 10 );
	        rounded += findTopTen(highest, 0);
	        highest = rounded < highest ? highest : rounded;
	        
	        //This calculates the x-distans between the datapoints.
	        pointsXDistance = (float)(w - Dip(8)) / (isZoomed ? (zoomStop - zoomStart) : topLineLength);

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
	        if (hasDataChanged)
	        	DrawGrid(highest, (int)(isZoomed ? (zoomStop - zoomStart) : topLineLength));
	        canvas.drawBitmap(grid, 0,0, dummyBitmapPaint);
	        
	        if (hasDataChanged)
	        	DrawLines(pointsXDistance, Points, h, highest);
	 	    canvas.drawBitmap(lines, 0,0, dummyBitmapPaint);
	        
	 	   
	 	   if (hasDataChanged)
	 		   hasDataChanged = false;
	        
	        
	        
	        
	        
	        //Time to find markers if user is touching the view
	        if (isTouching && !SCROLL_ENABLE) {
	        	
	        	//horizontalPos
	        	//Selected area to zoom into..ops, you're not suppose to know that yet ;)
	        	if (isMultiTouching)
	        		canvas.drawLine( 10 + secVerticalPos,  getHeight(), 10 + secVerticalPos, 0, TouchMarkerPaint);
	        	
	        	//As I've added orientation of the touch, we need to keep track of what line to draw.
	        	if (selOri == SelectionOrientation.Vertical || isMultiTouching) {
	        		canvas.drawLine( 10 + verticalPos,  getHeight(), 10 + verticalPos, 0, TouchMarkerPaint);
	        	} else if (selOri == SelectionOrientation.Horizontal && !isMultiTouching) {
	        		canvas.drawLine( 10.0F,-12 + getHeight() - horizontalPos,getWidth(),-12 + getHeight() - horizontalPos,TouchMarkerPaint);
	        	}
	        	
	        	
	        	ArrayList<MarkedData> markedData = null;
	        	if (!isMultiTouching) // We don't draw any markers when selecting zoom-area
		        	for (Line l : Lines) {
		        		if (hasMarker(l, pointsXDistance, h, highest)) { 
		        			if (markedData == null)
		        				markedData = new ArrayList<MarkedData>();
		        			
		        			ArrayList<PointF> points = getMarkerPoints(l, pointsXDistance, highest);
		        			ArrayList<Float> markedValues = new ArrayList<Float>(); //TODO: Might cause performance issue, but should not be a problem unless you've got A LOT(!) of lines.
		        	
		        			for (PointF fl : points) {
		        				markedValues.add(convertYCordToData(fl.y,highest));
		        				DrawDataPointMarker(10 + fl.x, -12 + getHeight() - fl.y, canvas, l.getColor());
		        			}
		        			points.clear(); // Save some memory :)
		        			
		        			markedData.add(new MarkedData(markedValues, l)); //TODO: Same as new ArrayList<Float>() above (performance issue thingy)
		        			
		        			System.gc(); //Hint to the Garbage collector that we have some data for it to collect. Let's hope it has time to collect it :)
		     
		        		}
		        	}
	        	
	        		
	        	//Callback for the selected values for this line. 
    			if (mListener != null && markedData != null) //If markedData is null, then there was no data-points where you user touched. No need to make a empty callback to the activity.
    				mListener.onDataPointsMarked(markedData);
	        	
	      }
	        
}
		
		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			/*
			 *The size has changed. We'll create new bitmap-layers to match the new size 
			 */
			createNewGridBitmap();
			createNewLineBitmap();
		}
		
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
					isMultiTouching =  true;
					
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
	    						if (((xFirst + xSec) / 2f) - SCROLL_START_POS >= SCROLL_SPEED) {
	    							//We added a offset to the datapoints, so we can give it a bit smoother scrolling.
	    							//When the offset is the same as the distans between 2 points, then we "move" one datapoint in the scrolling-direction
	    							//then zoom-area and resets the offset. 
	    							if (pointXOffset > pointsXDistance ) {
	    								zoomStart--;
	    								zoomStop--;
	    								pointXOffset = 0f;
	    							} else {
	    								pointXOffset += 7;
	    							}
	    							SCROLL_START_POS = (xFirst + xSec) / 2f;
	    							hasDataChanged = true;
	    						}
	    					} else if (scrollDirection == 1) { //Scroll right
	    						if (SCROLL_START_POS - ((xFirst + xSec) / 2f) >= SCROLL_SPEED) {
	    							if ((pointXOffset * (-1)) > pointsXDistance) {
		    							zoomStart++;
		    							zoomStop++;
		    							pointXOffset = 0f;
	    							} else {
	    								pointXOffset -= 7;
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
					horizontalPos =  getHeight() - event.getY(); //We need this because in java, 0,0 on the canvas is not bottom left. It's actually top left. So we need to correct for that :)
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
		 * 
		 * A simple load-animation that lets the chart "grow" from a straight line.
		 * Just for testing, noticed that it's terrible for lines with like 1000+ datapoints.
		 * 
		 */
		Runnable loadAnim = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
					highestMultiplication -= 1f;
					
				if (highestMultiplication < 1.1f) { //Error correction. We don't want to get stuck in this loop.
					highestMultiplication = 1f;
				}
				
				if (highestMultiplication > 1f)
					postDelayed(this, 25); //Restarts the runnable with a given delay, kind of like a loop that doesn't block the ui-thread.
				
				hasDataChanged = true; //Need to flag the data as changes for the view to actually redraw it.
				invalidate();
			}
			
		};
		
		   private class GestureListener extends GestureDetector.SimpleOnGestureListener {

		        @Override
		        public boolean onDown(MotionEvent e) {
		        	
		            return true;
		        }
		        

		        // event when double tap occurs
		        @Override
		        public boolean onDoubleTap(MotionEvent e) {
		            float x = e.getX();
		            float y = e.getY();

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

	        /**
	         * 
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
		   

		//Our own beautiful listener
		public interface OnDataPointMarkedListener {
			void onDataPointsMarked(ArrayList<MarkedData> data);
		}
		
		//Orientations
		public enum SelectionOrientation {
			Horizontal,
			Vertical
		}
		
}


