package edu.pdx.capstone.tiutracking.betaengine;

import java.util.ArrayList;

public class Pattern {
	public ArrayList<Double> max;
	public ArrayList<Double> min;
	public ArrayList<Double> mean;
	public ArrayList<Double> median;
	
	public Pattern() {
		this.max = new ArrayList<Double>();
		this.min = new ArrayList<Double>();
		this.mean = new ArrayList<Double>();
		this.median = new ArrayList<Double>();
	}
}
