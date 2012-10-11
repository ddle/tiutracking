package edu.pdx.capstone.tiutracking.kengine;

import edu.pdx.capstone.tiutracking.common.Vector2D;

public class DetectorInfo {

	public final PathLossModel pathLossModel;
	public final Vector2D location;
	public final double distance;

	/**
	 * Creates a DetectorInfo object.
	 * 
	 * @param pathLossModel
	 *            The path loss model for this detector.
	 * @param location
	 *            The location of this detector.
	 * @param distance
	 *            The distance from this detector to the tag of interest.
	 */
	public DetectorInfo(PathLossModel pathLossModel, Vector2D location,
			double distance) {
		this.pathLossModel = pathLossModel;
		this.location = location;
		this.distance = distance;
	}
}
