package com.mycompany.masterproject.ui;

import java.util.HashSet;
import java.util.Set;
import java.awt.Color;
import java.util.HashMap;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Waypoint;
import org.jxmapviewer.viewer.WaypointPainter;

import com.mycompany.masterproject.data.GPXData;
import com.mycompany.masterproject.util.VisualizationListener;


//manages the flow of data to visualization
public class DrawingLogic {
    
    private JXMapViewer mapViewer;
    private MapManipulator mapManipulator;
    private VisualizationListener visualizationListener;

    private boolean waypointsVisible=false;
    private Set<Waypoint> waypoints;
    private Set<Waypoint> tempPoints;

    // for deleting and maintaining the tracks
    private HashMap<String,GPXData> gpxTracks = new HashMap<>();

    //Painter related
    private WaypointPainter<Waypoint> waypointPainter;
    private WaypointPainter<Waypoint> tempPointPainter;
    private RoutePainter routePainter;  // Custom painter for drawing lines
    private CompoundPainter<JXMapViewer> compoundPainter;
    private final Set<Color> usedColors = new HashSet<>(); // Track used colors to ensure uniqueness

    public DrawingLogic(JXMapViewer mapViewer, MapManipulator mapManipulator){
        
        this.mapViewer = mapViewer;
        this.mapManipulator = mapManipulator;
        
        this.waypoints = new HashSet<>();
        this.waypointPainter = new WaypointPainter<>();
        this.tempPoints = new HashSet<>();
        this.tempPointPainter = new WaypointPainter<>();
        this.routePainter = new RoutePainter();
        this.compoundPainter= new CompoundPainter<JXMapViewer>();
    }

    //To influence the visualization listener from this class
    public void setVisualizationListener(VisualizationListener listener) {
        this.visualizationListener = listener;
    }

    //To set the visibility of the waypoints from the GUI
    public void setWaypointsVisible(boolean bool){
        this.waypointsVisible = bool;
    }

    //Need by OutlierRemover
    public boolean getWaypointsVisible(){
        return waypointsVisible;
    }

    //To add TempoPoints to the map
    @SuppressWarnings("unchecked")
    public void addTempPoint(Waypoint waypoint){
        tempPoints.add(waypoint);
        tempPointPainter.setWaypoints(tempPoints);

        if(waypointsVisible){
            compoundPainter = new CompoundPainter<>(routePainter, waypointPainter, tempPointPainter);
        }
        else{
            compoundPainter = new CompoundPainter<>(routePainter, tempPointPainter);
        }
        
        
        mapViewer.setOverlayPainter(compoundPainter);
        mapViewer.repaint();

        // Center the map on the first waypoint if available
        if (!tempPoints.isEmpty()) {
            mapManipulator.centerOnPoint(waypoint.getPosition());
            mapViewer.setZoom(1);
        }
    }
    //To remove the TemPoints via the GUI
    public void clearTempPoints(){
        tempPoints.clear();
        tempPointPainter.setWaypoints(tempPoints);
        mapViewer.repaint();
    }

    @SuppressWarnings("unchecked")
    public void waypointsVisibilityHandler(boolean showWaypoints) {
        waypointsVisible=showWaypoints;
        CompoundPainter<JXMapViewer> compoundPainter;

        if (showWaypoints) {
            // Display both waypoints and routes
            compoundPainter = new CompoundPainter<>(routePainter, waypointPainter, tempPointPainter);
        } else {
            // Display only the route
            compoundPainter = new CompoundPainter<>(routePainter);
        }

        mapViewer.setOverlayPainter(compoundPainter);
        mapViewer.repaint(); // Repaint the map to update visibility
    }

    //Method to show or hide the speed of the edges
    public void showEdgeSpeed(boolean bool){
        routePainter.setUseIndividualTrackColors(!bool); //We invert due to the way the method is implemented
        mapViewer.repaint();
    }

