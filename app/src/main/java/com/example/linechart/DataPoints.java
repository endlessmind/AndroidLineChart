package com.example.linechart;

public class DataPoints {

    public DataPoints() { draw = true; }

    public DataPoints(boolean d) {
        draw = d;
    }
    private String _info;
    private float _point;



    private boolean draw;

    public String getInfo() {
        return _info;
    }

    public void setInfo(String value) {
        _info = value;
    }

    public float getPoint() {
        return _point;
    }

    public void setPoint(int value) {
        _point = value;
    }

    public boolean shouldDraw() {
        return draw;
    }

    public void setDraw(boolean draw) {
        this.draw = draw;
    }
}
