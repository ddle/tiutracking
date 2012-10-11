/* Controller
 * This application fills the role as mediator between 
 * the Proxy, LocationEngine, and Database.
 * 
 * Key Aspects:
 * 	Three modes of operation
 * 		1) Collect; Saves data coming from proxy into a csv file
 * 		2) Calibrate; Saves data coming from proxy into Database.
 * 		3) Locate; Provides previously collected calibration data to location engines.
 * 			Formats data coming from proxy into a format suitable for a Location Engine to
 * 			calculate a location.
 * 
 * Architecture:
 * 	The GUI has 3 buttons; Collect, Calibrate, and Locate. Each has their own event handler.
 * 	There are 3 runnable thread objects; CollectReader, CalibrateReader, and LocateReader. They are responsible for
 *  reading data from a Port, and providing the functionality that is associated with each mode of operation. Each of the 3 buttons starts their 
 *  respective Reader. Each Reader inherits from the PortReader class. The PortReader class provides base 
 *  functionality for reading and parsing incoming data from a proxy.
 *  
 *  CalibrateReader, and LocateReader access a Database.
 * 
 *  Authors: Daniel Ferguson,
 *  Version: 0.9
 */

package edu.pdx.capstone.tiutracking.controller;
import edu.pdx.capstone.tiutracking.common.*;

import edu.pdx.capstone.tiutracking.gammaengine.BetaEngine;
import edu.pdx.capstone.tiutracking.gammaengine.GammaEngine;
import edu.pdx.capstone.tiutracking.kengine.KEngine;
//import edu.pdx.capstone.tiutracking.locator.FingerPrint;

import java.awt.EventQueue;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import gnu.io.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JComboBox;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JPanel;
import java.awt.Color;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.UIManager;

import java.sql.*;

import javax.swing.JTabbedPane;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JCheckBox;

public class Main {

	private enum AppViewMode{
		CALIBRATION,
		COLLECTION,
		LOCATING
	}
	
	private enum LocatorChoices{
		FINGERPRINT,
		BETA,
		KEWTON
	}
	
	private ConcurrentLinkedQueue<String> generalDisplayMessages=null;
	private LocatorChoices locatorChoice=LocatorChoices.KEWTON;
	private LocationEngine locator;
	private AppViewMode appViewMode;
	private JFrame frmController;
	private JComboBox cbxCOMPorts;
	private JTextArea txtOutput;
	
	private CommPort commPort;
	private SerialPort serialPort;
	private Socket socketPort;
	
