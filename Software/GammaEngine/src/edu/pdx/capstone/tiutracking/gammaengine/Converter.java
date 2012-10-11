package edu.pdx.capstone.tiutracking.gammaengine;

import edu.pdx.capstone.tiutracking.common.Vector2D;

public class Converter {
	
	public static void analogToDigital(int signal, double[] digital) {
		for (int i = 0; i < digital.length; i++) {
			digital[i] = signal%2;
			signal = signal/2;
		}
	}
		
	public static void digitalToAnalog(double[] output, Vector2D v, int offset) {
		for (int i = 0; i < output.length / 2; i++) {
			v.x += (Math.pow(2, i)) * output[i];
			v.y += (Math.pow(2, i)) * output[i + offset];
		}
	}
}
