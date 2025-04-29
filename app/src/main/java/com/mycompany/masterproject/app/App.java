package com.mycompany.masterproject.app;
import com.mycompany.masterproject.gpx.GPXLoader;
import com.mycompany.masterproject.ui.DrawingLogic;
import com.mycompany.masterproject.ui.FileDropListener;
import com.mycompany.masterproject.ui.MapManipulator;
import com.mycompany.masterproject.ui.UIMethods;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.TileFactoryInfo;

import javax.swing.*;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;


public class App extends JFrame {

    private JXMapViewer mapViewer;
    private DrawingLogic drawingLogic;
    private MapManipulator mapManipulator;
    private GPXLoader gpxLoader; // Add a reference to the GPXLoader


    public App() {

        super("Master Project GUI");
        ensureCleanedFilesDirectoryExists();
        applyLookAndFeelandWindow();
        initializeMapViewerComponents();
        initializeUIComponents();            
    }

    private void ensureCleanedFilesDirectoryExists() {
        File cleanedFilesDir = new File("./cleanedfiles");
        if (!cleanedFilesDir.exists()) {
            boolean created = cleanedFilesDir.mkdirs(); // Create the directory
            if (!created) {
                System.err.println("Failed to create 'cleanedfiles' directory.");
            }
        }
    }

    private void applyLookAndFeelandWindow() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("GTK+".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If GTK+ is not available, fall back to default
            e.printStackTrace();
        }

        //set Windows size
        setSize(1720, 1440);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());  // Using GridBagLayout for flexible layout
    }

    //initialize MapViewer, waypoints data structure, Drawing Logic and map manipulation
    private void initializeMapViewerComponents() {
        int maximumZoom = 20;
        this.mapViewer = new JXMapViewer();
        TileFactoryInfo info = new TileFactoryInfo(1, 19, maximumZoom, 256, true, true,
            "https://tile.openstreetmap.org",
            "x", "y", "z") {
            @Override
            public String getTileUrl(int x, int y, int zoom) {
                int z = maximumZoom - zoom;
                return this.baseURL + "/" + z + "/" + x + "/" + y + ".png";
            }
        };
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        mapViewer.setTileFactory(tileFactory);
        this.mapManipulator=new MapManipulator(mapViewer);
        this.drawingLogic = new DrawingLogic(mapViewer,mapManipulator);

        drawingLogic.setWaypointsVisible(false);// Waypoints are initially hidden
        mapManipulator.centerOnPoint(new GeoPosition(47.691, 9.1866));// Set the focus to a location 300m south of the University of Konstanz with adjusted longitude
        
        // Add the file dropper to the mapViewer
        gpxLoader = new GPXLoader();
        new DropTarget(mapViewer, new FileDropListener(gpxLoader,drawingLogic));

    }

    /**
     * Creates the GBC which contains the topBar and the mapPortion
     */
    private void initializeUIComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Create and add the top panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0.05;
        gbc.fill = GridBagConstraints.BOTH;
        add(topPanel, gbc);

        // Initialize the Top Bar
        UIMethods uIMethods = new UIMethods(topPanel,drawingLogic);
        drawingLogic.setVisualizationListener(uIMethods);

        //setJMenuBar(uIMethods.initializeMenuBar(gpxLoader));
        uIMethods.initializeSearchLocation();
        uIMethods.initializeTrackInteractions();
        uIMethods.initializeOutlierRemoval();
        uIMethods.initializeStreetDataInteractions();
        uIMethods.initializeMapMatching();
        //uIMethods.initializeClearCacheButton(); newest addition broke this feature but it is not needed for the demo

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.weighty = 0.95;
        gbc.fill = GridBagConstraints.BOTH;


        // Create and add the panel containing the map the map panel is layered so that the file name visualizations can be added
        JLayeredPane LayeredMapPanel = uIMethods.initializeMap(mapViewer);
        uIMethods.addFileVisualizations(LayeredMapPanel);

        add(LayeredMapPanel, gbc);
          



        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                mapViewer.requestFocusInWindow();  // Request focus when the window is fully open
            }
        });
    }

    public static void main(String[] args) {
        App app = new App();
        app.setVisible(true);
    }
}
