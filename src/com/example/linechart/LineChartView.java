package com.example.linechart;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

public class LineChartView extends View {
	private String TAG = "LineViewChart";
	ArrayList<Line> Lines = new ArrayList();
	boolean drawGrid = false;
	public LineChartView(Context context){
	   super(context);
	}

	public LineChartView(Context context, AttributeSet attrs) {
	    this(context, attrs, 0);
	}
	
	public LineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
    private float Dip(int value) {
    	Resources r = getResources();
    	float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, r.getDisplayMetrics());
    	return  px;

    }
	
	private void DrawDataPointMarker(float CenterX, float CenterY, Canvas canvas, Paint centerColor){
	    Paint markerColor = new Paint();
	    markerColor.setAntiAlias(true);
	    markerColor.setStyle(Paint.Style.FILL_AND_STROKE);
	    markerColor.setColor(Color.BLACK);
	    canvas.drawCircle(CenterX + 1.0F, CenterY + 1.0F, Dip(4), markerColor);
	    canvas.drawCircle(CenterX + 1.0F, CenterY + 1.0F, Dip(2), centerColor);
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
	   // grids = paramBoolean2;
	    invalidate();
	  }

	  private void DrawGrid(int max, int datapoints, Canvas canvas)
	  {
	    int w = getWidth();
	    int h = -24 + getHeight();
	    Paint GridPaint = new Paint();
	    GridPaint.setColor(Color.GRAY);
	    double gridSizeH = 0;
	    double gridSizeW = 0;
	    //Calculate gridline spacing
	    if ((max > 60) && (max < 1200))
	    {
	    	gridSizeH = h / (max / 60);
	    	gridSizeW = w / datapoints;
	    } else {
	        gridSizeH = 30.0D;
	        gridSizeW = 30.0D;
	    }
	    int m = 0;
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
	        int highest = 0;
	        ArrayList<DataPoints> Points = null;
	        //Find highest value. We'll scale the chart according to that.
	        for (Line l: Lines) {
	        	Points = l.getPoints();
	        	for (DataPoints dp : Points) {
	        		if (highest < dp.getPoint())
	        			highest = dp.getPoint();
	        	}	
	        }
	        //This is so that we don't get a ArithmeticException because we divid by zero.
	        //Both in the running app, but also in the GUI-designer.
	        if (highest < 1)
	        	return;
	        //This calculates the x-distans between the datapoints.
	       // int ValuePerPixelY = h / (highest / 2);
	        int ValuePerPixelX = w / Lines.get(0).getPoints().size();
	        //Draw grid
	        DrawGrid(highest, Lines.get(0).getPoints().size(), canvas);
	        for (Line l : Lines ) {
	        	Points = l.getPoints();
	        	ArrayList<Point> pointArray = new ArrayList<Point>();
	        	for (int i = 0; i < Points.size(); i++) {
	        		Point p = new Point(ValuePerPixelX * (i) ,(Points.get(i).getPoint() *h) / highest);
	        		pointArray.add(p);
	        	}
	        	
	        	for (int i = 0; i < pointArray.size(); i++) {
	        		float StartX = 0;
	        		float StartY = 0;
	        		float StopX = 0;
	        		float StopY = 0;
	        		
	        		if (!Double.isNaN(pointArray.get(i).getX())) {
	        			StartX = (float)pointArray.get(i).getX();
	        		} else { StartX = 0f; }
	        		
	        		if (!Double.isNaN(pointArray.get(i).getY())) {
	        			StartY = (float)pointArray.get(i).getY();
	        		} else { StartY = 0f; }
	        		if (i < pointArray.size() -1) {
	        			
	        			if (!Double.isNaN(pointArray.get(i + 1).getX())) {
	        				StopX = (float)pointArray.get(i + 1).getX();
	        			} else { StopX = 0f; }
	        		
	        			if (!Double.isNaN(pointArray.get(i + 1).getY())) {
	        				StopY = (float)pointArray.get(i + 1).getY();
	        			} else { StopY = 0f; }
	        		
	        		} else {
	        			StopX = StartX;
	        			StopY = StartY;
	        		}
	        		//Remember that in Java-canvas, point 0 is not the bottom, it's the top of the canvas		
	        		canvas.drawLine( 10 + StartX, -12 + getHeight() - StartY, 10 + StopX, -12 + getHeight() - StopY, l.getColor());
	        		DrawDataPointMarker(10 + StartX, -12 + getHeight() - StartY, canvas, l.getColor());
	        		if (i == pointArray.size() -1) {
	        			DrawDataPointMarker(10 + StopX, -12 + getHeight() - StopY, canvas, l.getColor());
	        		}
	        	}
	        	
	        	
	        }
	        
	        
		}

}
