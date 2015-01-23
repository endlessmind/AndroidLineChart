package com.example.linechart;

import java.util.ArrayList;

public class MarkedData {
	
	public ArrayList<Float> MarkedValues;
	public Line line;
	
	public MarkedData() {
	}
	
	public MarkedData(ArrayList<Float> Values, Line l) {
		MarkedValues = Values;
		line = l;
	}

}
