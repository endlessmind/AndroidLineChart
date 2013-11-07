package com.example.linechart;

public class Point
{
  private double _x;
  private double _y;

  public double getX()
  {
    return _x;
  }

  public double getY()
  {
    return _y;
  }

  public void setX(double value)
  {
    _x = value;
  }

  public void setY(double value)
  {
    _y = value;
  }
  
  public Point(double x, double y) {
	  this._x = x;
	  this._y = y;
  }
}
