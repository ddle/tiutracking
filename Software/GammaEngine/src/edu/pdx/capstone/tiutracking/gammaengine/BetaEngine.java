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
import edu.pdx.capstone.tiutracking.common.ObjectFiler;
import edu.pdx.capstone.tiutracking.common.StatisticMode;
import edu.pdx.capstone.tiutracking.common.Statistics;
import edu.pdx.capstone.tiutracking.common.Vector2D;

public class BetaEngine implements LocationEngine {
	
	private final static String CONFIG_FILE_NAME = "neuralnet.cfg";
	
	private final static int
		PARAM_LEARNING_RATE = 0,
		PARAM_MAX_ITERATION = 1,
		PARAM_INPUT_NUMBER  = 2,
		PARAM_OUTPUT_NUMBER = 3,
		PARAM_HIDDEN_NUMBER = 4,
		PARAM_INPUT_RES = 5,
		PARAM_OUTPUT_RES = 6,
		PARAM_LOGIC_HIGH = 7,
		PARAM_STATISTIC_MODE = 8,
		PARAM_ACCURACY = 9;

	// Declare configuration parameters
	private NeuralNetwork gammaNet;
	private static double learningRate = 0.2;
	private static int 
		maxIteration = 10000,
		inputNumber = 4,
		outputNumber = 2,
		hiddenNumber = 50,
		inputRes = 8,
		outputRes = 5,
		trainSize = 0,
		accuracy = 1;
	
	private static double logicHigh = 0.7;
	private StatisticMode statMode = StatisticMode.MEDIAN;
	
	//Table of configuration
	private ArrayList<ConfigurationParam> configuration;

	@SuppressWarnings("unchecked")
	public BetaEngine(){
		// Load configuration from file
		configuration = (ArrayList<ConfigurationParam>) ObjectFiler.load(CONFIG_FILE_NAME);
		
		//If file does not exist, initialize default configuration
		if (configuration == null) {
			
			configuration = new ArrayList<ConfigurationParam>(10);
			
			configuration.add(new ConfigurationParam(
					"Learning Rate",
					"Rate of weight adjustment during learning process",
					learningRate, 0.1, 1.0));
			configuration.add(new ConfigurationParam(
					"Max Iteration",
					"Maximum number of iterations of learning process",
					maxIteration, 5000, 50000));
			configuration.add(new ConfigurationParam(
					"Input Number",
					"Number of input signals which is equal to number of detectors",
					inputNumber, 3, 8));
			configuration.add(new ConfigurationParam(
					"Output Number",
					"Number of outputs, or (x, y) pair",
					outputNumber, 2, 2));
			configuration.add(new ConfigurationParam(
					"Hidden Number",
					"Number of neurons of the hidden layer in MLP network",
					hiddenNumber, 10, 100));
			configuration.add(new ConfigurationParam(
					"Input Resolution",
					"Resolution of Analog to Digital at input",
					inputRes, 8, 8));
			configuration.add(new ConfigurationParam(
					"Output Resolution",
					"Resolution of Digital to Analog at output",
					outputRes, 5, 10));
			configuration.add(new ConfigurationParam(
					"Logic High",
					"Define logic threshold that can be treated as HIGH at output",
					logicHigh, 0.6, 1.0));
			configuration.add(new ConfigurationParam(
					"Statistic Mode",
					"Statistic Mode which is used in learn/recall process",
					statMode, null, null));
			configuration.add(new ConfigurationParam(
					"Accuracy",
					"Accuracy of location (x,y)",
					accuracy, 1, 5));
		}
	}
	
