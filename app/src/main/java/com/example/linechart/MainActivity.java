package com.example.linechart;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.os.Bundle;
import android.support.v4.internal.view.SupportMenu;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

import com.example.linechart.LineChartView.OnDataPointMarkedListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {

    LineChartView lcw;
    EditText editText1;
    Button btnToggle;

    TextView tv3;
    CheckBox cb1, checkBox2;
    int count = 30;
    @SuppressWarnings("unused")
    private String TAG = "MainLineChart";
    private TextWatcher editText1_tw = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable arg0) {
            if (editText1.getText().toString() != "") {
                try {
                    count = Integer.parseInt(editText1.getText().toString());
                    createLines(count);
                } catch (Exception e) {
                }
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
        public void onCheckedChanged(CompoundButton view, boolean value) {
            if (view == cb1) {
                if (value)
                    lcw.addShadow();
                else
                    lcw.removeShadow();
            } else if (view == checkBox2) {
                lcw.setIndividualMax(!lcw.isIndividualMax());
            }

        }

    };
    private OnDataPointMarkedListener DataPointMarked = new OnDataPointMarkedListener() {

        @Override
        public void onDataPointsMarked(ArrayList<MarkedData> value) {
            for (MarkedData md : value) {
                if (md.MarkedValues != null && md.MarkedValues.size() > 0)
                    tv3.setText("Marked value: " + md.MarkedValues.get(0) + " from " + md.line.getName());
            }
        }

    };

    private LineChartView.OnInfoNeedParseListener parse_list = new LineChartView.OnInfoNeedParseListener() {
        public String onCustomParse(ArrayList<MarkedData> data) {
            String value = "";
            Iterator<MarkedData> it = data.iterator();
            while (it.hasNext()) {
                MarkedData md = it.next();
                if (md.MarkedValues != null && md.MarkedValues.size() > 0) {
                    //value = String.valueOf(value) + "Value:" + md.MarkedValues.get(0) + " tutte " + md.line.getName() + "\n";
                    value = String.valueOf(value) + md.line.getName() + ":" + md.MarkedValues.get(0)+ " - ";
                }
            }
            return value;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lcw = findViewById(R.id.lineChartView1);
        lcw.setOnClickListener(lcw_Click);
        lcw.setOnDataPointMarkedListener(DataPointMarked);
        lcw.setOnInfoNeedParseListener(this.parse_list);
        editText1 = findViewById(R.id.editText1);
        btnToggle = findViewById(R.id.btnToggle);
        tv3 = findViewById(R.id.textView3);
        editText1.addTextChangedListener(editText1_tw);
        cb1 = findViewById(R.id.checkBox1);
        checkBox2 = findViewById(R.id.checkBox2);
        cb1.setOnCheckedChangeListener(CheckChange);
        checkBox2.setOnCheckedChangeListener(CheckChange);
        createLines(count);
        this.btnToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                lcw.showInfo(!lcw.isInfoVisible());
            }
        });

    }

    private float Dip(int value) {
        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, r.getDisplayMetrics());
        return px;

    }

    public void createLines(int numberOfData) {
        Line line = new Line();
        Line line2 = new Line();
        Line line3 = new Line();
        line.setName("köket");
        line2.setName("sovrummet");
        Paint p = new Paint();
        p.setColor(-16776961);
        p.setStrokeWidth(Dip(3));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStyle(Paint.Style.STROKE);
        p.setAntiAlias(true);
        line.setColor(p);
        Paint p2 = new Paint();
        p2.setColor(SupportMenu.CATEGORY_MASK);
        p2.setStrokeWidth(Dip(3));
        p2.setStrokeCap(Paint.Cap.ROUND);
        p2.setStyle(Paint.Style.STROKE);
        p2.setAntiAlias(true);
        line2.setColor(p2);
        line3.setColor(p);
        new Random();
        ArrayList<DataPoints> points = new ArrayList<>();
        ArrayList<DataPoints> points2 = new ArrayList<>();
        ArrayList<DataPoints> points3 = new ArrayList<>();
        for (int i = 0; i < numberOfData; i++) {
            DataPoints dp = new DataPoints();
            dp.setInfo("bla");
            dp.setPoint((int) Math.round(Math.cbrt(((double) i) + ((double) new Random().nextInt(10))) * 10.0d * (Math.sin(((double) i) + 1.0d) + ((double) new Random().nextInt((int) Math.round((((double) i) * 4.61d) + 4.0d))))));
            points.add(dp);
            double r1 = new Random().nextInt(15);
            double r2 = new Random().nextInt((int) Math.round((((double) i) * 4.83d) + 5.0d));
            DataPoints dp3 = new DataPoints();
            dp3.setInfo("bla");
            dp3.setPoint((int) Math.round(Math.cbrt(((double) i) + r1) * 5.0d * (Math.sin(((double) i) + 1.0d) + r2)));
            points3.add(dp3);
            if (i < (numberOfData / 4)) { //Generates half the points
                DataPoints dp2 = new DataPoints();
                dp2.setInfo("bla");
                dp2.setPoint((int) Math.round(Math.cbrt(((double) i) +  r1) * 5.0d * (Math.sin(((double) i) + 1.0d) + r2)));
                points2.add(dp2);
            } else if (i > ((numberOfData / 4) * 3)) {
                DataPoints dp2 = new DataPoints();
                dp2.setInfo("bla");
                dp2.setPoint((int) Math.round(Math.cbrt(((double) i) +  r1) * 5.0d * (Math.sin(((double) i) + 1.0d) + r2)));
                points2.add(dp2);
            } else {
                points2.add(new DataPoints(false));
            }
        }
        line.setPoints(points);
        line2.setPoints(points2);
        line3.setPoints(points3);
        ArrayList<Line> lines = new ArrayList<>();
        lines.add(line);
        //lines.add(line3);
        lines.add(line2);

        if (this.lcw != null) {
            this.lcw.UpdateChart(lines, true, false);
        }
    }

}