    // Method to load the GPX file and update the map
    @SuppressWarnings("unchecked")
    public void handleGPXData(GPXData gpxData) {

        gpxData.setTrackColor(generateUniqueColor());

        // Add the filelabel
        if (visualizationListener != null) {
            visualizationListener.addFileLabel((gpxData.getName()));
        }

        gpxTracks.put(gpxData.getName(),gpxData);
        waypoints.addAll(gpxData.getWaypoints());

        waypointPainter.setWaypoints(waypoints);

        // Update route painter with new track points
        routePainter.addTrack(gpxData.getName(),gpxData.getTrackPoints(),gpxData.getColors(),gpxData.getTrackColor());

        // Set both waypoint and route painters
        
        if (waypointsVisible) {
            compoundPainter = new CompoundPainter<>(routePainter, waypointPainter, tempPointPainter);
        } else {
            compoundPainter = new CompoundPainter<JXMapViewer>(routePainter); // Only show route
        }

        mapViewer.setOverlayPainter(compoundPainter);

        // Center the map on the first waypoint if available
        if (!gpxData.getTrackPoints().isEmpty()) {
        // Extract the GeoPosition from the first TimedGeoPosition
        GeoPosition firstGeoPosition = gpxData.getTrackPoints().get(0).getPosition();

        // Center on the extracted GeoPosition
        mapManipulator.centerOnPoint(firstGeoPosition);
        }
    }

    // Generate a unique color for each track
    public Color generateUniqueColor() {
        Color color;
        do {
            float hue = (float) Math.random(); // Random hue for color diversity
            color = Color.getHSBColor(hue, 0.5f, 0.95f);
        } while (usedColors.contains(color));
        usedColors.add(color);
        return color;
    }

    // Method to delete all files and repaint the map cleanly
    public void clearCache(){
        gpxTracks.clear();
        compoundPainter = new CompoundPainter<>();
        mapViewer.setOverlayPainter(compoundPainter);
        mapViewer.repaint();
    }
    
    @SuppressWarnings("unchecked")
    // Method to hide a single track in the visualization
    public void hideTrack(String fileName) {
        GPXData gpxData = gpxTracks.get(fileName);
        waypoints.removeAll(gpxData.getWaypoints());
        waypointPainter.setWaypoints(waypoints);
        routePainter.removeTrack(fileName);
    
        if (waypointsVisible) {
            compoundPainter = new CompoundPainter<>(routePainter, waypointPainter);
        } else {
            compoundPainter = new CompoundPainter<>(routePainter);
        }
    
        mapViewer.setOverlayPainter(compoundPainter);
        mapViewer.repaint();
    }

    @SuppressWarnings("unchecked")
    public void showTrack(String fileName) {
        GPXData gpxData = gpxTracks.get(fileName);
        waypoints.addAll(gpxData.getWaypoints());
        waypointPainter.setWaypoints(waypoints);
    
        // Add the track with its previously stored color
        routePainter.addTrack(gpxData.getName(), gpxData.getTrackPoints(), gpxData.getColors(), gpxData.getTrackColor());
    
        if (waypointsVisible) {
            compoundPainter = new CompoundPainter<>(routePainter, waypointPainter);
        } else {
            compoundPainter = new CompoundPainter<>(routePainter);
        }
    
        mapViewer.setOverlayPainter(compoundPainter);
        mapViewer.repaint();
    }
    
    // Remove a track from the data structure forever
    @SuppressWarnings("unchecked")
    public void deleteTrack(String fileName){
        GPXData gpxData = gpxTracks.get(fileName);
        waypoints.removeAll(gpxData.getWaypoints());
        waypointPainter.setWaypoints(waypoints);
        routePainter.removeTrack(fileName);
        gpxTracks.remove(gpxData.getName());

        if(waypointsVisible){
            compoundPainter = new CompoundPainter<>(routePainter, waypointPainter);
        }
        else{
            compoundPainter = new CompoundPainter<>(routePainter);
        }
        usedColors.remove(gpxData.getTrackColor());
        mapViewer.setOverlayPainter(compoundPainter);

        
        mapViewer.repaint();
    }

    public GPXData getTrack(String fileName){
        System.out.println("getTrack: " + fileName);
        return gpxTracks.get(fileName);
    }

}