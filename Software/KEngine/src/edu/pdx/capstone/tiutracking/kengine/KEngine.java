package edu.pdx.capstone.tiutracking.kengine;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Set;

import edu.pdx.capstone.tiutracking.common.*;

/**
 * An implementation of the location engine based on path loss model.
 * <p>
 * The engine creates a path loss model for each detector and uses Delta Rule
 * from neural networks to adjust the models during the learning phase.
 * </p>
 * <p>
 * To locate a tag, the engine uses a simple algorithm in which each detector
 * imposes a force on the tag. The magnitude of the force is proportional to the
 * difference between the distance derived from an RSSI list and the current
 * distance. The net force causes the tag to move. After some amount of time,
 * the tag will be at a location at which the net force is virtually zero. That
 * location is the result of the locating process.
 * </p>
 * <p>
 * In case the algorithm does not converge, a max iteration count is used to
 * ensure that the algorithm will eventually stop.
 * </p>
 * 
 * @author Kin
 * 
 */
public class KEngine implements LocationEngine {

	private static final String DATA_FILE_NAME = "KEngine.dat";
	private static final String CONFIG_FILE_NAME = "KEngine.cfg";

	private static final int
		PARAM_LEARNING_CYCLE = 0,
		PARAM_LEARNING_RATE = 1,
		PARAM_MAX_ITERATIONS = 2,
		PARAM_STATISTIC_MODE = 3,
		PARAM_FORCE_THRESHOLD = 4,
		PARAM_VELOCITY_FACTOR = 5;

	private int maxIterations = 20;
	private StatisticMode statisticMode = StatisticMode.MEDIAN;
	private double forceThreshold = 1.0D;
	private double velocityFactor = 0.5D;

	// Table containing all congiruation params of this engine.
	private ArrayList<ConfigurationParam> configuration;

	// Table containing the path loss models for all detectors.
	private Hashtable<Integer, PathLossModel> pathLossModels;

	// Table holding the locations of all detectors.
	private Hashtable<Integer, Vector2D> detectorLocations;

	// List of detectors involved in the current locating request.
	private ArrayList<DetectorInfo> involvedDetectors = new ArrayList<DetectorInfo>();

	@SuppressWarnings("unchecked")
	public KEngine() {

		// Load configuration file if exists.
		configuration = (ArrayList<ConfigurationParam>) ObjectFiler.load(CONFIG_FILE_NAME);

		// If file does not exist, intialize default configuration.
		if (configuration == null) {

			configuration = new ArrayList<ConfigurationParam>(6);

			// The order must match PARAM_xx constants.

			configuration.add(new ConfigurationParam(
					"Learning Cycle",
					"Number of cyles that a list of raw data packets is used to adjust the models.",
					10, 1, 100));
			
			configuration.add(new ConfigurationParam(
					"Learning Rate",
					"Learning rate used to adjust the path loss models.",
					0.3D, 0.1D, 1.0D));
			
			configuration.add(new ConfigurationParam(
					"Max Iterations",
					"Number of iterations at which the locating algorithm " +
					"will stop in case it does not converge.",
					maxIterations, 10, 100));
			
			configuration.add(new ConfigurationParam(
					"Statistic Mode",
					"Statistic mode used to determine an 'average' RSSI from a list of raw RSSIs.",
					statisticMode, null, null));
			
			configuration.add(new ConfigurationParam(
					"Force Threshold",
					"Threshold that controls the convergence of the locating algorithm. If the " +
					"squared magnitude of the net force is below this value, the algorithm will stop.",
					forceThreshold, 0.2D, 2.0D));
			
			configuration.add(new ConfigurationParam(
					"Velocity Factor",
					"A scale factor that determines how fast the tag can move per iteration.",
					velocityFactor, 0.1D, 1.0D));
		}

		// Load data file if exists
		pathLossModels = (Hashtable<Integer, PathLossModel>) ObjectFiler.load(DATA_FILE_NAME);

		if (pathLossModels == null) {
			pathLossModels = new Hashtable<Integer, PathLossModel>();
		}

	}

