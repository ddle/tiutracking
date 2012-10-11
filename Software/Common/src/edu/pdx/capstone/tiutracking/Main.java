package edu.pdx.capstone.tiutracking;

import java.util.ArrayList;

import edu.pdx.capstone.tiutracking.common.ConfigurationParam;
import edu.pdx.capstone.tiutracking.common.StatisticMode;

public class Main {

	public static void main(String[] args) {

		ArrayList<ConfigurationParam> params = new ArrayList<ConfigurationParam>();
		params.add(new ConfigurationParam("A", null, 0.1, 0.0, 1.0));
		params.add(new ConfigurationParam("B", null, StatisticMode.MAX, null, null));
		params.add(new ConfigurationParam("C", null, false, null, null));

		for (ConfigurationParam p : params) {
			String[] list = p.getValueArray();
			if (list != null) {
				for (String item : list)
					System.out.println(item);
			} else {
				System.out.println("Not an enum");
			}
			System.out.println("-----------------");
		}
		ConfigurationParam p = new ConfigurationParam("Holly", "", 5, 0, 10);
		System.out.println(p.getValue() + " " + p.minValue + " " + p.maxValue);
		p.setValue("3");
		System.out.println(p.getValue() + " " + p.minValue + " " + p.maxValue);
		p.setValue("-8");
	}

}