	@Override
	public void learn(ArrayList<DataPacket> rawData,
			Hashtable<Integer, Vector2D> detectorLocations) {

		//Set parameter from loaded configuration
		System.out.println("Loading configuration...");
		learningRate = (Double) configuration.get(PARAM_LEARNING_RATE).getValue();
		maxIteration = (Integer) configuration.get(PARAM_MAX_ITERATION).getValue();
		inputNumber = (Integer) configuration.get(PARAM_INPUT_NUMBER).getValue();
		outputNumber = (Integer)configuration.get(PARAM_OUTPUT_NUMBER).getValue();
		hiddenNumber = (Integer) configuration.get(PARAM_HIDDEN_NUMBER).getValue();
		inputRes = (Integer) configuration.get(PARAM_INPUT_RES).getValue();
		outputRes = (Integer) configuration.get(PARAM_OUTPUT_RES).getValue();
		logicHigh = (Integer) configuration.get(PARAM_LOGIC_HIGH).getValue();
		statMode = (StatisticMode) configuration.get(PARAM_STATISTIC_MODE).getValue();
		accuracy = (Integer) configuration.get(PARAM_ACCURACY).getValue();
	
		System.out.println("Load successfully!");
		System.out.println("Learning Rate: " + learningRate);
		System.out.println("Max Iteration: " + maxIteration);
		System.out.println("Number of inputs: " + inputNumber);
		System.out.println("Number of outputs: " + outputNumber);
		System.out.println("Number of hidden neurons: " + hiddenNumber);
		System.out.println("Input resolution: " + inputRes);
		System.out.println("Output resolution: " + outputRes);
		System.out.println("Defined logic HIGH: " + logicHigh);
		System.out.println("Statistic Mode: " + statMode);
		
		trainSize = rawData.size();
		
		double[][] inputTrain = new double[trainSize][inputNumber * inputRes];
		double[][] outputTrain = new double[trainSize][outputNumber * outputRes];

		/*
		 * For each dataPacket, process rssiTable Add input patterns to training
		 * set Add supervised output pattern to training set Create three input
		 * patterns by using three different statistic mode MAX, MIN, MEAN
		 */
		for (int row = 0; row < trainSize; row++) {
			DataPacket dataPacket = rawData.get(row);
			Vector2D refLocation = dataPacket.location;

			// Create input pattern
			createPattern(dataPacket.rssiTable, statMode, inputTrain[row]);

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
					inputNumber * inputRes, hiddenNumber, outputNumber * outputRes);
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
		double[] inputPattern = new double[inputRes];
		createPattern(dataPacket.rssiTable, statMode, inputPattern);

		// Load the neural network
		NeuralNetwork myNet = NeuralNetwork.load("locator.nnet");

		myNet.setInput(inputPattern); // Set input pattern
		myNet.calculate(); // Calculate output

		// Return location (x,y) by modifying dataPacket.location
		double[] output = myNet.getOutputAsArray();
		for (int j = 0; j < outputNumber * outputRes; j++) {
			if (output[j] > logicHigh) {
				output[j] = 1.0; // Logic high
			} else {
				output[j] = 0.0; // Logic low
			}
		}

		Vector2D result = new Vector2D();
		Converter.digitalToAnalog(output, result, outputRes);
		dataPacket.location.set(result.x * accuracy, result.y * accuracy);

	}

	@Override
	public ArrayList<ConfigurationParam> getConfiguration() {

		return configuration;
	}

	@Override
	public void onConfigurationChanged() {
		//Update parameters
		System.out.println("Update parameters...");
		learningRate = (Double) configuration.get(PARAM_LEARNING_RATE).getValue();
		maxIteration = (Integer) configuration.get(PARAM_MAX_ITERATION).getValue();
		inputNumber = (Integer) configuration.get(PARAM_INPUT_NUMBER).getValue();
		outputNumber = (Integer)configuration.get(PARAM_OUTPUT_NUMBER).getValue();
		hiddenNumber = (Integer) configuration.get(PARAM_HIDDEN_NUMBER).getValue();
		inputRes = (Integer) configuration.get(PARAM_INPUT_RES).getValue();
		outputRes = (Integer) configuration.get(PARAM_OUTPUT_RES).getValue();
		logicHigh = (Integer) configuration.get(PARAM_LOGIC_HIGH).getValue();
		statMode = (StatisticMode) configuration.get(PARAM_STATISTIC_MODE).getValue();
		accuracy = (Integer) configuration.get(PARAM_ACCURACY).getValue();
		
		//Save configuration
		ObjectFiler.save(CONFIG_FILE_NAME, configuration);
	}

	private void createPattern(
			Hashtable<Integer, ArrayList<Integer>> rssiTable,
			StatisticMode mode, double[] inputPattern) {
		Set<Entry<Integer, ArrayList<Integer>>> set = rssiTable.entrySet();

		int index = 0;

		for (Entry<Integer, ArrayList<Integer>> entry : set) {
			int rssi = Statistics.calculate(entry.getValue(), mode);
			double[] conversionBuffer = new double[inputRes];

			Converter.analogToDigital(rssi, conversionBuffer);

			for (int i = 0; i < conversionBuffer.length; i++) {
				inputPattern[index * inputRes + i] = conversionBuffer[i];
			}

			index++;
		}

	}

	private void createTarget(Vector2D refLocation, double[] outputTarget) {
		double[] xBuffer = new double[outputRes];
		double[] yBuffer = new double[outputRes];

		Converter.analogToDigital(((int) refLocation.x)/accuracy, xBuffer);
		Converter.analogToDigital(((int) refLocation.y)/accuracy, yBuffer);

		for (int i = 0; i < outputRes; i++) {
			outputTarget[i] = xBuffer[i];
			outputTarget[i + outputRes] = yBuffer[i];
		}
	}
}
