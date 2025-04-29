package com.mycompany.masterproject.gpx;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.*;

import com.mycompany.masterproject.data.GPXData;
import com.mycompany.masterproject.graph.TimedGeoPosition;

import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.Waypoint;

/**
 * Loads a GPX file and extracts track points and waypoints. Returns a GPXData object.
 */

public class GPXLoader {

    public GPXData loadGPXTrack(File gpxFile) {
        try {
            System.out.println("Loading GPX file: " + gpxFile.getName());
            // Set up XML parser with namespace awareness
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); // Enable namespace awareness
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(gpxFile);
    
            // Define the GPX namespace (from your file)
            String gpxNamespace = "http://www.topografix.com/GPX/1/1";
    
            // Try fetching elements using the namespace
            NodeList nodeList = doc.getElementsByTagNameNS(gpxNamespace, "trkpt");
    
            // Fallback: If no elements found with namespace, try without namespace
            if (nodeList.getLength() == 0) {
                nodeList = doc.getElementsByTagName("trkpt");
            }
    
            Set<Waypoint> waypoints = new HashSet<>();
            List<TimedGeoPosition> trackPoints = new ArrayList<>();
    
            // Iterate through each track point
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element trkpt = (Element) nodeList.item(i);
    
                // Extract latitude and longitude attributes
                double lat = Double.parseDouble(trkpt.getAttribute("lat"));
                double lon = Double.parseDouble(trkpt.getAttribute("lon"));
                GeoPosition position = new GeoPosition(lat, lon);
    
                // Extract time element
                NodeList timeNodes = trkpt.getElementsByTagNameNS(gpxNamespace, "time");
                if (timeNodes.getLength() == 0) {
                    timeNodes = trkpt.getElementsByTagName("time"); // Fallback to no namespace
                }
    
                long timestamp = 0;
                if (timeNodes.getLength() > 0) {
                    String timeString = timeNodes.item(0).getTextContent();
                    if (!timeString.endsWith("Z")) {
                        timeString += "Z"; // Assume UTC if 'Z' is missing
                    }
                    timestamp = java.time.Instant.parse(timeString).toEpochMilli();
                }
    
                // Create TimedGeoPosition and add to list
                TimedGeoPosition timedPosition = new TimedGeoPosition(position, timestamp);
                waypoints.add(new DefaultWaypoint(position));
                trackPoints.add(timedPosition);
            }
    
            return new GPXData(gpxFile.getName(), waypoints, trackPoints);
    
        } catch (Exception ex) {
            System.err.println("Error loading GPX file: " + gpxFile.getName());
            ex.printStackTrace();  // Print full stack trace for debugging
        }
        return null; // Return null if there's an error
    }
}