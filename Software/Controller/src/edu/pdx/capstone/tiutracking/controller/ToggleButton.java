package edu.pdx.capstone.tiutracking.controller;

import javax.swing.JButton;

public class ToggleButton extends JButton{
	private String firstTitle;
	private String secondTitle;
	
	public ToggleButton(String firstTitle, String secondTitle){
		super(firstTitle);
		this.firstTitle = firstTitle;
		this.secondTitle = secondTitle;
	}
	
	public void toggleTitle(){
		if (this.getText().equals(firstTitle)){
			this.setText(secondTitle);
		}else{
			this.setText(firstTitle);
		}
	}
	
	public String getFirstTitle(){
		return firstTitle;
	}
	public String getSecondTitle(){
		return secondTitle;
	}
}
