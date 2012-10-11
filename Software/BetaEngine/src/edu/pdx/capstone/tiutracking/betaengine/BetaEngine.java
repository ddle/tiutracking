package edu.pdx.capstone.tiutracking.betaengine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

import edu.pdx.capstone.tiutracking.common.DataPacket;
import edu.pdx.capstone.tiutracking.common.ObjectFiler;
import edu.pdx.capstone.tiutracking.common.Statistics;
import edu.pdx.capstone.tiutracking.common.Vector2D;



public class BetaEngine {
	private double learningRate;
	private ArrayList<Neuron> neurons;
	private double sigma;
	private final String CONFIG_FILENAME = "AlphaEngine.cfg";
	private final double NEARNESS = 0.1;

	/**
	 * Creates an instance of AlphaEngine class which is implemented as a neural
	 * network Load configuration from AlphaEngine.cfg
	 * 
	 */

	@SuppressWarnings("unchecked")
	public BetaEngine() {

		// Initialize parameters of the neural network
		learningRate = 0.2;
		sigma = 1.0;

		/*
		 * Load the configuration from file Create an ArrayList of neurons
		 */
		try {
			neurons = (ArrayList<Neuron>) ObjectFiler.load(CONFIG_FILENAME);
			System.out.println("Load configuration sucessfully!");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Learning process The neural network processes rawData and generates the
	 * training set The learning process is supervised by using the known
	 * locations of asset tags provided in rawData
	 * 
	 * @param rawData
	 *            An ArrayList of DataPacket type which contains RSSI values and
	 *            reference location for each asset tag
	 * @param detectors
	 *            A hash table which contains known locations of detectors
	 */
	public void learn(ArrayList<DataPacket> rawData,
			Hashtable<Integer, Vector2D> detectors) {

		System.out.println("Learning mode...");
		for (int index = 0; index < rawData.size(); index++) {
			Hashtable<Integer, ArrayList<Integer>> rssiTable = rawData
					.get(index).rssiTable;
			Vector2D refLocation = rawData.get(index).location;
			train(rssiTable, refLocation);

		}
		System.out.println("Learning successful!");

		// Save the configuration of the neural network
		try {
			ObjectFiler.save(CONFIG_FILENAME, neurons);
			System.out.println("Saved configuration.");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Locates asset tags base on measured RSSI values
	 * 
	 * @param dataPacket
	 *            A data packet which contains RSSI values measured by detectors
	 *            from specified tag
	 * @return Modify dataPacket.location
	 */
	public void locate(DataPacket dataPacket) {
		Pattern pattern = new Pattern();
		preprocess(dataPacket.rssiTable, pattern);

		Vector2D resultMax = new Vector2D();
		Vector2D resultMin = new Vector2D();
		Vector2D resultMean = new Vector2D();
		Vector2D resultMedian = new Vector2D();

		recall(resultMax, pattern.max);
		recall(resultMin, pattern.min);
		recall(resultMean, pattern.mean);
		recall(resultMedian, pattern.median);

		/*
		 * Modified location variable in dataPacket In this case, location is
		 * determined by applying pattern.max
		 */
		dataPacket.location.set(resultMax);

	}

	/**
	 * Gets configuration of the neural net
	 * 
	 * @return A hash table which contains configuration's info
	 */
	public Hashtable<String, String> getConfiguration() {

		return null;
	}

	/**
	 * Load configuration of the neural network
	 * 
	 * <p>
	 * Process the given hash table and update new configuration of the neural
	 * network
	 * 
	 * @param config
	 *            A hash table which contains configuration's info
	 */
	public void setConfiguration(Hashtable<String, String> config) {

	}

	private void train(Hashtable<Integer, ArrayList<Integer>> rssiTable,
			Vector2D refLocation) {
		Pattern pattern = new Pattern();
		preprocess(rssiTable, pattern);

		// Trains the neural net using pattern.max
		for (Neuron neuron : neurons) {
			// if the reference location is around one existed outstar Vector2D
			if (neuron.outstar.distanceTo(refLocation) <= NEARNESS) {
				linearInterpolate(neuron.instar, neuron.instar, pattern.max,
						learningRate);
				return;
			}
		}

		// There is no such neuron, then add a new neuron which has outstar
		// weights specified by refLocation
		neurons.add(new Neuron(pattern.max, refLocation));
	}

	private void preprocess(Hashtable<Integer, ArrayList<Integer>> rssiTable,
			Pattern pattern) {
		int index = 0;

		for (ArrayList<Integer> v : rssiTable.values()) {
			pattern.max.add(index, (double) Collections.max(v));
			pattern.min.add(index, (double) Collections.min(v));
			pattern.mean.add(index, (double) Statistics.mean(v));
			pattern.median.add(index, (double) Statistics.median(v));

		}
	}

	private void linearInterpolate(ArrayList<Double> result,
			ArrayList<Double> a, ArrayList<Double> b, double factor) {

		assert (result.size() == a.size()) && (a.size() == b.size());

		for (int i = result.size() - 1; i >= 0; i--) {
			double s = a.get(i);
			double d = b.get(i);
			result.set(i, s + (d - s) * factor);
		}
	}

	// Calculate distance between input vector and instar vector
	private double distance(ArrayList<Double> input, ArrayList<Double> instar) {
		assert (input.size() == instar.size());

		double result = 0;

		for (int i = 0; i < input.size(); i++) {
			result += Math.pow((input.get(i) - instar.get(i)), 2);
		}

		return Math.sqrt(result);
	}

	private double gaussian(double distance) {
		return Math.exp(-(distance * distance) / (2 * sigma * sigma));
	}

	private void recall(Vector2D result, ArrayList<Double> input) {
		double hsum = 0;
		double h;
		for (Neuron neuron : neurons) {
			h = gaussian(distance(neuron.instar, input));
			hsum += h;
			result.x += h * neuron.outstar.x;
			result.y += h * neuron.outstar.y;
		}
		result.mult(1 / hsum);
	}

}
