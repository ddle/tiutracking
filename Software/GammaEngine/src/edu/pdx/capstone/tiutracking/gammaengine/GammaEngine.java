package edu.pdx.capstone.tiutracking.gammaengine;

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

public class GammaEngine implements LocationEngine {

	private final static int INPUT_NUM = 24;
	private final static int IN_RESOL = 8;
	private final static int OUTPUT_NUM = 10;
	private final static int OUT_RESOL = 5;
	private final static int ACCURACY = 1; // accuracy is 1m for ADC
	private final static int HID_NUM = 10; // number of neurons of hidden layer
	private final static double LOGIC_LOW = 0.3;
	private final static double LOGIC_HIGH = 0.7;

	private MultiLayerPerceptron gammaNet;
	private static StatisticMode mode;
	private static double learningRate;
	private static int maxIteration;
	private static double threshold;

	private static int trainSize;

	@Override
	public void learn(ArrayList<DataPacket> rawData,
			Hashtable<Integer, Vector2D> detectorLocations) {

		/**
		 * About GammaEngine Assume that the system has four detectors RSSI
		 * values from these four detectors will be applied to a neural net to
		 * calculate out the location of the given asset tag
		 */
		// Set configuration
		learningRate = 0.2;
		maxIteration = 10000;
		threshold = 0.8;
		mode = StatisticMode.MEAN;

		// Create training set (input patterns and supervised output)from
		// rawData
		trainSize = rawData.size();
		double[][] inputTrain = new double[trainSize][INPUT_NUM];
		double[][] outputTrain = new double[trainSize][OUTPUT_NUM];

		for (int row = 0; row < trainSize; row++) {
			DataPacket dataPacket = rawData.get(row);
			Vector2D refLocation = dataPacket.location;
			Set<Entry<Integer, ArrayList<Integer>>> set = dataPacket.rssiTable
					.entrySet();

			// Process input pattern
			int index = INPUT_NUM / IN_RESOL - 1;

			for (Entry<Integer, ArrayList<Integer>> entry : set) {
				// int key = entry.getKey();
				int rssi = Statistics.calculate(entry.getValue(), mode);
				double[] conversionBuffer = new double[IN_RESOL];

				analogToDigital(rssi, conversionBuffer);

				for (int i = 0; i < conversionBuffer.length; i++) {
					inputTrain[row][index * IN_RESOL + i] = conversionBuffer[i];
				}
				index--;
			}

			// Process supervised output
			double[] xBuffer = new double[OUT_RESOL];
			double[] yBuffer = new double[OUT_RESOL];

			analogToDigital(((int) refLocation.x) / ACCURACY, xBuffer);
			analogToDigital(((int) refLocation.y) / ACCURACY, yBuffer);

			for (int i = 0; i < OUT_RESOL; i++) {
				outputTrain[row][i] = xBuffer[i];
				outputTrain[row][i + OUT_RESOL] = yBuffer[i];
			}

		}

		// Create a training set
		TrainingSet trainSet = new TrainingSet();
		for (int i = 0; i < inputTrain.length; i++) {
			trainSet.addElement(new SupervisedTrainingElement(inputTrain[i],
					outputTrain[i]));
		}

		// Load the neural network if existed
		// Otherwise, create a new neural network
		gammaNet = new MultiLayerPerceptron(TransferFunctionType.SIGMOID,
				INPUT_NUM, HID_NUM, OUTPUT_NUM);
		IterativeLearning learnRule = new BackPropagation();
		learnRule.setNeuralNetwork(gammaNet);

		// Train the network with the training set
		learnRule.learn(trainSet, maxIteration);
		learnRule.stopLearning();
		
		System.out.println("Learning successful!");

		// Save the neural net
		gammaNet.save("locator.nnet");

	}

	@Override
	public void locate(DataPacket dataPacket) {
		
		System.out.println("Locating mode..");
		NeuralNetwork myNet = NeuralNetwork.load("locator.nnet");
		System.out.println("Loaded neural net!");

		double[] inputPattern = new double[INPUT_NUM];
		Set<Entry<Integer, ArrayList<Integer>>> set = dataPacket.rssiTable
				.entrySet();

		// Process dataPacket and generate input pattern
		int index = INPUT_NUM / IN_RESOL - 1;

		for (Entry<Integer, ArrayList<Integer>> entry : set) {
			// int key = entry.getKey();
			int rssi = Statistics.calculate(entry.getValue(), mode);
			double[] conversionBuffer = new double[IN_RESOL];

			analogToDigital(rssi, conversionBuffer);

			for (int i = 0; i < conversionBuffer.length; i++) {
				inputPattern[index * IN_RESOL + i] = conversionBuffer[i];
			}
			index--;
		}

		
		myNet.setInput(inputPattern); // Apply input pattern
		myNet.calculate(); // Calculate
		
		/* Return location (x, y)
		 * by modifying dataPacket.location
		 */
		double[] output = myNet.getOutputAsArray();
		for (int j = 0; j < OUTPUT_NUM; j++) {
			if (output[j] > LOGIC_HIGH) {
				output[j] = 1.0; //Logic high
			}
			else {
				output[j] = 0.0; //Logic low
			}
		}
			
		Vector2D result = new Vector2D();
		
		digitalToAnalog(output, result);
		
		dataPacket.location.set(result.x * ACCURACY, result.y * ACCURACY);
		
	}

	@Override
	public ArrayList<ConfigurationParam> getConfiguration() {

		return null;
	}

	@Override
	public void onConfigurationChanged() {

	}

	public static void analogToDigital(int signal, double[] digital) {
		for (int i = 0; i < digital.length; i++) {
			digital[i] = signal % 2;
			signal = signal / 2;
		}

	}

	public static void digitalToAnalog(double[] output, Vector2D v) {
		for (int i = 0; i < output.length / 2; i++) {
			v.x += (Math.pow(2, i)) * output[i];
			v.y += (Math.pow(2, i)) * output[i + OUT_RESOL];
		}
	}

}
