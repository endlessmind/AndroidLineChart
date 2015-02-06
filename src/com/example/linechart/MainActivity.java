package com.example.linechart;

import java.util.ArrayList;
import java.util.Random;

import com.example.linechart.LineChartView.OnDataPointMarkedListener;

import android.os.Bundle;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class MainActivity extends Activity {

	private String TAG = "MainLineChart";
	LineChartView lcw;
	EditText editText1;
	TextView tv3;
	CheckBox cb1;
	int count = 30;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		lcw = (LineChartView) findViewById(R.id.lineChartView1);
		lcw.setOnClickListener(lcw_Click);
		lcw.setOnDataPointMarkedListener(DataPointMarked);
		editText1 = (EditText) findViewById(R.id.editText1);
		tv3 = (TextView) findViewById(R.id.textView3);
		editText1.addTextChangedListener(editText1_tw);
		cb1 = (CheckBox) findViewById(R.id.checkBox1);
		cb1.setOnCheckedChangeListener(CheckChange);
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
    
    private OnCheckedChangeListener CheckChange = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton arg0, boolean value) {
			if (value)
				lcw.addShadow();
			else
				lcw.removeShadow();
			
		}
    	
    };
    
    private OnDataPointMarkedListener DataPointMarked = new OnDataPointMarkedListener() {

		@Override
		public void onDataPointsMarked(ArrayList<MarkedData> value) {
			for (MarkedData md : value) {
				tv3.setText("Marked value: " + md.MarkedValues.get(0) + " from " + md.line.getName());
			}
		}
    	
    };
	
	private void createLines(int numberOfData) {
		Line line = new Line();
		line.setName("TS" + System.currentTimeMillis());
		Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
		p.setColor(Color.BLUE);
		p.setStyle(Paint.Style.STROKE);
		p.setStrokeCap(Cap.ROUND);
		p.setStrokeWidth(Dip(2));
		line.setColor(p);
		Random r = new Random();
		ArrayList<DataPoints> points = new ArrayList<DataPoints>();
		for (int i = 0; i < numberOfData; i++) {
			DataPoints dp = new DataPoints();
			dp.setInfo("bla");
			
			dp.setPoint((int)Math.round(Math.cbrt((double)i + new Random().nextInt(10))* (10 * (Math.sin((double)i +1) + new Random().nextInt((int)Math.round(i * 12.7D + 10))) ) ));
			points.add(dp);
		}
		line.setPoints(points);
		ArrayList<Line> lines = new ArrayList<Line>();
		lines.add(line);
		if (lcw != null)
		lcw.UpdateChart(lines, false);
		
	}
	
}
