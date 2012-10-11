package edu.pdx.capstone.tiutracking.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * This utility class provides two useful methods for writing an object to or
 * reading an object from a file.
 * 
 * @author Kin
 * 
 */
public class ObjectFiler {

	/**
	 * Loads an object from a file
	 * 
	 * @param fileName
	 *            Name of the file to be read
	 * @return The object read from the file or <b>null</b> if there is any
	 *         error.
	 */
	public static Object load(String fileName) {

		File f = new File(fileName);
		if (f.exists()) {
			ObjectInputStream in;
			try {
				in = new ObjectInputStream(new FileInputStream(f));
				Object result = in.readObject();
				in.close();
				return result;

			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Saves an object to a file
	 * 
	 * @param fileName
	 *            Name of the file to be written
	 * @param obj
	 *            The object to be written
	 * @return True if successful, else false.
	 */
	public static boolean save(String fileName, Object obj) {

		try {
			ObjectOutputStream out = new ObjectOutputStream(
					new FileOutputStream(fileName));
			out.writeObject(obj);
			out.close();
		} catch (Exception e) {
			return false;
		}

		return true;
	}

}
