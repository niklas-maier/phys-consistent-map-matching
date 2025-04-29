package com.mycompany.masterproject.matching;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jxmapviewer.viewer.GeoPosition;

import com.mycompany.masterproject.data.ClosestStreetResult;
import com.mycompany.masterproject.data.GPXData;
import com.mycompany.masterproject.graph.Graph;
import com.mycompany.masterproject.graph.TimedGeoPosition;
import com.mycompany.masterproject.grid.StreetGrid;

public class MapMatcher {
    

    public static boolean mapMatch(GPXData gpxData, StreetGrid streetGrid, Graph graph, String outputFileName) {
        List<TimedGeoPosition> trackPoints = gpxData.getTrackPoints();
    
        if (trackPoints.isEmpty()) {
            return false; // If no points, return original GPXData
        }
    
        List<TimedGeoPosition> snappedPoints = new ArrayList<>();
        List<Long> fullPath = new ArrayList<>(); // Store the concatenated path
    
        // Iterate through each point and snap it to the closest street
        List<ClosestStreetResult> closestStreetResults = new ArrayList<>();

        //For Debugging
        StringBuilder logBuilder = new StringBuilder(); // Accumulate the output

        for (TimedGeoPosition currentPoint : trackPoints) {
            logBuilder.append("Processing point: ").append(currentPoint.getPosition()).append("\n");
            List<ClosestStreetResult> streets = streetGrid.findClosestStreets(currentPoint, 10);
            for (ClosestStreetResult street : streets) {
                logBuilder.append("  Found street: ").append(street.getPosition().getPosition()).append("\n");
            }
            if (streets.isEmpty()) {
                logBuilder.append("  No street found for this point.\n");
                break; // If no street found, skip this point
            }
            closestStreetResults.add(streets.get(0)); // Use the closest street result
            snappedPoints.add(streets.get(0).getPosition()); // Save the snapped point
        }

        //Write the log to a file
        // Write the accumulated log to a file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("./cleanedfiles/CLOSESTSTREETRESULTS.txt"))) {
            writer.write(logBuilder.toString());
        } catch (IOException e) {
            System.err.println("Error writing log to file: " + e.getMessage());
            return false;
        }
    
        // Process each pair of successive snapped points
        for (int i = 0; i < closestStreetResults.size() - 1; i++) {
            ClosestStreetResult start = closestStreetResults.get(i);
            ClosestStreetResult end = closestStreetResults.get(i + 1);
    
            try {
                // Run Dijkstra between the two closest streets
                Map<String, Object> result = graph.dijkstraBetweenClosestStreetResults(start, end);
    
                // Extract the path and add it to the fullPath
                @SuppressWarnings("unchecked")
                List<Long> path = (List<Long>) result.get("path");
                if (path != null) {
                    fullPath.addAll(path);
                }
    
            } catch (Exception e) {
                System.err.println("Failed to calculate path between points " + i + " and " + (i + 1) + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    
        // Convert the full path to GPX data and write it to a file
        GPXData pathData = graph.convertPathToGPXData(fullPath, "UnraveledPath");
        writeCleanedGPXToFile(pathData, outputFileName);
    
        return true;
    }
    
        

    // Method to write cleaned GPX data to a file
    private static void writeCleanedGPXToFile(GPXData cleanedData, String outputFileName) {
        try (FileWriter writer = new FileWriter(outputFileName)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<gpx version=\"1.1\" creator=\"OutlierRemover\">\n");
            writer.write("  <trk>\n");
            writer.write("    <trkseg>\n");

            for (TimedGeoPosition timedGeoPos : cleanedData.getTrackPoints()) {
                GeoPosition pos = timedGeoPos.getPosition();
                writer.write(String.format("      <trkpt lat=\"%f\" lon=\"%f\">\n", pos.getLatitude(), pos.getLongitude()));
                writer.write(String.format("        <time>%s</time>\n", 
                    java.time.Instant.ofEpochMilli(timedGeoPos.getTimestamp()).toString()));
                writer.write("      </trkpt>\n");
            }

            writer.write("    </trkseg>\n");
            writer.write("  </trk>\n");
            writer.write("</gpx>\n");

            System.out.println("Cleaned GPX track written to " + outputFileName);
        } catch (IOException e) {
            System.err.println("Error writing cleaned GPX to file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