	Thread writerThread;
	Thread readerThread;
	LocatorReader locatorReader;
	CollectorReader collectorReader;
	CalibratorReader calibratorReader;
	Writer writer;
	private JTextField txtLocationDescription;
	private JLabel lblPreviousLocationDescription;
	private ToggleButton btnStartCalibrating;
	private ToggleButton btnStartCollecting;
	private ToggleButton btnStartLocating;
	private JTextField txtCalibrateX;
	private JTextField txtCalibrateY;
	private JTextField txtCalibrateTagID;
	private JTextField txtCalibrateBlockNumber;
	private JLabel lblNewLabel;
	private JLabel lblNewLabel_1;
	private JLabel lblNewLabel_2;
	private JLabel lblNewLabel_3;
	private JTabbedPane tabbedPaneMain;
	private JMenu mnFile;
	private JMenuItem mntmExit;
	private JTable tblLocatorConfiguration;
	private JScrollPane scrollPane;
	private JTextField txtModifyTagId;
	private JTextField txtModifyBlockId;
	private JTextField txtModifyDetectorId;
	private JTextField txtModifyRSSIIndex;
	private JTextField txtTimeout;
	private JTextField txtMinimumDetectors;
	private JTextField txtModifyNewValue;
	private JLabel lblNewValue;
	private JScrollPane scrollPane_1;
	private JTextField txtProxyIPAddress;
	private JTextField txtProxyPort;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	JRadioButton rdbtnSerial;
	JRadioButton rdbtnTcpip;
	private JTextField txtActualX;
	private JTextField txtActualY;
	private JTextField txtMinimumSamplesPerCalDet;
	private JTextField txtRSSIThreshold;
	private JTextField txtCalibrateMinDetectors;
	private JLabel lblMinimumDetectorParticipants;
	private JTextField txtListLenThreshold;
	private JLabel lblDetectorExclusionList;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main window = new Main();
					window.frmController.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static LocationEngine createLocator(LocatorChoices locatorChoice) {
		switch (locatorChoice){
		case FINGERPRINT:
			return (LocationEngine)new FingerPrint();
		case BETA:
			return(LocationEngine)new BetaEngine();
		case KEWTON:
			return(LocationEngine)new KEngine();
		}
		return null;
	}
	public static void populateConfigurationTable(LocationEngine locator, JTable tblLocatorConfiguration) {
		ArrayList<ConfigurationParam> parameters = locator.getConfiguration();
		if (parameters != null){
			ConfigurationTableModel ctm = new ConfigurationTableModel();
			ctm.setData(parameters);
			tblLocatorConfiguration.setModel(ctm);
			tblLocatorConfiguration.setFont(new Font(null,Font.PLAIN, 8));
			tblLocatorConfiguration.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);   
			tblLocatorConfiguration.getColumnModel().getColumn(0).setPreferredWidth(55);
			tblLocatorConfiguration.getColumnModel().getColumn(1).setPreferredWidth(400);					
			tblLocatorConfiguration.getColumnModel().getColumn(2).setPreferredWidth(55);
		}else{
			for (int i = 0;i < tblLocatorConfiguration.getColumnModel().getColumnCount();i++){
				tblLocatorConfiguration.getColumnModel().removeColumn(tblLocatorConfiguration.getColumnModel().getColumn(0));
			}
		}
	}
	public boolean openPort() throws NoSuchPortException,
	PortInUseException, UnsupportedCommOperationException, UnknownHostException, IOException {
		
		if (rdbtnSerial.isSelected()){
			if (cbxCOMPorts.getItemCount() < 1 || cbxCOMPorts.getSelectedIndex() == -1){
				JOptionPane.showMessageDialog(null,"A port was no selected. Please select a port.");
				return false;
			}
			String selectedPortName = cbxCOMPorts.getItemAt(cbxCOMPorts.getSelectedIndex()).toString();
			CommPortIdentifier port = CommPortIdentifier.getPortIdentifier(selectedPortName);
			commPort = port.open("Controller",2000);
			if (!(commPort instanceof SerialPort)){
				JOptionPane.showMessageDialog(null,"Only Serial Ports are supported. Please select another port.");
				return false;
			}
			serialPort = (SerialPort) commPort;
			serialPort.setSerialPortParams(19200, 
										   SerialPort.DATABITS_8, 
										   SerialPort.STOPBITS_1, 
										   SerialPort.PARITY_NONE);
		}else if (rdbtnTcpip.isSelected()){
			socketPort = new Socket();//txtProxyIPAddress.getText(), Integer.parseInt(txtProxyPort.getText()));
			socketPort.setReuseAddress(true);
			socketPort.connect(new InetSocketAddress(txtProxyIPAddress.getText(), Integer.parseInt(txtProxyPort.getText())), 1000);
			
		}
		return true;
	}
	
	/*loadCalibrationData
	 * 	deserializes an ArrayList<DataPacket> from the local filesystem
	 * 	and returns it on success. returns null if there does not exist
	 * 	previously serialized calibration data.
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<DataPacket> loadCalibrationData()
			throws ClassNotFoundException, SQLException, IOException {
		File calFile = new File("calibrationdata.dat");
		if (!calFile.exists()){
			//System.out.println("Calibration data not found.");
			printDisplayMessage("Calibration data not found\n");
			return null;
		}									
		return (ArrayList<DataPacket>)ObjectFiler.load("calibrationdata.dat");
	}
	
	@SuppressWarnings("unchecked")
	private void storeCalibrationData(int tagId, int blockId, DataPacket dataPacket)
			throws ClassNotFoundException, SQLException, IOException {
		//TODO: make cal data directory and data file configurable.	
		
		/*for (Map.Entry<Integer,ArrayList<Integer>> e: dataPacket.rssiTable.entrySet()){
			double stdev = Statistics.stdDev(e.getValue());
			System.out.println(String.format("Calibration Data: StdDev for Detector %1$d: %2$f",e.getKey(),stdev));
			int median = Statistics.median(e.getValue());
			ArrayList<Integer> values = new ArrayList<Integer>();
			if (values.size() >= 5){
				for (int i = 0;i < e.getValue().size();i++){
					int value = e.getValue().get(i);
					if (value <  (median-stdev) || value > (median+stdev)){
						//This must be an outlier; remove it.					
						System.out.println(String.format("CalibrationData: Removing outlier %1$d from Detector's %2$d set",value,e.getKey()));					
					}else{					
						values.add(value);
					}
				}			
				dataPacket.rssiTable.put(e.getKey(), values);
			}else{
				System.out.println("Not enough calibration data for detector "+e.getKey()+" to filter.");				
			}
			
		}*/
		
		
		
		ArrayList<DataPacket> calibrationData=null;			
		File calFile = new File("calibrationdata.dat");
		if (calFile.exists()){				
			calibrationData = (ArrayList<DataPacket>)ObjectFiler.load("calibrationdata.dat");
		}else{
			calibrationData = new ArrayList<DataPacket>();
		}
		
		//If it exists, replace entry with a matching block Id, else just append it.
		boolean foundExisting=false;		
		for (int h = 0;h < calibrationData.size();h++){
			if (calibrationData.get(h).blockId == blockId && calibrationData.get(h).tagId == tagId){
				calibrationData.set(h, dataPacket);
				foundExisting = true;
				break;
			}
		}
		if (!foundExisting){
			calibrationData.add(dataPacket);
		}
		//Serialize Calibration Data..
		ObjectFiler.save("calibrationdata.dat", calibrationData);
	}
	
	public void printDisplayMessage(String message){
		txtOutput.append(message);
		txtOutput.setCaretPosition(txtOutput.getDocument().getLength());
	}
	/**
	 * Create the application.
	 */
	public Main() {
		//GUI init
		initialize();

		//member variable init
		appViewMode = AppViewMode.LOCATING;
		
		//Populate combo box with available serial ports.
		@SuppressWarnings("unchecked")
		Enumeration<CommPortIdentifier> ports = CommPortIdentifier.getPortIdentifiers();
		while (ports.hasMoreElements()){
			CommPortIdentifier port = ports.nextElement();	
			cbxCOMPorts.addItem(port.getName());
		}	
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		generalDisplayMessages = new ConcurrentLinkedQueue<String>();
		
		frmController = new JFrame();
		frmController.setTitle("Controller");
		frmController.setBounds(100, 100, 971, 520);
		frmController.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmController.getContentPane().setLayout(null);
		
		tabbedPaneMain = new JTabbedPane(JTabbedPane.TOP);
		tabbedPaneMain.setBounds(10, 234, 935, 227);
		
		JPanel pnlSettings = new JPanel();		
		JPanel pnlCollect = new JPanel();
		JPanel pnlLocate = new JPanel();
		
		tabbedPaneMain.addTab("Settings",pnlSettings);
		pnlSettings.setLayout(null);
		
		cbxCOMPorts = new JComboBox();
		cbxCOMPorts.setBounds(10, 24, 265, 20);
		pnlSettings.add(cbxCOMPorts);
		
		JLabel lblSerialPort = new JLabel("Serial Port");
		lblSerialPort.setBounds(10, 11, 80, 14);
		pnlSettings.add(lblSerialPort);
		
		JLabel lblLocatorConfiguration = new JLabel("Locator Configuration");
		lblLocatorConfiguration.setBounds(10, 62, 122, 14);
		pnlSettings.add(lblLocatorConfiguration);
		
		scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 87, 741, 101);
		pnlSettings.add(scrollPane);
		
		tblLocatorConfiguration = new JTable();
		scrollPane.setViewportView(tblLocatorConfiguration);
		
		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder(null, "Locator Engine", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel.setBounds(755, 80, 165, 111);
		pnlSettings.add(panel);
		panel.setLayout(null);
		
		final JRadioButton rdbtnFingerPrintEngine = new JRadioButton("Finger Print Engine");
		rdbtnFingerPrintEngine.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				if (rdbtnFingerPrintEngine.isSelected()){
					locatorChoice = LocatorChoices.FINGERPRINT;
					locator = Main.createLocator(locatorChoice);
					Main.populateConfigurationTable(locator, tblLocatorConfiguration);
				}
			}
		});
		rdbtnFingerPrintEngine.setBounds(6, 21, 145, 23);
		panel.add(rdbtnFingerPrintEngine);
		
		final JRadioButton rdbtnGammaEngine = new JRadioButton("Beta Engine");
		rdbtnGammaEngine.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				if (rdbtnGammaEngine.isSelected()){
					locatorChoice = LocatorChoices.BETA;
					locator = Main.createLocator(locatorChoice);				
					Main.populateConfigurationTable(locator, tblLocatorConfiguration);
				}
			}
		});
		rdbtnGammaEngine.setBounds(6, 40, 145, 23);
		panel.add(rdbtnGammaEngine);
		
		final JRadioButton rdbtnKewtonEngine = new JRadioButton("Kewton Engine");
		rdbtnKewtonEngine.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				if (rdbtnKewtonEngine.isSelected()){
					locatorChoice = LocatorChoices.KEWTON;
					locator = Main.createLocator(locatorChoice);				
					Main.populateConfigurationTable(locator, tblLocatorConfiguration);
				}
			}
		});
		
		ButtonGroup engineBtnGroup = new ButtonGroup(); 
		engineBtnGroup.add(rdbtnFingerPrintEngine);
		engineBtnGroup.add(rdbtnGammaEngine);
		engineBtnGroup.add(rdbtnKewtonEngine);
		rdbtnKewtonEngine.setBounds(6, 59, 109, 23);
		panel.add(rdbtnKewtonEngine);
		
		txtProxyIPAddress = new JTextField();
		txtProxyIPAddress.setText("192.168.16.3");
		txtProxyIPAddress.setBounds(325, 24, 86, 20);
		pnlSettings.add(txtProxyIPAddress);
		txtProxyIPAddress.setColumns(10);
		
		JLabel lblProxyIpAddress = new JLabel("Proxy IP Address:");
		lblProxyIpAddress.setBounds(325, 11, 102, 14);
		pnlSettings.add(lblProxyIpAddress);
		
		txtProxyPort = new JTextField();
		txtProxyPort.setText("2000");
		txtProxyPort.setBounds(437, 24, 86, 20);
		pnlSettings.add(txtProxyPort);
		txtProxyPort.setColumns(10);
		
		JLabel lblProxyPort = new JLabel("Proxy Port:");
		lblProxyPort.setBounds(437, 11, 67, 14);
		pnlSettings.add(lblProxyPort);
		
		rdbtnSerial = new JRadioButton("Serial");
		buttonGroup.add(rdbtnSerial);
		rdbtnSerial.setBounds(166, 51, 109, 23);
		pnlSettings.add(rdbtnSerial);
		
		rdbtnTcpip = new JRadioButton("TCP/IP");
		rdbtnTcpip.setSelected(true);
		buttonGroup.add(rdbtnTcpip);
		rdbtnTcpip.setBounds(325, 51, 109, 23);
		pnlSettings.add(rdbtnTcpip);
		tabbedPaneMain.addTab("Collect", pnlCollect);
		pnlCollect.setLayout(null);
		
		JLabel lblLocationDescription = new JLabel("Output File Description");
		lblLocationDescription.setBounds(10, 11, 123, 16);
		pnlCollect.add(lblLocationDescription);
		
		txtLocationDescription = new JTextField();
		txtLocationDescription.setBounds(10, 26, 265, 20);
		pnlCollect.add(txtLocationDescription);
		txtLocationDescription.setColumns(10);
		
		lblPreviousLocationDescription = new JLabel("Previous...");
		lblPreviousLocationDescription.setBounds(11, 43, 177, 16);
		pnlCollect.add(lblPreviousLocationDescription);
		
		btnStartCollecting = new ToggleButton("Start Collecting", "Stop Collecting");
		btnStartCollecting.setBounds(10, 157, 264, 31);
		pnlCollect.add(btnStartCollecting);
		btnStartCollecting.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {					
					if (btnStartCollecting.getText() == btnStartCollecting.getFirstTitle()){
						if (txtLocationDescription.getText().length() < 2){
							JOptionPane.showMessageDialog(null,"Please specify a file description.");
							return;
						}
						openPort();
						InputStream in;// = serialPort.getInputStream();
						OutputStream out;// = serialPort.getOutputStream();
						if (rdbtnSerial.isSelected()){
							in= serialPort.getInputStream();
							out = serialPort.getOutputStream();
						}else{
							in = socketPort.getInputStream();
							out = socketPort.getOutputStream();
						}
						collectorReader = new CollectorReader(in);
						writer = new Writer(out);
						writerThread = new Thread(collectorReader);
						readerThread = new Thread(writer);
						writerThread.start();
						readerThread.start();						
						btnStartCollecting.toggleTitle();						
					}else if (btnStartCollecting.getText() == btnStartCollecting.getSecondTitle()){
						writer.stop();
						collectorReader.requestStop();
						writerThread.join();
						readerThread.join();
						if (rdbtnSerial.isSelected()){
							serialPort.close();
						}else{
							socketPort.close();
						}
						btnStartCollecting.toggleTitle();
					}
				} catch (NoSuchPortException e) {
					//Caused by CommPortIdentifier.getPortIdentifier
					e.printStackTrace();
				} catch (PortInUseException e) {
					//Caused by port.open
					e.printStackTrace();
				} catch (UnsupportedCommOperationException e) {
					//Caused by serialPort.setSerialPortParams
					e.printStackTrace();
				} catch (IOException e) {
					//Caused by serialPort.getInputStream() or serialPort.getOutputStream()
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}		
		});
		btnStartCollecting.setToolTipText("Saves collected data into a file");
		JPanel pnlCalibrate = new JPanel();
		tabbedPaneMain.addTab("Calibrate", pnlCalibrate);
		pnlCalibrate.setLayout(null);
		
		lblNewLabel = new JLabel("X");
		lblNewLabel.setBounds(10, 11, 46, 14);
		pnlCalibrate.add(lblNewLabel);
		
		lblNewLabel_1 = new JLabel("Y");
		lblNewLabel_1.setBounds(98, 11, 46, 14);
		pnlCalibrate.add(lblNewLabel_1);
		
		btnStartCalibrating = new ToggleButton("Start Calibrating", "Stop Calibrating");
		btnStartCalibrating.setBounds(10, 157, 265, 31);
		pnlCalibrate.add(btnStartCalibrating);
		btnStartCalibrating.setToolTipText("Saves collected data into the database");
		
		txtCalibrateX = new JTextField();
		txtCalibrateX.setBounds(10, 24, 86, 20);
		pnlCalibrate.add(txtCalibrateX);
		txtCalibrateX.setColumns(10);
		
		txtCalibrateY = new JTextField();
		txtCalibrateY.setBounds(98, 24, 86, 20);
		pnlCalibrate.add(txtCalibrateY);
		txtCalibrateY.setColumns(10);
		
		txtCalibrateTagID = new JTextField();
		txtCalibrateTagID.setBounds(10, 68, 265, 20);
		pnlCalibrate.add(txtCalibrateTagID);
		txtCalibrateTagID.setColumns(10);
		
		txtCalibrateBlockNumber = new JTextField();
		txtCalibrateBlockNumber.setBounds(10, 111, 265, 20);
		pnlCalibrate.add(txtCalibrateBlockNumber);
		txtCalibrateBlockNumber.setColumns(10);
		
		lblNewLabel_3 = new JLabel("Block Number");
		lblNewLabel_3.setBounds(10, 96, 102, 14);
		pnlCalibrate.add(lblNewLabel_3);
		
		lblNewLabel_2 = new JLabel("TagID");
		lblNewLabel_2.setBounds(10, 55, 46, 14);
		pnlCalibrate.add(lblNewLabel_2);
		
		txtMinimumSamplesPerCalDet = new JTextField();
		txtMinimumSamplesPerCalDet.setText("5");
		txtMinimumSamplesPerCalDet.setBounds(285, 24, 86, 20);
		pnlCalibrate.add(txtMinimumSamplesPerCalDet);
		txtMinimumSamplesPerCalDet.setColumns(10);
		
		txtRSSIThreshold = new JTextField();
		txtRSSIThreshold.setText("200");
		txtRSSIThreshold.setBounds(285, 67, 86, 20);
		pnlCalibrate.add(txtRSSIThreshold);
		txtRSSIThreshold.setColumns(10);
		
		JLabel lblMinimumSamplesPer_1 = new JLabel("Minimum Samples Per Detector");
		lblMinimumSamplesPer_1.setBounds(285, 11, 182, 14);
		pnlCalibrate.add(lblMinimumSamplesPer_1);
		
		JLabel lblRssiCutoffThreshold = new JLabel("RSSI Cutoff Threshold");
		lblRssiCutoffThreshold.setBounds(285, 52, 156, 14);
		pnlCalibrate.add(lblRssiCutoffThreshold);
		
		txtCalibrateMinDetectors = new JTextField();
		txtCalibrateMinDetectors.setText("2");
		txtCalibrateMinDetectors.setBounds(285, 111, 86, 20);
		pnlCalibrate.add(txtCalibrateMinDetectors);
		txtCalibrateMinDetectors.setColumns(10);
		
		lblMinimumDetectorParticipants = new JLabel("Minimum Detector Participants");
		lblMinimumDetectorParticipants.setBounds(285, 96, 144, 14);
		pnlCalibrate.add(lblMinimumDetectorParticipants);
		
		txtListLenThreshold = new JTextField();
		txtListLenThreshold.setText("15");
		txtListLenThreshold.setBounds(285, 157, 86, 20);
		pnlCalibrate.add(txtListLenThreshold);
		txtListLenThreshold.setColumns(10);
		
		lblDetectorExclusionList = new JLabel("List Length Threshold");
		lblDetectorExclusionList.setBounds(285, 142, 117, 14);
		pnlCalibrate.add(lblDetectorExclusionList);
		btnStartCalibrating.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					if (btnStartCalibrating.getText() == btnStartCalibrating.getFirstTitle()){						
						openPort();
						InputStream in;// = serialPort.getInputStream();
						OutputStream out;// = serialPort.getOutputStream();
						if (rdbtnSerial.isSelected()){
							in= serialPort.getInputStream();
							out = serialPort.getOutputStream();
						}else{
							in = socketPort.getInputStream();
							out = socketPort.getOutputStream();
						}
						calibratorReader = new CalibratorReader(in);						
						readerThread = new Thread(calibratorReader);
						
						
						readerThread.start();							
						btnStartCalibrating.toggleTitle();						
					//}else if (btnStartCalibrating.getText() == btnStartCalibrating.getSecondTitle()){
						readerThread.join();						
						java.awt.Toolkit.getDefaultToolkit().beep();
						if (rdbtnSerial.isSelected()){
							serialPort.close();
						}else{
							socketPort.close();
						}
						int oldBlockId = Integer.parseInt(txtCalibrateBlockNumber.getText());
						oldBlockId++;
						txtCalibrateBlockNumber.setText(String.format("%1$d",oldBlockId));
						
						btnStartCalibrating.toggleTitle();
					}
					/*if (btnStartCalibrating.getText() == btnStartCalibrating.getFirstTitle()){						
						openPort();
						InputStream in;// = serialPort.getInputStream();
						OutputStream out;// = serialPort.getOutputStream();
						if (rdbtnSerial.isSelected()){
							in= serialPort.getInputStream();
							out = serialPort.getOutputStream();
						}else{
							in = socketPort.getInputStream();
							out = socketPort.getOutputStream();
						}
						calibratorReader = new CalibratorReader(in);
						writer = new Writer(out);
						writerThread = new Thread(calibratorReader);
						readerThread = new Thread(writer);
						writerThread.start();
						readerThread.start();							
						btnStartCalibrating.toggleTitle();						
					}else if (btnStartCalibrating.getText() == btnStartCalibrating.getSecondTitle()){
						writer.stop();
						calibratorReader.requestStop();
						writerThread.join();
						readerThread.join();
						if (rdbtnSerial.isSelected()){
							serialPort.close();
						}else{
							socketPort.close();
						}
						int oldBlockId = Integer.parseInt(txtCalibrateBlockNumber.getText());
						oldBlockId++;
						txtCalibrateBlockNumber.setText(String.format("%1$d",oldBlockId));
						
						btnStartCalibrating.toggleTitle();
					}*/
				} catch (NoSuchPortException e) {
					//Caused by CommPortIdentifier.getPortIdentifier
					e.printStackTrace();
				} catch (PortInUseException e) {
					//Caused by port.open
					e.printStackTrace();
				} catch (UnsupportedCommOperationException e) {
					//Caused by serialPort.setSerialPortParams
					e.printStackTrace();
				} catch (IOException e) {
					//Caused by serialPort.getInputStream() or serialPort.getOutputStream()
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		tabbedPaneMain.addTab("Locate", pnlLocate);
		pnlLocate.setLayout(null);
		
		btnStartLocating = new ToggleButton("Start Locating", "Stop Locating");
		btnStartLocating.setBounds(10, 157, 265, 31);
		pnlLocate.add(btnStartLocating);
		btnStartLocating.setToolTipText("Passes collected data to the Locating engine");
		
		txtTimeout = new JTextField();
		txtTimeout.setText("5");
		txtTimeout.setBounds(10, 27, 86, 20);
		pnlLocate.add(txtTimeout);
		txtTimeout.setColumns(10);
		
		JLabel lblTimeout = new JLabel("Message Collection Time");
		lblTimeout.setBounds(10, 13, 160, 14);
		pnlLocate.add(lblTimeout);
		
		txtMinimumDetectors = new JTextField();
		txtMinimumDetectors.setText("4");
		txtMinimumDetectors.setBounds(10, 71, 86, 20);
		pnlLocate.add(txtMinimumDetectors);
		txtMinimumDetectors.setColumns(10);
		
		JLabel lblMinimumSamplesPer = new JLabel("Minimum Detector Participants");
		lblMinimumSamplesPer.setBounds(10, 58, 160, 14);
		pnlLocate.add(lblMinimumSamplesPer);
		
		txtActualX = new JTextField();
		txtActualX.setText("0");
		txtActualX.setBounds(249, 71, 86, 20);
		pnlLocate.add(txtActualX);
		txtActualX.setColumns(10);
		
		txtActualY = new JTextField();
		txtActualY.setText("0");
		txtActualY.setBounds(345, 71, 86, 20);
		pnlLocate.add(txtActualY);
		txtActualY.setColumns(10);
		
		JLabel lblActualX = new JLabel("Actual X");
		lblActualX.setBounds(249, 58, 46, 14);
		pnlLocate.add(lblActualX);
		
		JLabel lblActualY = new JLabel("Actual Y");
		lblActualY.setBounds(345, 58, 46, 14);
		pnlLocate.add(lblActualY);
		btnStartLocating.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {				
				try {					
					if (btnStartLocating.getText() == btnStartLocating.getFirstTitle()){						
						openPort();
						InputStream in;// = serialPort.getInputStream();
						OutputStream out;// = serialPort.getOutputStream();
						if (rdbtnSerial.isSelected()){
							in= serialPort.getInputStream();
							out = serialPort.getOutputStream();
						}else{
							in = socketPort.getInputStream();
							out = socketPort.getOutputStream();
						}
						locatorReader = new LocatorReader(in);
						writer = new Writer(out);
						writerThread = new Thread(locatorReader);
						readerThread = new Thread(writer);
						writerThread.start();
						readerThread.start();					
						btnStartLocating.toggleTitle();
						
					}else if (btnStartLocating.getText() == btnStartLocating.getSecondTitle()){
						writer.stop();
						locatorReader.requestStop();
						writerThread.join();
						readerThread.join();
						if (rdbtnSerial.isSelected()){
							serialPort.close();
						}else{
							socketPort.close();
						}
						btnStartLocating.toggleTitle();
					}
				} catch (NoSuchPortException e) {
					//Caused by CommPortIdentifier.getPortIdentifier
					e.printStackTrace();
				} catch (PortInUseException e) {
					//Caused by port.open
					e.printStackTrace();
				} catch (UnsupportedCommOperationException e) {
					//Caused by serialPort.setSerialPortParams
					e.printStackTrace();
				} catch (IOException e) {
					//Caused by serialPort.getInputStream() or serialPort.getOutputStream()
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		
		
		frmController.getContentPane().add(tabbedPaneMain);
		JPanel pnlInfo = new JPanel();
		
		tabbedPaneMain.addTab("Info", pnlInfo);
		pnlInfo.setLayout(null);
		
		JButton btnShowCalibrationData = new JButton("Show Calibration Data");
		btnShowCalibrationData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					//Deserialize calibration data
					ArrayList<DataPacket> calibrationData;
					
					calibrationData = loadCalibrationData();
						
					//Debug printing..
					//System.out.println("FingerPrintTable:");
					printDisplayMessage("Calibration Data:\n");
					for(DataPacket t: calibrationData){
						//System.out.println(String.format("TagID=%1$d, BlockNumber=%2$d",t.tagId, t.blockId));
						printDisplayMessage(String.format("TagID=%1$d, BlockNumber=%2$d(%3$f, %4$f)\n",t.tagId, t.blockId,t.location.x, t.location.y));
						for (Map.Entry<Integer, ArrayList<Integer>> e: t.rssiTable.entrySet()){						
							//System.out.print(String.format("\tDetectorID %1$d: ", e.getKey()));
							printDisplayMessage(String.format("\tDetectorID %1$d: ", e.getKey()));
							for (Integer rssi: e.getValue()){
								//System.out.print(String.format("%1$d  ", rssi));
								printDisplayMessage(String.format("%1$d  ", rssi));
							}
							//System.out.println();
							printDisplayMessage("\n");
						}
						//System.out.println();
						printDisplayMessage("\n");
					}	
				} catch (ClassNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}			
			}
		});
		btnShowCalibrationData.setBounds(10, 157, 265, 31);
		pnlInfo.add(btnShowCalibrationData);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new TitledBorder(null, "Modify Cal Data", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_1.setBounds(731, 0, 199, 199);
		pnlInfo.add(panel_1);
		panel_1.setLayout(null);
		
		txtModifyTagId = new JTextField();
		txtModifyTagId.setBounds(10, 62, 86, 20);
		panel_1.add(txtModifyTagId);
		txtModifyTagId.setColumns(10);
		
		txtModifyBlockId = new JTextField();
		txtModifyBlockId.setBounds(10, 29, 86, 20);
		panel_1.add(txtModifyBlockId);
		txtModifyBlockId.setColumns(10);
		
		txtModifyDetectorId = new JTextField();
		txtModifyDetectorId.setBounds(10, 95, 86, 20);
		panel_1.add(txtModifyDetectorId);
		txtModifyDetectorId.setColumns(10);
		
		txtModifyRSSIIndex = new JTextField();
		txtModifyRSSIIndex.setBounds(10, 128, 86, 20);
		panel_1.add(txtModifyRSSIIndex);
		txtModifyRSSIIndex.setColumns(10);
		
		JButton btnModifyCalData = new JButton("Modify");
		btnModifyCalData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					int blockId = Integer.parseInt(txtModifyBlockId.getText());
					int tagId = Integer.parseInt(txtModifyTagId.getText());
					ArrayList<DataPacket> calibrationData = loadCalibrationData();
					for (int g = 0;g < calibrationData.size();g++){
						if (calibrationData.get(g).tagId == tagId){
							if (calibrationData.get(g).blockId == blockId){								
								int detectorId =Integer.parseInt(txtModifyDetectorId.getText());
								int rssiIndex= Integer.parseInt(txtModifyRSSIIndex.getText());
								int rssiNewValue =Integer.parseInt(txtModifyNewValue.getText());
								calibrationData.get(g).rssiTable.get(detectorId).set(rssiIndex,rssiNewValue);
								storeCalibrationData(tagId, blockId, calibrationData.get(g));
								break;
							}
						}
					}
					
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		});
		btnModifyCalData.setBounds(103, 165, 86, 23);
		panel_1.add(btnModifyCalData);
		
		JLabel lblTagid = new JLabel("TagId");
		lblTagid.setBounds(10, 49, 46, 14);
		panel_1.add(lblTagid);
		
		JLabel lblBlockid = new JLabel("BlockId");
		lblBlockid.setBounds(10, 17, 46, 14);
		panel_1.add(lblBlockid);
		
		JLabel lblDetectorid = new JLabel("DetectorId");
		lblDetectorid.setBounds(10, 83, 66, 14);
		panel_1.add(lblDetectorid);
		
		JLabel lblRssiIndex = new JLabel("RSSI Index");
		lblRssiIndex.setBounds(10, 116, 66, 14);
		panel_1.add(lblRssiIndex);
		
		txtModifyNewValue = new JTextField();
		txtModifyNewValue.setBounds(103, 128, 86, 20);
		panel_1.add(txtModifyNewValue);
		txtModifyNewValue.setColumns(10);
		
		lblNewValue = new JLabel("New Value");
		lblNewValue.setBounds(103, 116, 66, 14);
		panel_1.add(lblNewValue);
		
		scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(10, 11, 935, 221);
		frmController.getContentPane().add(scrollPane_1);
		
		txtOutput = new JTextArea();
		scrollPane_1.setViewportView(txtOutput);
		/*When the Start/Stop Locating button is hit
		 *Start/Stop the LocatorThread 
		 */
		/*When the Start/Stop Calibrating button is hit
		 *Start/Stop the CalibratorThread 
		 */
		
		JMenuBar menuBar = new JMenuBar();
		frmController.setJMenuBar(menuBar);
		
		mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				//TODO: stop everything, close everything, exit application
			}
		});
		mnFile.add(mntmExit);
		
		JMenu mnview = new JMenu("View");
		menuBar.add(mnview);
		
		JMenuItem mntmClear = new JMenuItem("Clear");
		mntmClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				txtOutput.setText(null);
			}
		});
		mnview.add(mntmClear);
	}
	
	public class Writer implements Runnable{
		private OutputStream out;
		private volatile boolean done;
		public Writer(OutputStream out){
			this.out = out;			
		}
		public void stop(){
			done=true;
		}
		public void run(){
			/*try{
				this.done = false;
				while (!done){
							
				}
			}catch (IOException e){
				e.printStackTrace();
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted in writer", e);
			}*/
		}
	}
	
	/*class PortReader
	 * 	Implements basic functionality shared by all reader threads.
	 * 	It provides:
	 * 		Reading raw data and packaging it into a RawSample object.
	 * 		Displaying a RawSample object to an output txt field
	 * 		Ability to stop the reader thread.
	 * 
	 */
	public class PortReader{
		protected InputStream in;
		protected volatile boolean done;
		
		public PortReader(InputStream in){
			this.in = in;
		}
		public void requestStop(){
			done=true;
		}		
		
		protected String printRawSample(RawSample rawSample) {
			DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
			Date date = new Date();
			String time = dateFormat.format(date);			
			String displayString = String.format("%1$s, D:%2$d, S:%3$d, T:%4$d, RSSI:%5$d, MsgID: %6$d, TagBattery: %7$d, DetBattery: %8$d\n",time, rawSample.detectorId, rawSample.sourceId, rawSample.tagId, rawSample.rssi, rawSample.messageId, rawSample.tagBattery, rawSample.detectorBattery);			
			txtOutput.append(displayString);
			txtOutput.setCaretPosition(txtOutput.getDocument().getLength());
			return displayString;
		}
		protected void parseRawSample(ArrayList<Byte> list, RawSample newSample) {
			newSample.sourceId 			= list.get(0) & 0xff; 		
			newSample.detectorId 		= list.get(1) & 0xff; 		
			newSample.rssi 				= ((list.get(2) & 0xff) << 8) + (list.get(3) & 0xff);			
			newSample.tagId 			= list.get(4) & 0xff;		
			newSample.messageId 		= list.get(5) & 0xff;		
			newSample.tagBattery		= list.get(6) & 0xff;	
			newSample.detectorBattery 	= list.get(7) & 0xff;
			
			//Remove the message from the input buffer.
			for (int r = 0;r < 8;r++)
				list.remove(0);
			
			//System.out.println("Parsing Sample From " + newSample.detectorId);
		}
		protected void readPort(ArrayList<Byte> list, int bufferSize)
				throws IOException {
			byte[] buffer = new byte[bufferSize];
			int bytesRead=0;					
			if (in.available() > 0){
				while (bytesRead != '$'){
					bytesRead = in.read();
					//System.out.println("Searching for Start...");					
				}
				//System.out.println("Found Start.");
				bytesRead=0;
				while (bytesRead < bufferSize){
					bytesRead += in.read(buffer,bytesRead, bufferSize-bytesRead);
				}
				//System.out.println("Read: " + bytesRead + " bytes");
				for (int k = 0;k < bytesRead;k++){
					list.add(buffer[k]);
				}
			}
		}
	}
	
	/*class RawSample
	 * 	Represents a sample that is exactly as it comes in from the radio.
	 * 	Used internally. 
	 */
	public class RawSample{
		public int detectorId;
		public int rssi;
		public int sourceId;
		public int tagId;
		public int messageId;
		public int tagBattery;
		public int detectorBattery;
	}
	
	/*class CalibratorReader
	 * 	This class implements a thread that reads raw samples from a (Serial)Port.
	 * 	It ignores all Raw Samples that come from a tag that doesn't match the
	 *  TagId specified in the user interface for calibration mode.
	 * 	If a raw sample is collected with a matching TagId, it is stored into
	 * 	a DataPacket object.
	 * 	When this reader is requested to stop, it first serializes the DataPacket object
	 * 	to a local file.
	 */
	public class CalibratorReader extends PortReader implements Runnable {
		Hashtable<Integer, Calendar> ttl = new Hashtable<Integer, Calendar>();
		Hashtable<Integer, ArrayList<String>> samples = new Hashtable<Integer,ArrayList<String>>();
		public CalibratorReader(InputStream in){
			super(in);
		}
		public void run() {
			try{				
				this.done=false;
				ArrayList<Byte> list = new ArrayList<Byte>();				
				DataPacket dataBlock = null;
				boolean firstSample=true;
				int targetTagId = Integer.parseInt(txtCalibrateTagID.getText());
				int blockId = Integer.parseInt(txtCalibrateBlockNumber.getText());
				double x = Double.parseDouble(txtCalibrateX.getText());
				double y = Double.parseDouble(txtCalibrateY.getText());
				
				while (!done){		
					RawSample rawSample = new RawSample();
					int bufferSize = 8;
					readPort(list, bufferSize);					
					if (list.size() >= bufferSize){						
						parseRawSample(list, rawSample);						
						printRawSample(rawSample);						
						
						if (targetTagId == rawSample.tagId ){//Only calibrate for specified tag
							if (firstSample){								
								dataBlock = new DataPacket(blockId, rawSample.tagId, new Vector2D(x,y));
								firstSample = false;
							}	
							//Don't save RSSI that are below a threshold
							if (rawSample.rssi >= Integer.parseInt(txtRSSIThreshold.getText())){
								saveSample(dataBlock, rawSample);
								//Get Max RSSI List length while considering all detectors
								int maxRSSIListLen=0;
								for (Map.Entry<Integer, ArrayList<Integer>> e: dataBlock.rssiTable.entrySet()){
									if (e.getValue().size() > maxRSSIListLen){
										maxRSSIListLen = e.getValue().size(); 
									}								
								}
								
								if (maxRSSIListLen >= Integer.parseInt(txtListLenThreshold.getText())){
									//we're done here
									done = true;
								}
								/*if (oneHasMinimumSamples(dataBlock) && hasMinimumParticipatingDetectors(dataBlock)){
									//Get Max RSSI List length while considering all detectors
									int maxRSSIListLen=0;
									for (Map.Entry<Integer, ArrayList<Integer>> e: dataBlock.rssiTable.entrySet()){
										if (e.getValue().size() > maxRSSIListLen){
											maxRSSIListLen = e.getValue().size(); 
										}								
									}
									
									float listLenThreshold = Float.parseFloat(txtListLenThreshold.getText());
									
									ArrayList<Integer> keysToRemove = new ArrayList<Integer>();
									for (Map.Entry<Integer, ArrayList<Integer>> e: dataBlock.rssiTable.entrySet()){
										float ratio = e.getValue().size()/maxRSSIListLen;
										if (ratio < listLenThreshold){
											keysToRemove.add(e.getKey());
										
										}
									}
									
									for (Integer i : keysToRemove){
										dataBlock.rssiTable.remove(i);
									}
									keysToRemove.clear();
									
								}*/
								
								//Make sure all detectors have at least minimum samples
								/*if (hasMinimumSamples(dataBlock)){
									done = true;
								}*/
							}else{
								System.out.println("RSSI= " + rawSample.rssi +" is too low");
							
							}
							
						}
					}					
				}
				
				//Serialize calibration data
				if (dataBlock == null){
					//System.out.println(String.format("No calibration data for Tag %1$d", targetTagId));
					printDisplayMessage(String.format("No calibration data for Tag %1$d", targetTagId));
				}else{
					storeCalibrationData(targetTagId,blockId, dataBlock);
				}
			}catch (IOException e){
				e.printStackTrace();
			}catch (RuntimeException e){
				e.printStackTrace();			
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		public boolean hasMinimumParticipatingDetectors(DataPacket dataBlock) {			
			if (dataBlock.rssiTable.size() > Integer.parseInt(txtCalibrateMinDetectors.getText())){
				return true;
			}
			return false;
		}
		public boolean hasMinimumSamples(DataPacket dataBlock) {
			boolean allHaveAtLeastMinSamples=true;
			
			for (Map.Entry<Integer, ArrayList<Integer>> e: dataBlock.rssiTable.entrySet()){
				if (e.getValue().size() < Integer.parseInt(txtMinimumSamplesPerCalDet.getText())){
					allHaveAtLeastMinSamples = false;
				}
			}
			if (allHaveAtLeastMinSamples == true){
				//we're done
				if (dataBlock.rssiTable.keySet().size() < Integer.parseInt(txtCalibrateMinDetectors.getText())){
					txtOutput.append("NOT ENOUGH DETECTORS FOR CALIBRATION DATA");
					txtOutput.setCaretPosition(txtOutput.getDocument().getLength());
				}
			}
			return allHaveAtLeastMinSamples;
		} 
		public boolean oneHasMinimumSamples(DataPacket dataBlock) {
			boolean allHaveAtLeastMinSamples=false;
			
			for (Map.Entry<Integer, ArrayList<Integer>> e: dataBlock.rssiTable.entrySet()){
				if (e.getValue().size() >= Integer.parseInt(txtMinimumSamplesPerCalDet.getText())){
					allHaveAtLeastMinSamples = true;
				}
			}			
			return allHaveAtLeastMinSamples;
		} 
		protected void saveSample(DataPacket dataPacket, RawSample rawSample) {
			//Hash incoming data based on detector ID
			ArrayList<Integer> samples = null;
			if (dataPacket.rssiTable.containsKey(rawSample.detectorId)){
				//If samples for this detectorID have already been received
				samples = dataPacket.rssiTable.get(rawSample.detectorId);
			}else{
				//If samples for this detectorID have NOT been received before.
				samples = new ArrayList<Integer>();
			}
			samples.add(rawSample.rssi);
			dataPacket.rssiTable.put(rawSample.detectorId, samples);
		}				
	}
	
	/*class CollectorReader
	 * 	This class implements a thread that reads raw samples from a (Serial)Port.
	 * 	It writes each collected Raw Sample to a local file in CSV format.
	 * 
	 */
	public class CollectorReader extends PortReader implements Runnable{
		Hashtable<Integer, Calendar> ttl = new Hashtable<Integer, Calendar>();
		Hashtable<Integer, ArrayList<String>> samples = new Hashtable<Integer,ArrayList<String>>();
		public CollectorReader(InputStream in){
			super(in);
		}
	
		public void run(){
			try{				
				this.done=false;
				ArrayList<Byte> list = new ArrayList<Byte>();
				FileWriter fw = new FileWriter(txtLocationDescription.getText()+"_data.csv");	
				//Read the wifly connection string *HELLO*
				for (int b =0;b < 7;b++){
					System.out.print((char)in.read());
				}
				System.out.println();
				while (!done){
					int bufferSize = 8;
					readPort(list,bufferSize);
					if (list.size() >= bufferSize){												
						RawSample rawSample = new RawSample();
						parseRawSample(list, rawSample);						
						String printString = printRawSample(rawSample);		
						fw.write(printString);
					}					
				}
				fw.close();
				lblPreviousLocationDescription.setText(txtLocationDescription.getText());
				txtLocationDescription.setText(null);			
			}catch (IOException e){
				e.printStackTrace();
			}catch (RuntimeException e){
				e.printStackTrace();			
			}
		}
	}

	
	/*class LocatorReader
	 * 	This class implements a thread that reads raw samples from a (Serial)Port.
	 *  It investigates the TagId and MsgId; If it has not seen that combination,
	 *  it waits a period of time to allow other raw samples with the same TagId and MsgId
	 *  combination. When the period of time expires, it takes the Raw samples it has collected
	 *  and puts them into a DataPacket.
	 *  
	 *  1) Deserialize calibration data from local filesystem.
	 *  2) Construct a Locator implementation, and pass in calibration data.
	 *  3) Begin collecting Raw Samples
	 *  	4) If conditions are right, and a DataPacket is formed
	 *  	   invoke the locate method on the Locator instance, and
	 *  	   pass in the DataPacket.
	 *  	5) Save results of locate method into DB	
	 * 
	 */
	public class LocatorReader  extends PortReader implements Runnable{	
		private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		public LocatorReader(InputStream in){
			super(in);
		}		
		public void run(){
			try{				
				this.done=false;
				ArrayList<Byte> list = new ArrayList<Byte>();
				Hashtable<Integer, ArrayList<RawSample>> rawSampleTable = new Hashtable<Integer, ArrayList<RawSample>>();
				Hashtable<Integer, Calendar> timeout = new Hashtable<Integer, Calendar>();
				
				//Get all detector Info from DB
				Hashtable<Integer,Vector2D> detectorLocations = null;//getDetectorInfo();
				
				ArrayList<DataPacket> calibrationData = loadCalibrationData();
				
				
				
				//Create locator instance, pass in calibration data, and 
				//information about detector locations				
				LocationEngine locator = Main.createLocator(locatorChoice);
				locator.learn(calibrationData, detectorLocations);				
				Hashtable<Integer,ArrayList<DataPacket>> results = new Hashtable<Integer,ArrayList<DataPacket>>();			
				while (!done){				
					RawSample rawSample = new RawSample();
					int bufferSize = 8;
					readPort(list,bufferSize);
					if (list.size() >= bufferSize){						
						parseRawSample(list, rawSample);						
						//printSample(newSample);						
						int key = rawSample.tagId;//((rawSample.detectorId &0xff)<<8) + (rawSample.tagId & 0xff);
						//System.out.println(String.format("Saving Key: %1$d",key));
						if (rawSample.rssi >= Integer.parseInt(txtRSSIThreshold.getText())){
							saveRawSample(key, rawSampleTable, rawSample);						
							//Start a new time window, and associate with key, if key has not been seen.
							if (!timeout.containsKey(key)){
								Calendar currentMoment = Calendar.getInstance();
								currentMoment.add(Calendar.SECOND, Integer.parseInt(txtTimeout.getText()));
								timeout.put(key, currentMoment);						
							}
						}
					}	
					ArrayList<Integer> expiredItems = new ArrayList<Integer>();
					for (Map.Entry<Integer, Calendar> e: timeout.entrySet()){
						Calendar savedMoment = e.getValue();
						if (savedMoment.before(Calendar.getInstance())){
							ArrayList<RawSample> rawSamples = rawSampleTable.get(e.getKey());

							DataPacket dataPacket=null;
							boolean first=true;
							 
							for (int s = 0;s < rawSamples.size();s++){
								if (first){									
									dataPacket = new DataPacket(-1,rawSamples.get(0).tagId,null);
									first =false;
								}
								
								
								ArrayList<Integer> rawRSSI = dataPacket.rssiTable.get(rawSamples.get(s).detectorId);
								if (rawRSSI == null){
									rawRSSI = new ArrayList<Integer>();
								}
								rawRSSI.add(rawSamples.get(s).rssi);
								dataPacket.rssiTable.put(rawSamples.get(s).detectorId,rawRSSI);
							}
							
							for (Map.Entry<Integer,ArrayList<Integer>> e2: dataPacket.rssiTable.entrySet()){
								double stdev = Statistics.stdDev(e2.getValue());
								System.out.println(String.format("Location Data: StdDev for Detector %1$d: %2$f",e2.getKey(),stdev));
								int median = Statistics.median(e2.getValue());
								ArrayList<Integer> values = new ArrayList<Integer>();
								/*for (int i = 0;i < e2.getValue().size();i++){
									int value = e2.getValue().get(i);
									if (value <  (median-stdev) || value > (median+stdev)){
										//This must be an outlier; remove it.					
										System.out.println(String.format("Location Data: Removing outlier %1$d from Detector's %2$d set",value,e2.getKey()));					
									}else{					
										values.add(value);
									}
								}*/
								
								values.add(median);
								dataPacket.rssiTable.put(e2.getKey(), values);
							}
							
							///////////
							if (dataPacket.rssiTable.size() >= Integer.parseInt(txtMinimumDetectors.getText())){
							
								//String displayString = String.format("Frame prepared for Locator:\n");
								//printDisplayMessage(displayString);
								//System.out.println(String.format("TagID=%1$d, BlockNumber=%2$d",t.tagId, t.blockId));
								printDisplayMessage(String.format("\tTagID=%1$d, BlockNumber=%2$d\n",dataPacket.tagId, dataPacket.blockId));
								for (Map.Entry<Integer, ArrayList<Integer>> g: dataPacket.rssiTable.entrySet()){						
									//System.out.print(String.format("\tDetectorID %1$d: ", e.getKey()));
									printDisplayMessage(String.format("\t\tDetectorID %1$d: ", g.getKey()));
									for (Integer rssi: g.getValue()){
										//System.out.print(String.format("%1$d  ", rssi));
										printDisplayMessage(String.format("%1$d  ", rssi));
									}
									//System.out.println();
									printDisplayMessage("\n");
								}
								locator.locate(dataPacket);
								//Print to txtOutput
								String displayString = String.format("Rx'd From Locator: TagId=%1$d at (%2$f, %3$f)(%4$f), Block=%5$d\n", dataPacket.tagId, dataPacket.location.x, dataPacket.location.y, Math.sqrt(Math.pow(dataPacket.location.x - Float.parseFloat(txtActualX.getText()),2)+Math.pow(dataPacket.location.y - Float.parseFloat(txtActualY.getText()),2)),dataPacket.blockId);
								txtOutput.append(displayString);
								txtOutput.setCaretPosition(txtOutput.getDocument().getLength());
								//Expire Transaction
								expiredItems.add(e.getKey());								
								storeResult(dataPacket);	
							}else{
								printDisplayMessage(String.format("Not Enough Detectors participated in locating tag %1$d: %2$d.\n",dataPacket.tagId,dataPacket.rssiTable.keySet().size() ));
								expiredItems.add(e.getKey());
								break;
							}						
						}						
					}
					//Remove expired items.
					for (Integer key:expiredItems){
						rawSampleTable.remove(key);
						timeout.remove(key);
					}			
					
				}

			
			}catch (IOException e){
				e.printStackTrace();
			}catch (RuntimeException e){
				e.printStackTrace();
			
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public void storeResult(DataPacket dataPacket){
			try{
				Connection connect;
				Statement statement;
				//Open a connection to a database					
				// This will load the MySQL driver, each DB has its own driver
				Class.forName("com.mysql.jdbc.Driver");
				// Setup the connection with the DB
				connect = DriverManager.getConnection("jdbc:mysql://db.cecs.pdx.edu/hoangman?" + 
				 "user=hoangman&password=c@p2011$#tT");
				// Statements allow to issue SQL queries to the database
				statement = connect.createStatement();					
				//Remove rows from CalibrateBlock and BlockDate where BlockNumber == User Specified BlockNumber 
				String query1 = String.format("insert into TagInfo values(%1$d, '%2$s', %3$f, %4$f, %5$d)",
											  dataPacket.tagId, 
											  sdf.format(dataPacket.timestamp), 
											  dataPacket.location.x, 
											  dataPacket.location.y, 
											  dataPacket.battery);
				//System.out.println(query1);
				statement.executeUpdate(query1);
				statement.close();
				connect.close();
			}catch (Exception e){
				e.printStackTrace();
			}
		}
		
		public Hashtable<Integer, Vector2D> getDetectorInfo(){
			try{
				Connection connect;
				Statement statement;
				//Open a connection to a database					
				// This will load the MySQL driver, each DB has its own driver
				Class.forName("com.mysql.jdbc.Driver");
				// Setup the connection with the DB
				connect = DriverManager.getConnection("jdbc:mysql://db.cecs.pdx.edu/hoangman?" + 
				 "user=hoangman&password=c@p2011$#tT");
				// Statements allow to issue SQL queries to the database
				statement = connect.createStatement();	
				
				//Remove rows from CalibrateBlock and BlockDate where BlockNumber == User Specified BlockNumber 
				String query1 = String.format("select * from Detectors;");
				ResultSet detectorsResult = statement.executeQuery(query1);
				
				//Package results into a Hashtable<Integer, Vector2D>
				Hashtable<Integer,Vector2D> detectorTable = new Hashtable<Integer, Vector2D>();
				//System.out.println("Getting Detectors:");
				printDisplayMessage("Getting Detectors:\n");
				while (detectorsResult.next()){
					int detectorId = detectorsResult.getInt("DetectorID");
					double x = detectorsResult.getFloat("X");
					double y = detectorsResult.getFloat("Y");
					Vector2D location = new Vector2D(x,y);
					detectorTable.put(detectorId, location);
					//System.out.println(String.format("\tDetectorId=%1$d, (X,Y)=(%2$f, %3$f)\n",detectorId,x,y));
					printDisplayMessage(String.format("\tDetectorId=%1$d, (X,Y)=(%2$f, %3$f)\n",detectorId,x,y));
				}
				
				return detectorTable;
				
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			return null;			
		}

		/*saveRawSample
		 * 	This function is used to save a RawSample into a hashtable.
		 *  The key may be any integer.
		 *  This function is used to store a RawSample before it is known
		 *  which DataPacket it will be associated with. 		 * 
		 */
		protected void saveRawSample(int key, Hashtable<Integer, ArrayList<RawSample>> rawDataTable, RawSample rawSample) {
			System.out.println(String.format("Saving raw sample: D:%1$d T:%2$d  M:%3$d",rawSample.detectorId, rawSample.tagId, rawSample.messageId));
			//Hash incoming data based on key
			ArrayList<RawSample> samples = null;
			if (rawDataTable.containsKey(key)){
				//If samples for this key have already been received
				samples = rawDataTable.get(key);
			}else{
				//If samples for this key have NOT been received before.
				samples = new ArrayList<RawSample>();
			}
			samples.add(rawSample);
			rawDataTable.put(key, samples);
		}
	}	
}
