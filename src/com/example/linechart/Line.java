package com.example.linechart;

import java.util.ArrayList;

import android.graphics.Paint;

public class Line
{
  private Paint _color;
  private String _name;
  private ArrayList<DataPoints> _points;
  private String _xUnit;
  private String _yUnit;

  public Paint getColor()
  {
    return _color;
  }

  public String getName()
  {
    return _name;
  }

  public ArrayList<DataPoints> getPoints()
  {
    return _points;
  }

  public String getXUnit()
  {
    return _xUnit;
  }

  public String getYUnit()
  {
    return _yUnit;
  }

  public void setColor(Paint value)
  {
    _color = value;
  }

  public void setName(String value)
  {
    _name = value;
  }

  public void setPoints(ArrayList<DataPoints> value)
  {
    _points = value;
  }

  public void setXUnit(String value)
  {
    _xUnit = value;
  }

  public void setYUnit(String value)
  {
    _yUnit = value;
  }
}