package edu.pdx.capstone.tiutracking.controller;
import java.util.ArrayList;

import edu.pdx.capstone.tiutracking.common.*;
import javax.swing.table.AbstractTableModel;

public class ConfigurationTableModel extends AbstractTableModel{

	public String[] columnNames ={"Name", "Description", "Value"};
	
	ArrayList<ConfigurationParam> configParams = null;

	public ConfigurationTableModel(){
		
	}
	public void setData(ArrayList<ConfigurationParam> params){
		configParams = params;
	}
	
	@Override
	public int getColumnCount() {
		return 3;
	}
	
	public String getColumnName(int col){
		return columnNames[col];
	}

	@Override
	public int getRowCount() {
		// TODO Auto-generated method stub
		return configParams.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		// TODO Auto-generated method stub
		switch (columnIndex){
		case 0:
			return configParams.get(rowIndex).getTypeName();
		case 1:
			return configParams.get(rowIndex).description;
		case 2:
			return configParams.get(rowIndex).getValue();
				
		}
		return null;
	}
	@Override
	public boolean isCellEditable(int row, int column) {
		return true;
	}
	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex){
		configParams.get(rowIndex).setValue((String)value);
		fireTableCellUpdated(rowIndex, columnIndex);
	}

}
