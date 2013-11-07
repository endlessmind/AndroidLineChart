package com.example.linechart;

import java.util.ArrayList;
import java.util.Random;

import android.os.Bundle;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class MainActivity extends Activity {

	private String TAG = "MainLineChart";
	LineChartView lcw;
	EditText editText1;
	int count = 30;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		lcw = (LineChartView) findViewById(R.id.lineChartView1);
		lcw.setOnClickListener(lcw_Click);
		editText1 = (EditText) findViewById(R.id.editText1);
		editText1.addTextChangedListener(editText1_tw);
		createLines(count);
	}


    private float Dip(int value) {
    	Resources r = getResources();
    	float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, r.getDisplayMetrics());
    	return  px;

    }
    
    private TextWatcher editText1_tw = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable arg0) {
			if (editText1.getText().toString() != "") {
				try {
					count = Integer.parseInt(editText1.getText().toString());
					createLines(count);
				} catch (Exception e) { }
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}
    	
    };
    
    private OnClickListener lcw_Click = new OnClickListener() {

		@Override
		public void onClick(View v) {
			createLines(count);
		}
    	
    };
	
	private void createLines(int numberOfData) {
		Line line = new Line();
		Paint p = new Paint();
		p.setColor(Color.BLUE);
		p.setStrokeWidth(Dip(2));
		p.setAntiAlias(true);
		line.setColor(p);
		Random r = new Random();
		ArrayList<DataPoints> points = new ArrayList<DataPoints>();
		for (int i = 0; i < numberOfData; i++) {
			DataPoints dp = new DataPoints();
			dp.setInfo("bla");
			dp.setPoint((int)Math.round(Math.cbrt((double)i +1)* (10 * (Math.sin((double)i +1) + 1) ) ));
			points.add(dp);
		}
		line.setPoints(points);
		ArrayList<Line> lines = new ArrayList<Line>();
		lines.add(line);
		if (lcw != null)
		lcw.UpdateChart(lines, false);
		
	}
	
}