package edu.pdx.capstone.tiutracking.betaengine;

import java.io.Serializable;
import java.util.ArrayList;

import edu.pdx.capstone.tiutracking.common.Vector2D;

public class Neuron implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3950975134538979978L;
	public ArrayList<Double> instar;
	public Vector2D outstar;
	
	public Neuron(ArrayList<Double> instar, Vector2D outstar) {
		this.instar = new ArrayList<Double>(instar);
		this.outstar = new Vector2D(outstar);
		
	}

}
