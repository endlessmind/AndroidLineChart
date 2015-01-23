package com.example.linechart;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class LineChartView extends View {
	private String TAG = "LineViewChart";
	ArrayList<Line> Lines = new ArrayList<Line>();
    Paint GridPaint = new Paint();
    Paint TouchMarkerPaint = new Paint();
    Paint markerColor = new Paint();
    
	boolean drawGrid = false;
	
	float verticalPos = 0.0f;
	boolean isTouching = false;
	
	private OnDataPointMarkedListener mListener;
	
	public LineChartView(Context context){
	   super(context);
	   GridPaint.setColor(Color.BLACK);
	   TouchMarkerPaint.setColor(Color.DKGRAY);
	   TouchMarkerPaint.setStrokeWidth(Dip(1));
	}

	public LineChartView(Context context, AttributeSet attrs) {
	    this(context, attrs, 0);
	}
	
	public LineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		GridPaint.setColor(Color.BLACK);
		TouchMarkerPaint.setColor(Color.DKGRAY);
		TouchMarkerPaint.setStrokeWidth(Dip(1));
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
		
		invalidate();
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
		
	    canvas.drawCircle(CenterX + 1.0F, CenterY + 1.0F, Dip(4), markerColor);
	    canvas.drawCircle(CenterX + 1.0F, CenterY + 1.0F, Dip(2), centerColor);
	}
	

	
	
	/**
	 * Get if there is a point marker at the supplied X-cordinate
	 * 
	 * @param line Line object cointaining the points
	 * @param x X-cordinate
	 * @return Returns true of there is a marker at that x-cordinate, else false
	 */
	private boolean hasMarker(Line line, float xDistance) {
		
		for (int i = 0; i< line.getPoints().size(); i++) {
			if ((xDistance * (i)) < (verticalPos + Dip(1)) && (xDistance * (i)) > (verticalPos - Dip(1))) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Gets the Y-cordinates for each data point at the X-cordinate that the user is currently touching
	 * 
	 * 
	 * @param xDistance Distance between each point
	 * @param highest Highest value of any points added
	 * @return Returns an array of floating points
	 */
	private ArrayList<PointF> getMarkerYPoints(Line l, float xDistance, float highest) {
		ArrayList<PointF> points = new ArrayList<PointF>();
		int h = -24 + getHeight();
		
			for (int i = 0; i< l.getPoints().size(); i++) {
				if ((xDistance * (i)) < (verticalPos + Dip(1)) && (xDistance * (i)) > (verticalPos - Dip(1))) {
					PointF p = new PointF(xDistance * (i) , (l.getPoints().get(i).getPoint() * (float)h) / (float)highest);
					points.add(p);
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
	 * Update the LineChart
	 * @param lines Lines of data to be displayed
	 * @param grid  Draw the background grid
	 */
	  public void UpdateChart(ArrayList<Line> lines, boolean grid)
	  {
	    Lines = lines;
	    drawGrid = grid;
	    invalidate();
	  }

	  private void DrawGrid(float max, int datapoints, Canvas canvas)
	  {
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
	    	gridSizeW = w / datapoints;
	    } else {
	        gridSizeW = 30.0D;
	    }
	    
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
	  
	  
		@Override
	    protected void onDraw(Canvas canvas) {
	        super.onDraw(canvas);
	        int w = getWidth();
	        int h = -24 + getHeight();
	        float highest = 0;
	        float maxPoints = 0;
	        ArrayList<DataPoints> Points = null;
	        //Find highest value. We'll scale the chart according to that.
	        //We also want to know largets amount of points that any of the lines hold. 
	        for (Line l: Lines) {
	        	Points = l.getPoints();
	        	for (DataPoints dp : Points) {
	        		if (highest < dp.getPoint())
	        			highest = dp.getPoint();
	        	}
	        	if (maxPoints < (Points.size() -1 ))
	        		maxPoints = Points.size() -1;
	        	
	        }
	        //This is so that we don't get a ArithmeticException because we divid by zero.
	        //Both in the running app, but also in the GUI-designer.
	        if (highest < 1)
	        	return;
	        if (maxPoints < 1)
	        	return;
	        
	        //fix the highest value, so we're not maxing out our gird. Now we'll have a bit of space above the highest datapoint
	        int rounded = (int)round(highest, Math.round(highest / 2) + 10 );
	        rounded += findTopTen(highest, 0);
	        highest = rounded < highest ? highest : rounded;
	        
	        //This calculates the x-distans between the datapoints.
	        float xDistance = (float)(w - Dip(8)) / maxPoints;
	        

	        //Draw grid, needs som fixing
	        DrawGrid(highest, (int)maxPoints, canvas);
	        
	        for (Line l : Lines ) {
	        	Points = l.getPoints();

	        	for (int i = 0; i < Points.size(); i++) {
	        		float StartX = 0;
	        		float StartY = 0;
	        		float StopX = 0;
	        		float StopY = 0;
	        		//We don't want the values to be null. We can't work with null as value.
	        		StartX = fixNull(xDistance * (i)); //The X-cordinate. We just move it one step forward for each point.
	        		StartY = fixNull((Points.get(i).getPoint() *h) / highest); //The Y-cordinate. Value times the height divided by the highest number (so it's to scale)
	        			
	        		if (i < Points.size() -1) {
	        			//Again, we can't work with null as value!
	        			StopX = fixNull(xDistance * (i + 1));
	        			StopY = fixNull((Points.get(i + 1).getPoint() *h) / highest);
	        		} else {
	        			StopX = StartX;
	        			StopY = StartY;
	        		}
	        		
	        		//Remember that in Java-canvas, point 0 is not the bottom, it's the top of the canvas		
	        		canvas.drawLine( 10 + StartX, -12 + getHeight() - StartY, 10 + StopX, -12 + getHeight() - StopY, l.getColor());
	        	}
	        	
	        	
	        }
	        
	        //Time to find markers if user is touching the view
	        if (isTouching) {

	        	canvas.drawLine( 10 + verticalPos,  getHeight(), 10 + verticalPos, 0, TouchMarkerPaint);
	        	ArrayList<MarkedData> markedData = null;
	        	for (Line l : Lines) {
	        		if (hasMarker(l, xDistance)) { 
	        			if (markedData == null)
	        				markedData = new ArrayList<MarkedData>();
	        			
	        			
	        			ArrayList<PointF> points = getMarkerYPoints(l, xDistance, highest);
	        			ArrayList<Float> markedValues = new ArrayList<Float>(); //TODO: Might cause performance issue, but should not be a problem unless you've got A LOT(!) of lines.
	        	
	        			for (PointF fl : points) {
	        				markedValues.add(convertYCordToData(fl.y,highest));
	        				DrawDataPointMarker(10 + fl.x, -12 + getHeight() - fl.y, canvas, l.getColor());
	        			}
	        			points.clear(); // Save some memory :)
	        			
	        			markedData.add(new MarkedData(markedValues, l)); //TODO: Same as new ArrayList<Float>() above
	        			
	        			System.gc(); //Hint to the Garbage collector that we have some data for it to collect. Let's hope it has time to collect it :)
	     
	        		}
	        	}
	        	//Callback for the selected values for this line. 
    			if (mListener != null && markedData != null) //If markedData is null, then there was no data-points where you user touched. No need to make a empty callback to the activity.
    				mListener.onDataPointsMarked(markedData);
	        	
	      }
	        
}
		
		
		@Override
	    public boolean onTouchEvent(MotionEvent event) {
			
			if (event.getAction() == MotionEvent.ACTION_UP) {
				//Not touching. Let's update the state and redraw the view
				isTouching = false;
				invalidate();
			} else if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
				//Touching! Set the points and redraw
				isTouching = true;
				verticalPos = event.getX();
				invalidate();
				
			}
			
			return true;
			
		}


		//Our own beautiful listener
		public interface OnDataPointMarkedListener {
			void onDataPointsMarked(ArrayList<MarkedData> data);
		}
		
}


