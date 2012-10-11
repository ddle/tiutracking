package edu.pdx.capstone.tiutracking.kengine;

import java.io.Serializable;

/**
 * Path loss model: RSSI(d) = c - n ln(d), where c = RSSI(1m).
 * 
 * @author Kin
 * 
 */
public class PathLossModel implements Serializable {

	private static final long serialVersionUID = 8828994075222256415L;

	private double c;
	private double n;

	public PathLossModel() {
		c = 200;
		n = 10;
	}

	public double rssiToDistance(int rssi) {
		return Math.exp((c - rssi) / n);
	}

	/**
	 * Adjusts the model's parameters to better estimate distance, given an RSSI
	 * value measured between a tag and a detector and a learning rate.
	 * <p>
	 * The path loss model adjusts its parameters using Delta learning rule from
	 * neural network.
	 * </p>
	 * 
	 * @param rssi
	 *            The RSSI value derived from a list of RSSI values.
	 * @param distance
	 *            The real distance between the tag and the detector.
	 * @param learningRate
	 *            The learning rate.
	 */
	public void learn(int rssi, double distance, double learningRate) {
		/*
		 * Input vector:	x = [1, -ln(d)]^T
		 * Weight vector:	w = [c, n]^T
		 * Activation func:	phi(t) = t
		 * Output RSSI:	 	y = phi(x dot w) = c - n * ln d
		 * Desired RSSI: 	t
		 * Learning Rate:	eta
		 * dy/dc = (t - y) * (d phi/dt) * x[1]
		 * dy/dn = (t - y) * (d phi/dt) * x[2]
		 * c += eta * dy/dc
		 * n += eta * dy/dn
		 */
		double lnd = Math.log(distance);
		double localGrad = learningRate * (rssi - (c - n * lnd));
		c += localGrad;
		n -= localGrad * lnd;
	}
	
	public String toString() {
		return "<c: " + c + ", n: " + n + ">";
	}
}
