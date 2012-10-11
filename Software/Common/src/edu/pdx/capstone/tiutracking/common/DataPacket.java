package edu.pdx.capstone.tiutracking.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

public final class DataPacket implements Serializable {

	private static final long serialVersionUID = -1121353283619537046L;

	public int blockId;

	public final int tagId;

	public Date timestamp;

	/**
	 * Location of the tag.
	 * <p>
	 * In calibration mode, this field is initialized by the controller. In
	 * operation mode, it is modified by the location engine in the locate()
	 * method.
	 * </p>
	 */
	public final Vector2D location;

	/**
	 * Battery level of the tag, ranging from 0 to 100.
	 */
	public int battery;

	/**
	 * A table which contains RSSI values associated with this tag for each
	 * detector.
	 * <p>
	 * To traverse the table, please use rssiTable.entrySet().iterator().
	 * </p>
	 */
	public final Hashtable<Integer, ArrayList<Integer>> rssiTable;

	/**
	 * Creates a instance of Tag class.
	 * 
	 * @param id
	 *            Id of the tag
	 * @param location
	 *            Location of the tag. If this field is NOT <b>null</b>, we are
	 *            in calibration mode.
	 */
	public DataPacket(int blockId, int tagId, Vector2D location) {
		this.blockId = blockId;
		this.tagId = tagId;
		this.timestamp = new Date();
		if (location == null) {
			this.location = new Vector2D();
		} else {
			this.location = new Vector2D(location);
		}
		this.battery = 100;
		this.rssiTable = new Hashtable<Integer, ArrayList<Integer>>();
	}
}
