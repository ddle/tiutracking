package edu.pdx.capstone.tiutracking;

import edu.pdx.capstone.tiutracking.common.ConfigurationParam;
import edu.pdx.capstone.tiutracking.common.LocationEngine;
import edu.pdx.capstone.tiutracking.kengine.KEngine;

public class Main {

	public static void main(String[] args) {

		LocationEngine engine = new KEngine();
		for (ConfigurationParam p : engine.getConfiguration()) {
			System.out.println(p.name);
			System.out.println(p.description);
			System.out.println(p.getTypeName());
			System.out.println(p.getValue());
			System.out.println(p.getValueArray());
			System.out.println("-------------------------------");
			if (p.name == "Learning Cycle") {
				p.setValue("20");
			}
		}
		engine.onConfigurationChanged();
	}

}
