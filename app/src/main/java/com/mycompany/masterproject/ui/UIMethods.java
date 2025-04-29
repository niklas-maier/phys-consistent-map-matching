package com.mycompany.masterproject.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;

import com.mycompany.masterproject.data.GPXData;
import com.mycompany.masterproject.gpx.GPXLoader;
import com.mycompany.masterproject.gpx.OutlierRemover;
import com.mycompany.masterproject.graph.Graph;
import com.mycompany.masterproject.grid.StreetGrid;
import com.mycompany.masterproject.grid.StreetGridLoader;
import com.mycompany.masterproject.matching.MapMatcher;
import com.mycompany.masterproject.util.VisualizationListener;

public class UIMethods implements VisualizationListener{
    
    private JPanel topPanel;
    private JPanel filePanel;
    private StreetGrid streetGrid;
    private JComboBox<String> trackComboBox0;
    private JComboBox<String> trackComboBox1;
    private List<String> fileNames = new ArrayList<>();
    private DrawingLogic drawingLogic;
    private Graph graph;

    public UIMethods(JPanel topPanel, DrawingLogic drawingLogic){
        this.topPanel = topPanel;
        this.drawingLogic = drawingLogic;
    }

    public JLayeredPane initializeMap(JXMapViewer mapViewer){

        JLayeredPane layeredMapPanel = new JLayeredPane();

        // gpxPanel holds the visualized GPX track
        JPanel gpxPanel = new JPanel(new BorderLayout());
        gpxPanel.setBackground(Color.LIGHT_GRAY);
        gpxPanel.add(mapViewer, BorderLayout.CENTER);

        JComponent mapComponent = gpxPanel;
        mapComponent.setBounds(0, 0, layeredMapPanel.getWidth(), layeredMapPanel.getHeight());  // Set the bounds for the map

        // Add the map to the default layer of the layered pane
        layeredMapPanel.add(mapComponent, JLayeredPane.DEFAULT_LAYER);

        // Ensure the mapPanel is resized with the window
        layeredMapPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                mapComponent.setBounds(0, 0, layeredMapPanel.getWidth(), layeredMapPanel.getHeight());  // Resize map when the mapPanel changes size
                layeredMapPanel.revalidate();  // Revalidate the layout to trigger recalculation
                layeredMapPanel.repaint();     // Repaint to ensure proper rendering
            }
        });

        return layeredMapPanel;
    }

    public void initializeSearchLocation() {
        // Create a panel for input fields and buttons
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints inputGbc = new GridBagConstraints();
        inputGbc.insets = new Insets(5, 5, 5, 5);  // Add padding around elements
        inputGbc.fill = GridBagConstraints.BOTH;  // Ensure components fill the space
    
        // Create a single input field for "lat, lon"
        JTextField locationField = new JTextField(20);
        locationField.setCaretColor(Color.WHITE);
    
        // Set placeholder text
        setPlaceholder(locationField, "Enter Latitude, Longitude");
    
        // Create buttons
        JButton findLocationButton = new JButton("Find Location");
        findLocationButton.addActionListener(e -> {
            try {
                // Get the raw input
                String input = locationField.getText().trim();
                System.out.println("Raw Input: " + input);

                double lat, lon;

                // Check if the input matches the <trkpt> format
                if (input.matches("<trkpt\\s+lat=\"[+-]?\\d+(\\.\\d+)?\"\\s+lon=\"[+-]?\\d+(\\.\\d+)?\">")) {
                    // Extract latitude and longitude from the <trkpt> format
                    String latStr = input.replaceAll(".*lat=\"([+-]?\\d+(\\.\\d+)?)\".*", "$1");
                    String lonStr = input.replaceAll(".*lon=\"([+-]?\\d+(\\.\\d+)?)\".*", "$1");

                    // Parse latitude and longitude as doubles
                    lat = Double.parseDouble(latStr);
                    lon = Double.parseDouble(lonStr);

                    System.out.println("Parsed Latitude (trkpt): " + lat);
                    System.out.println("Parsed Longitude (trkpt): " + lon);
                } else {
                    // Handle the original format
                    // Replace only decimal commas with dots
                    String normalizedInput = input.replaceAll("(?<=\\d),(?=\\d)", "."); // Replace commas between digits with dots
                    System.out.println("Normalized Input: " + normalizedInput);

                    // Split the input into latitude and longitude
                    String[] parts = normalizedInput.split("\\s*,\\s*"); // Split on separating comma
                    System.out.println("Split Parts: " + Arrays.toString(parts));

                    // Ensure exactly two parts (latitude and longitude)
                    if (parts.length != 2) {
                        throw new NumberFormatException("Invalid input format");
                    }

                    // Parse latitude and longitude as doubles
                    lat = Double.parseDouble(parts[0]);
                    lon = Double.parseDouble(parts[1]);

                    System.out.println("Parsed Latitude: " + lat);
                    System.out.println("Parsed Longitude: " + lon);
                }

                // Validate the range of latitude and longitude
                if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                    throw new NumberFormatException("Latitude or longitude out of range");
                }

                // Call the method to find the location on the map
                drawingLogic.addTempPoint(new DefaultWaypoint(new GeoPosition(lat, lon)));
            } catch (NumberFormatException ex) {
                // Display an error message for invalid input
                JOptionPane.showMessageDialog(
                    null,
                    "Invalid input. Please enter in the format 'lat, lon' or '<trkpt lat=\"...\" lon=\"...\">' with valid numbers.\n" +
                    "Examples:\n" +
                    "  - 48.028570460155606, 7.827331982553005\n" +
                    "  - <trkpt lat=\"48.028570460155606\" lon=\"7.827331982553005\">"
                );
            }
        });

    
        JButton clearLocationButton = new JButton("Clear Locations");
        clearLocationButton.addActionListener(e -> {
            // Call the method to clear the found location on the map
            drawingLogic.clearTempPoints();
        });
    
        // Configure layout for the input field (1st row)
        inputGbc.gridx = 0;
        inputGbc.gridy = 0;
        inputGbc.gridwidth = 2;  // Span across two columns
        inputPanel.add(locationField, inputGbc);
    
        // Configure layout for the buttons (2nd row)
        inputGbc.gridx = 0;
        inputGbc.gridy = 1;
        inputGbc.gridwidth = 1;  // Each button in its own column
        inputPanel.add(findLocationButton, inputGbc);
    
        inputGbc.gridx = 1;
        inputPanel.add(clearLocationButton, inputGbc);
    
        // Add the inputPanel to the topPanel
        topPanel.add(inputPanel);
    }

    //For showing or hiding the edge speeds and the waypoints
    public void initializeTrackInteractions(){

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints inputGbc = new GridBagConstraints();
        inputGbc.insets = new Insets(5, 5, 5, 5);  // Add padding around elements
        inputGbc.fill = GridBagConstraints.BOTH;  // Ensure components fill the space

        JCheckBox toggleWaypointsCheckbox = new JCheckBox("Show Waypoints");
        toggleWaypointsCheckbox.addActionListener(e -> {
            if (toggleWaypointsCheckbox.isSelected()) {
                drawingLogic.waypointsVisibilityHandler(true);  // Show waypoints
            } else {
                drawingLogic.waypointsVisibilityHandler(false);  // Hide waypoints
            }
        });

        JCheckBox toggleStreetsCheckbox = new JCheckBox("Show Edge Speed");
        toggleStreetsCheckbox.addActionListener(e -> {
            if (toggleStreetsCheckbox.isSelected()) {
                drawingLogic.showEdgeSpeed(true);
            } else {
                drawingLogic.showEdgeSpeed(false);
            }
        });

        // Configure the grid layout for the top two input fields (1st row)
        inputGbc.gridx = 0;
        inputGbc.gridy = 0;
        inputGbc.weightx = 1.0;  // Allow the checkbox to expand horizontally
        inputGbc.weighty = 1.0;  // Allow the checkbox to expand vertically
        inputPanel.add(toggleWaypointsCheckbox, inputGbc);  // Add first input field to the grid

        // Configure the grid layout for the bottom two buttons (2nd row)
        inputGbc.gridx = 0;
        inputGbc.gridy = 1;
        inputGbc.weightx = 1.0;  // Allow the checkbox to expand horizontally
        inputGbc.weighty = 1.0;  // Allow the checkbox to expand vertically
        inputPanel.add(toggleStreetsCheckbox, inputGbc);  // Add "Find Location" button to the grid

        // Add the inputPanel to the topPanel (which shares the same layout)
        topPanel.add(inputPanel);

    }

    public void initializeOutlierRemoval(){
        // Create a panel for input fields and buttons in a 2x2 grid
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints inputGbc = new GridBagConstraints();
        inputGbc.insets = new Insets(5, 5, 5, 5);  // Add padding around elements
        inputGbc.fill = GridBagConstraints.BOTH;  // Ensure components fill the space
    
        // Create input field for speed limit (latitude, longitude)
        JTextField locationField1 = new JTextField(10);  // Input field for speed limit
        locationField1.setCaretColor(Color.WHITE);
    
        // Set placeholder text
        setPlaceholder(locationField1, "Enter Max Speed");
    
        // Predefined track names (3 test strings for now)
        
        // Create JComboBox for selecting the track
        trackComboBox0 = new JComboBox<>();
        trackComboBox0.setPreferredSize(new Dimension(250, 38));  // Set size for combo box
    
        // "Remove Outliers" button
        JButton findLocationButton = new JButton("Remove Outliers");
        findLocationButton.setPreferredSize(new Dimension(150, 35));
    
        // Add ActionListener for "Remove Outliers" button
        findLocationButton.addActionListener(e -> {
            try {
                // Read the input fields for the outlier removal speed limit
                double speedLimit = Double.parseDouble(locationField1.getText().replace(',', '.'));
    
                // Get the selected track from the JComboBox
                String selectedTrack = (String) trackComboBox0.getSelectedItem();
                
                // Define the output file name
                String outputFileName = "./cleanedfiles/" + selectedTrack.replace(".gpx", "_cleaned_" + locationField1.getText().replace('.', ',') + ".gpx");
                
                // Call the method to remove the outliers from the selected track
                OutlierRemover.nk(drawingLogic, speedLimit, selectedTrack, outputFileName);
    
                // Load and visualize the cleaned track
                GPXLoader gpxLoader= new GPXLoader();
                GPXData gpxData = gpxLoader.loadGPXTrack(new File(outputFileName));
    
                if (gpxData != null) {
                    // Notify App to update the map with the cleaned GPX data
                    drawingLogic.handleGPXData(gpxData);
                }
    
            } catch (NumberFormatException ex) {
                // Handle invalid input (e.g., non-numeric text)
                JOptionPane.showMessageDialog(null, "Invalid speed limit. Please enter a valid number.");
            }
        });
    
        // Ensure the input fields and buttons have the same size
        Dimension size = new Dimension(150, 34);  // Set a preferred size
        locationField1.setPreferredSize(size);
    
        // Configure the grid layout for input fields and buttons
        inputGbc.gridx = 0;
        inputGbc.gridy = 0;
        inputPanel.add(locationField1, inputGbc);  // Add input field for speed limit
    
        inputGbc.gridx = 0;
        inputGbc.gridy = 1;
        inputPanel.add(trackComboBox0, inputGbc);  // Add JComboBox for track selection
    
        inputGbc.gridx = 0;
        inputGbc.gridy = 2;
        inputPanel.add(findLocationButton, inputGbc);  // Add "Remove Outliers" button to the grid
    
        // Add the inputPanel to the topPanel (which shares the same layout)
        topPanel.add(inputPanel);
    }
    
    public void initializeStreetDataInteractions(){

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints inputGbc = new GridBagConstraints();
        inputGbc.insets = new Insets(5, 5, 5, 5);  // Add padding around elements
        inputGbc.fill = GridBagConstraints.BOTH;  // Ensure components fill the space

        // Button 1: Load Street Grid
        JButton loadStreetGridButton = new JButton("Load Grid");
        loadStreetGridButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (file.getName().toLowerCase().endsWith(".jsonl")) { // Ensure correct file format
                    try {
                        // Load the StreetGrid using the static method
                        streetGrid = StreetGridLoader.loadStreetGrid(file.getAbsolutePath());

                        // Show a success message
                        JOptionPane.showMessageDialog(null, "Street grid loaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException f) {
                        // Show an error message if loading fails
                        JOptionPane.showMessageDialog(null, "Failed to load the street grid:\n" + f.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid file format. Please select a .jsonl file.");
                }
            }
        });

        // Button 2: Add Street Graph
        JButton loadStreetGraphButton = new JButton("Load Graph");
        loadStreetGraphButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (file.getName().toLowerCase().endsWith(".jsonl")) { // Ensure correct file format
                    try {
                        // Initialize the graph instance
                        graph = new Graph();
                        graph.readFromJsonl(file.getAbsolutePath());
                        JOptionPane.showMessageDialog(null, "Street graph loaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(null, "Failed to load the street graph:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid file format. Please select a .jsonl file.");
                }
            }
        });

        // Button 3: Clear Street Data
        JButton clearStreetButton = new JButton("Clear Street Data");
        clearStreetButton.addActionListener(e -> {
            // Clear the street grid and any associated data
            streetGrid = null;
            JOptionPane.showMessageDialog(null, "Street data cleared.", "Info", JOptionPane.INFORMATION_MESSAGE);
        });

        // Add buttons to the input panel using GridBagConstraints
        // Row 0: Load Street Grid Button
        inputGbc.gridx = 0;
        inputGbc.gridy = 0;
        inputPanel.add(loadStreetGridButton, inputGbc);

        // Row 1: Add Street Graph Button
        inputGbc.gridx = 0;
        inputGbc.gridy = 1;
        inputPanel.add(loadStreetGraphButton, inputGbc);

        // Row 2: Clear Street Data Button
        inputGbc.gridx = 0;
        inputGbc.gridy = 2;
        inputPanel.add(clearStreetButton, inputGbc);

        // Add the inputPanel to the topPanel
        topPanel.add(inputPanel);

    }


    // This function initializes the map matching button and the corresponding action listener
    public void initializeMapMatching(){

        // Create a panel for input fields and buttons in a 2x2 grid
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints inputGbc = new GridBagConstraints();
        inputGbc.insets = new Insets(5, 5, 5, 5);  // Add padding around elements
        inputGbc.fill = GridBagConstraints.BOTH;  // Ensure components fill the space
            
        // Create JComboBox for selecting the track
        trackComboBox1 = new JComboBox<>();
        trackComboBox1.setPreferredSize(new Dimension(250, 30));  // Set size for combo box
    
        // "Remove Outliers" button
        JButton mapMatchingButton = new JButton("Perform Map Matching");   
        topPanel.add(mapMatchingButton);
    
        // Add ActionListener for "Remove Outliers" button
        mapMatchingButton.addActionListener(e -> {
            try {
    
                // Get the selected track from the JComboBox
                String selectedTrack = (String) trackComboBox1.getSelectedItem();
                
                // Define the output file name
                String outputFileName = "./cleanedfiles/" + selectedTrack.replace(".gpx", "_MM_" + ".gpx");
                
                // Call the method to snap the tracks to the closest street from the selected track if the street grid is loaded
                if(streetGrid != null){
                    if(MapMatcher.mapMatch(drawingLogic.getTrack(selectedTrack), streetGrid, graph, outputFileName)){
                        System.out.println("Map Matching Successful");
                        // Load and visualize the cleaned track
                        GPXLoader gpxLoader= new GPXLoader();
                        GPXData gpxData = gpxLoader.loadGPXTrack(new File(outputFileName));
                        drawingLogic.handleGPXData(gpxData);
                    }
                    else{
                        JOptionPane.showMessageDialog(null, "There is one or more points that could not be matched to the street grid.");
                        System.out.println("Map Matching Failed");
                    }
                }
                else{
                    JOptionPane.showMessageDialog(null, "Please load the street data first.");
                }
                
    
            } catch (NumberFormatException ex) {
                // Handle invalid input (e.g., non-numeric text)
                JOptionPane.showMessageDialog(null, "Invalid speed limit. Please enter a valid number.");
            }
        });
    
    
        inputGbc.gridx = 0;
        inputGbc.gridy = 0;
        inputPanel.add(trackComboBox1, inputGbc);  // Add JComboBox for track selection
    
        inputGbc.gridx = 0;
        inputGbc.gridy = 1;
        inputPanel.add(mapMatchingButton, inputGbc);  // Add "Remove Outliers" button to the grid
    
        // Add the inputPanel to the topPanel (which shares the same layout)
        topPanel.add(inputPanel);
    }
    
    public void initializeClearCacheButton(){
        JButton clearCacheButton = new JButton("Clear Cache");
        
        
        clearCacheButton.addActionListener(e -> {
            // Call the method to clear the found location on the map
            drawingLogic.clearCache();
            streetGrid = null;
            clearFiles();

        });

        topPanel.add(clearCacheButton);

        
    }

    public JMenuBar initializeMenuBar(GPXLoader gpxLoader) {
        if (gpxLoader == null) {
            System.err.println("gpxLoader is not initialized.");
            return null;
        }

        // Create the menu bar
        JMenuBar menuBar = new JMenuBar();
    
        // Create the File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
    
        // Create and add menu items
        JMenuItem loadGpxItem = new JMenuItem("Load GPX");
        JMenuItem loadStreetData = new JMenuItem("Load Street Data");

        // Add menu items to the File menu
        fileMenu.add(loadGpxItem);  
        fileMenu.add(loadStreetData);
    
        // Add action listeners for menu items
        loadGpxItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (file.getName().toLowerCase().endsWith(".gpx")) {
                    // Load the GPX file using GPXLoader
                    GPXData gpxData = gpxLoader.loadGPXTrack(file);

                    if (gpxData != null) {
                        // Notify App to update the map
                        drawingLogic.handleGPXData(gpxData);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid file format. Please drop a .gpx file.");
                }
            }
        });
        
        loadStreetData.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                //File file = fileChooser.getSelectedFile();
                /*
                if (file.getName().toLowerCase().endsWith(".osm.pbf")) {
                    // Load the GPX file using GPXLoader
                    MapData mapData = mapLoader.loadMap(file);

                    if (mapData != null) {
                        // Notify App to update the map
                        drawingLogic.handleMapData(mapData);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid file format. Please drop a .gpx file.");
                }
                 */
            }
        });
    
        // Set the menu bar
        return(menuBar);
    }

    //Helper Functions

    public void addFileVisualizations(JLayeredPane layeredMapPanel) {
        // Create a JPanel to hold the file names in the bottom-left corner
        this.filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));  // Vertical list of files
        filePanel.setOpaque(false);  // Ensure transparency
        filePanel.setBackground(new Color(0, 0, 0, 128));  // Semi-transparent background
    
        // Wrap filePanel in a JScrollPane to make it scrollable
        JScrollPane scrollPane = new JScrollPane(filePanel);
        scrollPane.setOpaque(false);  // Keep JScrollPane transparent
        scrollPane.getViewport().setOpaque(false);  // Ensure viewport transparency
        scrollPane.setBorder(null);  // Remove the border for seamless look
    
        // Set the size and position of the scrollPane in the bottom-left corner
        int panelWidth = 350;
        int panelHeight = 150;
        scrollPane.setBounds(10, layeredMapPanel.getHeight() - panelHeight - 10, panelWidth, panelHeight);  // Bottom-left position
    
        // Add the scrollPane to the palette layer of the JLayeredPane
        layeredMapPanel.add(scrollPane, JLayeredPane.PALETTE_LAYER);
    
        // Ensure the scrollPane stays in the bottom-left when the window is resized
        layeredMapPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                scrollPane.setBounds(10, layeredMapPanel.getHeight() - panelHeight - 10, panelWidth, panelHeight);  // Resize and reposition
            }
        });
    }
    
    public void addFileLabel(String fileName) {
        // Create a new FileLabelPanel with the given file name
        filePanel.add(new FileLabelPanel(fileName), JLayeredPane.PALETTE_LAYER);
        fileNames.add(fileName);
        // Revalidate and repaint the filePanel to update the UI
        filePanel.revalidate();
        filePanel.repaint();
        updateTrackComboBox();
    }

    //Used in clear Cache to remove all the files from the filePanel
    public void clearFiles() {
        fileNames.clear();
        rebuildFilePanel(); // Refresh the panel to display nothing
    }

    private void rebuildFilePanel() {
        filePanel.removeAll(); // Clear the panel's contents
        for (String fileName : fileNames) {
            filePanel.add(new FileLabelPanel(fileName)); // Add a panel for each file
        }
        filePanel.revalidate(); // Revalidate the layout
        filePanel.repaint();   // Repaint the UI
    }

    // Method to set placeholder text that disappears when the user starts typing
    private void setPlaceholder(JTextField field, String placeholder) {
        // Set the initial text
        field.setText(placeholder);
        field.setForeground(Color.WHITE);  // Set the placeholder text color

        // Add focus listener to handle removing and restoring placeholder text
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                // When the field gains focus, clear the placeholder if it's still there
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.WHITE);  // Set the normal text color
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                // When the field loses focus, restore the placeholder if the field is empty
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(Color.WHITE);  // Set the placeholder text color
                }
            }
        });
    }

    //updates the contents of the trackComboBox according to the fileNames list
    private void updateTrackComboBox() {
        // Clear the existing items in the combo box
        trackComboBox0.removeAllItems();
        trackComboBox1.removeAllItems();
        // Add each file name from the list to the combo box
        for (String fileName : fileNames) {
            trackComboBox0.addItem(fileName);
            trackComboBox1.addItem(fileName);
        }
    
        // Optionally, revalidate and repaint the combo box if needed
        trackComboBox0.revalidate();
        trackComboBox1.revalidate();
        trackComboBox0.repaint();
        trackComboBox1.repaint();
    }

    //For displaying the GPX file interaction menu including the name of the file
    public class FileLabelPanel extends JPanel {
        @SuppressWarnings("unused")
        private String fileName;

        public FileLabelPanel(String fileName) {
            this.fileName = fileName;
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));  // 5px padding on all sides
            setLayout(new BorderLayout());
            setOpaque(false);  // Ensure transparency for rounded corners
    
            // Set preferred size to restrict the size of the panel
            setPreferredSize(new Dimension(300, 50));  // You can adjust the width and height
            setMaximumSize(new Dimension(300, 50));    // Restrict the maximum size
            setMinimumSize(new Dimension(300, 50));    // Restrict the minimum size
    
            // Create the file label with black text and transparent background
            JLabel fileLabel = new JLabel(fileName);
            fileLabel.setForeground(Color.WHITE);  // Set text color to black
            fileLabel.setOpaque(false);  // Make background transparent
            
            
            //Import images for the buttons
            ImageIcon ogEyecon = new ImageIcon(getClass().getResource("/icons/eye.png"));
            ImageIcon scaledEyecon  = new ImageIcon(ogEyecon.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));  // Resize to 20x20 pixels
            ImageIcon ogEyeconSlash = new ImageIcon(getClass().getResource("/icons/eye-slash.png"));
            ImageIcon scaledEyeSlash  = new ImageIcon(ogEyeconSlash.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));  // Resize to 20x20 pixels
            ImageIcon ogTrashIcon = new ImageIcon(getClass().getResource("/icons/trash.png"));
            ImageIcon scaledTrashIcon  = new ImageIcon(ogTrashIcon.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));  // Resize to 20x20 pixels


            // Create the buttons with transparent backgrounds
            JButton showHideButton = new JButton(scaledEyecon);
            JButton deleteButton = new JButton(scaledTrashIcon);

            showHideButton.setPreferredSize(new Dimension(30, 50));
            deleteButton.setPreferredSize(new Dimension(30, 50));

            showHideButton.setOpaque(false);
            deleteButton.setOpaque(false);
            showHideButton.setContentAreaFilled(false);  // Ensure transparency for button background
            deleteButton.setContentAreaFilled(false);  // Ensure transparency for button background

            // Set the alignment of the buttons
            showHideButton.setAlignmentY(Component.CENTER_ALIGNMENT);
            deleteButton.setAlignmentY(Component.CENTER_ALIGNMENT);
    
            // Panel to hold the buttons, aligned to the right
            JPanel buttonPanel = new JPanel();
            buttonPanel.setOpaque(false);  // Transparent to blend with rounded corners
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));  // Horizontal alignment of buttons

            // Add buttons to the button panel
            buttonPanel.add(showHideButton);
            buttonPanel.add(deleteButton);

            showHideButton.addActionListener(e -> {
                if(showHideButton.getIcon() == scaledEyecon){
                    showHideButton.setIcon(scaledEyeSlash);
                    drawingLogic.hideTrack(fileName);
                }
                else{
                    showHideButton.setIcon(scaledEyecon);
                    drawingLogic.showTrack(fileName);
                }                
            });

            deleteButton.addActionListener(e -> {
                // Call the method to clear the found location on the map
                System.out.println("Delete Button Pressed");
                Container parent = this.getParent();
                fileNames.remove(fileName);
                updateTrackComboBox();
                if (parent != null) {
                    parent.remove(this);
                    parent.revalidate();  // Revalidate to adjust the layout
                    parent.repaint();     // Repaint to refresh the UI
                }
                drawingLogic.deleteTrack(fileName);
                
            });
    
            // Add components to the FileLabelPanel
            add(fileLabel, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.EAST);
        }
    
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
            // Set the background color for the rounded rectangle
            g2d.setColor(new Color(43, 50, 53));  // Dark background
    
            // Draw rounded rectangle with a radius of 15 for rounded corners
            g2d.fillRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 15, 15);
    
            g2d.dispose();  // Clean up the graphics object
        }
    }
}
