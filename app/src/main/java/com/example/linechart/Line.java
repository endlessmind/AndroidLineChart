package com.example.linechart;


import android.graphics.Paint;

import java.util.ArrayList;

public class Line {
    private Paint _color;
    private String _name;
    private ArrayList<DataPoints> _points;
    private String _xUnit;
    private String _yUnit;

    public Paint getColor() {
        return _color;
    }

    public void setColor(Paint value) {
        _color = value;
    }

    public String getName() {
        return _name;
    }

    public void setName(String value) {
        _name = value;
    }

    public ArrayList<DataPoints> getPoints() {
        return _points;
    }

    public void setPoints(ArrayList<DataPoints> value) {
        _points = value;
    }

    public String getXUnit() {
        return _xUnit;
    }

    public void setXUnit(String value) {
        _xUnit = value;
    }

    public String getYUnit() {
        return _yUnit;
    }

    public void setYUnit(String value) {
        _yUnit = value;
    }
}