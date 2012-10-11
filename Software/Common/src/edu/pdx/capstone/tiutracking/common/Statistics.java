package edu.pdx.capstone.tiutracking.common;

import java.util.ArrayList;
import java.util.Collections;

/**
 * A utility class that calculates various statistic values of an integer array.
 * All functions in this class assume that the data given are valid, i.e.
 * containing at least one element.
 * 
 * @version 1.0
 * 
 * @author Kin
 * 
 */
public class Statistics {

	public static int calculate(ArrayList<Integer> data,
			StatisticMode mode) {
		
		switch (mode) {
		case MAX:
			return Collections.max(data);
		case MEDIAN:
			return median(data);
		case MEAN:
			return mean(data);
		case MIN:
			return Collections.min(data);
		default:
			throw new IllegalArgumentException("Invalid argument: " + mode);
		}
	}

	public static int mean(ArrayList<Integer> data) {
		
		int sum = 0;
		for (int x : data) {
			sum += x;
		}
		return sum / data.size();
	}

	public static int median(ArrayList<Integer> data) {
		
		Collections.sort(data);
		int i = data.size() >> 1;
		if ((data.size() & 1) == 1) {
			return data.get(i);
		}
		return (data.get(i) + data.get(i - 1)) >> 1;
	}

	public static double stdDev(ArrayList<Integer> data) {
		
		int n = data.size();
		if (n == 1) {
			return 0;
		}
		int sum = 0;
		int sumOfSquares = 0;
		for (int x : data) {
			sum += x;
			sumOfSquares += x * x;
		}

		return Math.sqrt(
				(double) (n * sumOfSquares - sum * sum) / (n * (n - 1)));
	}

}