	@Override
	public void learn(ArrayList<DataPacket> rawData,
			Hashtable<Integer, Vector2D> detectorLocations) {

		// Save the locations of all detectors for later use.
		this.detectorLocations = detectorLocations;

		int learningCycle = (Integer) configuration.get(PARAM_LEARNING_CYCLE).getValue();
		double learningRate = (Double) configuration.get(PARAM_LEARNING_RATE).getValue();

		// Performs learning algorithm
		
		for (int cycle = learningCycle - 1; cycle >= 0; cycle--) {

			for (DataPacket packet : rawData) {

				Set<Entry<Integer, ArrayList<Integer>>> set = packet.rssiTable.entrySet();

				for (Entry<Integer, ArrayList<Integer>> entry : set) {

					// Prepare data to train the path loss model.
					int detectorId = entry.getKey();
					double distance = packet.location.distanceTo(detectorLocations.get(detectorId));
					int rssi = Statistics.calculate(entry.getValue(), statisticMode);

					// Get the model for this detector if exists, else create a new one.
					PathLossModel model = pathLossModels.get(detectorId);
					if (model == null) {
						model = new PathLossModel();
						pathLossModels.put(detectorId, model);
					}

					// Adjust the model.
					model.learn(rssi, distance, learningRate);
				}
			}
		}

		ObjectFiler.save(DATA_FILE_NAME, pathLossModels);
		
		// Creates a log file for the path loss models.
		try {
			FileWriter writer = new FileWriter("PathLossModels.txt");
			for (Entry<Integer, PathLossModel> entry : pathLossModels.entrySet()) {
				writer.write(entry.getKey() + entry.getValue().toString() + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void locate(DataPacket dataPacket) {

		Set<Entry<Integer, ArrayList<Integer>>> set = dataPacket.rssiTable.entrySet();

		if (set.size() > 0) {

			// Clear the result first.
			Vector2D result = dataPacket.location;
			result.set(0, 0);

			// Gets a list of detectors involving in this locating request.
			involvedDetectors.clear();
			for (Entry<Integer, ArrayList<Integer>> entry : set) {
				int key = entry.getKey();

				PathLossModel model = pathLossModels.get(key);
				Vector2D location = detectorLocations.get(key);
				int rssi = Statistics.calculate(entry.getValue(), statisticMode);
				double distance = model.rssiToDistance(rssi);

				involvedDetectors.add(new DetectorInfo(model, location, distance));
				result.add(location);
			}

			// Initialize the tag's location to be at the centroid of the
			// polygon formed by the involved detectors.
			result.mult(1 / involvedDetectors.size());

			// Initialize the iteration counter.
			int count = 0;

			// Create the force vectors out here to optimize memory.
			Vector2D force = new Vector2D();
			Vector2D netForce = new Vector2D();

			while (true) {

				// Calculate individual forces and net force imposed on the tag.
				for (DetectorInfo info : involvedDetectors) {

					// Get the vector pointing from the detector to the tag.
					force.x = info.location.x - result.x;
					force.y = info.location.y - result.y;

					// Scale it by the percent of difference between the
					// measured distance and the current distance, resulting
					// in the force imposed by this detector.
					//
					// The force's direction is from detector to tag, and its
					// magnitude
					// is propotional to the difference between the distance
					// derived from
					// RSSI and the current distance.
					force.mult(info.distance / force.mag() - 1);

					// Add to the net force.
					netForce.add(force);
				}

				// Update counter and result.
				count++;
				result.x += velocityFactor * netForce.x;
				result.y += velocityFactor * netForce.y;

				// If convergence criterion is met, stop locating.
				if ((netForce.magSquared() < forceThreshold)
						|| (count >= maxIterations)) {
					return;
				}

				// Reset the net force to zero before next iteration.
				netForce.set(0, 0);
			}
		}
	}

	@Override
	public ArrayList<ConfigurationParam> getConfiguration() {
		return configuration;
	}

	@Override
	public void onConfigurationChanged() {

		// Update configuration params and save the configuration to configuration file. 

		maxIterations  = (Integer)		 configuration.get(PARAM_MAX_ITERATIONS).getValue();
		statisticMode  = (StatisticMode) configuration.get(PARAM_STATISTIC_MODE).getValue();
		forceThreshold = (Double) 		 configuration.get(PARAM_FORCE_THRESHOLD).getValue();
		velocityFactor = (Double) 		 configuration.get(PARAM_VELOCITY_FACTOR).getValue();
		
		ObjectFiler.save(CONFIG_FILE_NAME, configuration);
	}

}
