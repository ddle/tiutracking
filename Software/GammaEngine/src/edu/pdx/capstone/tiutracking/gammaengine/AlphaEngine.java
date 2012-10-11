package edu.pdx.capstone.tiutracking.gammaengine;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Set;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.learning.IterativeLearning;
import org.neuroph.core.learning.SupervisedTrainingElement;
import org.neuroph.core.learning.TrainingSet;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.neuroph.util.TransferFunctionType;

import edu.pdx.capstone.tiutracking.common.ConfigurationParam;
import edu.pdx.capstone.tiutracking.common.DataPacket;
import edu.pdx.capstone.tiutracking.common.LocationEngine;
import edu.pdx.capstone.tiutracking.common.StatisticMode;
import edu.pdx.capstone.tiutracking.common.Statistics;
import edu.pdx.capstone.tiutracking.common.Vector2D;

public class AlphaEngine implements LocationEngine {

	// Declare constants
	private final static int INPUT_NUM = 3; //3 inputs, 8 bits each
	private final static int HID_NUM = 50; // number of neurons of hidden layer
	private final static int OUTPUT_NUM = 2; //(x, y) pair, 5 bits each
	
	private final static int IN_RES = 8; // 8-bit input value
	private final static int OUT_RES = 5; // 5-bit output value

	//private final static double LOGIC_LOW = 0.3;
	private final static double LOGIC_HIGH = 0.7;

	// Declare configuration parameters
	private NeuralNetwork gammaNet;
	private static double learningRate;
	private static int maxIteration;
	private static int trainSize;

	@Override
	public void learn(ArrayList<DataPacket> rawData,
			Hashtable<Integer, Vector2D> detectorLocations) {

		// Initialize parameters
		learningRate = 0.2;
		maxIteration = 10000;

		trainSize = rawData.size();
		double[][] inputTrain = new double[trainSize][INPUT_NUM * IN_RES];
		double[][] outputTrain = new double[trainSize][OUTPUT_NUM * OUT_RES];

		/*
		 * For each dataPacket, process rssiTable Add input patterns to training
		 * set Add supervised output pattern to training set Create three input
		 * patterns by using three different statistic mode MAX, MIN, MEAN
		 */
		for (int row = 0; row < trainSize; row++) {
			DataPacket dataPacket = rawData.get(row);
			Vector2D refLocation = dataPacket.location;

			// Create input pattern
			createPattern(dataPacket.rssiTable, StatisticMode.MEDIAN,
					inputTrain[row]);

			// Create supervised output
			createTarget(refLocation, outputTrain[row]);
		}

		// Create training set
		TrainingSet trainSet = new TrainingSet();
		for (int i = 0; i < inputTrain.length; i++) {
			trainSet.addElement(new SupervisedTrainingElement(inputTrain[i],
					outputTrain[i]));
		}

		File netFile = new File("locator.nnet");
		if (!netFile.exists()) { // If the file does not exist, create a new
									// neural network
			gammaNet = new MultiLayerPerceptron(TransferFunctionType.SIGMOID,
					INPUT_NUM * IN_RES, HID_NUM, OUTPUT_NUM * OUT_RES);
		} else { // Else, load the previous neural network file
			gammaNet = NeuralNetwork.load("locator.nnet");
		}

		// Initialize a learning rule for the specified neural network
		IterativeLearning learnRule = new BackPropagation();
		learnRule.setNeuralNetwork(gammaNet);
		// learnRule.setLearningRate(learningRate);

		// Train the network with the training set
		learnRule.learn(trainSet, maxIteration);
		learnRule.stopLearning();

		System.out.println("Learning successful!");

		gammaNet.save("locator.nnet"); // Save the neural net
	}

	@Override
	public void locate(DataPacket dataPacket) {

		File netFile = new File("locator.nnet");

		// If file does not exist, locate FAILED!, return
		if (!netFile.exists()) {
			System.out.println("*.NNET FILE NOT FOUND!");
			return;
		}

		// Process dataPacket to create input pattern
		double[] inputPattern = new double[IN_RES];
		StatisticMode mode = StatisticMode.MEDIAN;
		createPattern(dataPacket.rssiTable, mode, inputPattern);

		// Load the neural network
		NeuralNetwork myNet = NeuralNetwork.load("locator.nnet");

		myNet.setInput(inputPattern); // Set input pattern
		myNet.calculate(); // Calculate output

		// Return location (x,y) by modifying dataPacket.location
		double[] output = myNet.getOutputAsArray();
		for (int j = 0; j < OUTPUT_NUM * OUT_RES; j++) {
			if (output[j] > LOGIC_HIGH) {
				output[j] = 1.0; // Logic high
			} else {
				output[j] = 0.0; // Logic low
			}
		}

		Vector2D result = new Vector2D();
		Converter.digitalToAnalog(output, result, OUT_RES);
		dataPacket.location.set(result.x, result.y);

	}

	@Override
	public ArrayList<ConfigurationParam> getConfiguration() {

		return null;
	}

	@Override
	public void onConfigurationChanged() {

	}

	private void createPattern(
			Hashtable<Integer, ArrayList<Integer>> rssiTable,
			StatisticMode mode, double[] inputPattern) {
		Set<Entry<Integer, ArrayList<Integer>>> set = rssiTable.entrySet();

		int index = 0;

		for (Entry<Integer, ArrayList<Integer>> entry : set) {
			int rssi = Statistics.calculate(entry.getValue(), mode);
			double[] conversionBuffer = new double[IN_RES];

			Converter.analogToDigital(rssi, conversionBuffer);

			for (int i = 0; i < conversionBuffer.length; i++) {
				inputPattern[index * IN_RES + i] = conversionBuffer[i];
			}

			index++;
		}

	}

	private void createTarget(Vector2D refLocation, double[] outputTarget) {
		double[] xBuffer = new double[OUT_RES];
		double[] yBuffer = new double[OUT_RES];

		Converter.analogToDigital((int) refLocation.x, xBuffer);
		Converter.analogToDigital((int) refLocation.y, yBuffer);

		for (int i = 0; i < OUT_RES; i++) {
			outputTarget[i] = xBuffer[i];
			outputTarget[i + OUT_RES] = yBuffer[i];
		}
	}

}
