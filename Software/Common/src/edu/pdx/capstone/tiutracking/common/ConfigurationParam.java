package edu.pdx.capstone.tiutracking.common;

import java.io.Serializable;

/**
 * A class that defines a configuration param.
 * 
 * Supported value types: Integer, Double, Boolean, String, StatisticMode.
 * 
 * @author Kin
 * 
 */
public final class ConfigurationParam implements Serializable {

	private static final long serialVersionUID = -7916728150923528984L;

	public final String name;
	public final String description;
	private Object value;
	public final Object minValue;
	public final Object maxValue;

	/**
	 * Creates a new configuration param.
	 * 
	 * @param name
	 *            Name of this param. Name cannot be null.
	 * @param desc
	 *            The description for this param.
	 * @param value
	 *            The value assigned to this param. The value's type should be
	 *            explicitly defined, e.g. 1.0D for Double.
	 * @param minValue
	 *            Lower bound of the value, used only for integer and floating
	 *            point values, ignored in other cases.
	 * @param maxValue
	 *            Upper bound of the value, used only for integer and floating
	 *            point values, ignored in other cases.
	 * @throws IllegalArgumentException
	 */
	public ConfigurationParam(String name, String desc, Object value,
			Object minValue, Object maxValue) {

		if (name == null) {
			throw new IllegalArgumentException("Name cannot be null.");
		}

		if (value == null) {
			throw new IllegalArgumentException("Value cannot be null.");
		}

		this.name = name;
		this.description = desc;
		this.value = value;
		this.minValue = minValue;
		this.maxValue = maxValue;

		// This is needed to ensure value is in range, and value, minValue, and
		// maxValue are of the same type.
		setValue(value.toString());
	}

	/**
	 * Returns a string representing the value type of this param.
	 * 
	 * @return A string representing the value type of this param.
	 */
	public String getTypeName() {
		return value.getClass().getSimpleName();
	}

	public Object getValue() {
		return value;
	}

	/**
	 * Returns an array of all possible values for this param.
	 * 
	 * @return An array of strings representing possible values if the value is
	 *         an enum, else null. Note that Boolean is considered as an enum.
	 */
	public String[] getValueArray() {

		String[] result = null;
		Class<?> c = value.getClass();
		Object[] values = c.getEnumConstants();
		
		if (values != null) {
			result = new String[values.length];
			for (int i = 0; i < values.length; i++) {
				result[i] = values[i].toString();
			}
		} else if (c == Boolean.class) {
			result = new String[2];
			result[0] = "false";
			result[1] = "true";
		}
		
		return result;
	}

	/**
	 * Sets value of this param from a string. This provides a consistent and
	 * simple interface for the controller to change the param's value since the
	 * controller does not care about the param's type.
	 * 
	 * @param valueStr
	 *            A string representing the value to be assigned to this param.
	 * @throws IllegalArgumentException
	 */
	public void setValue(String valueStr) {

		boolean ok = true;
		Class<?> c = value.getClass();

		if (c == Double.class) {
			Double v = Double.valueOf(valueStr);
			Double vmin = (Double) minValue;
			Double vmax = (Double) maxValue;
			if (v >= vmin && v <= vmax) {
				value = v;
			} else {
				ok = false;
			}
		} else if (c == Integer.class) {
			Integer v = Integer.valueOf(valueStr);
			Integer vmin = (Integer) minValue;
			Integer vmax = (Integer) maxValue;
			if (v >= vmin && v <= vmax) {
				value = v;
			} else {
				ok = false;
			}
		} else if (c == StatisticMode.class) {
			value = StatisticMode.valueOf(valueStr);
		} else if (c == Boolean.class) {
			value = Boolean.valueOf(valueStr);
		} else if (c == String.class) {
			value = valueStr;
		} else {
			throw new IllegalArgumentException("Unsupported value type.");
		}

		if (ok == false) {
			throw new IllegalArgumentException("Value out of range: "
					+ valueStr + ".");
		}
	}

}
