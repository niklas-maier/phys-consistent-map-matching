package com.mycompany.masterproject.grid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.jxmapviewer.viewer.GeoPosition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.masterproject.data.ClosestStreetResult;
import com.mycompany.masterproject.graph.TimedGeoPosition;

import com.mycompany.masterproject.graph.Segment;


public class StreetGrid {
    private GridBounds bounds;
    private Map<CellId, GridCell> cells = new HashMap<>();

    public StreetGrid(GridBounds bounds) {
        this.bounds = bounds;
    }

    /**
     * Adds a GridCell to the StreetGrid using its original coordinates.
     */
    public void addCell(GridCell cell) {
        // Use original row and col values from the input JSON as the key
        CellId cellId = new CellId(cell.getOriginalRow(), cell.getOriginalCol());
        cells.put(cellId, cell);
    }
    
    private List<CellId> getNearbyCells(double lat, double lon) {
        List<CellId> cells = new ArrayList<>();
        double[] dLat = {0, bounds.cell_size, -bounds.cell_size};
        double[] dLon = {0, bounds.cell_size, -bounds.cell_size};
    
        for (double dLatOffset : dLat) {
            for (double dLonOffset : dLon) {
                double nearbyLat = lat + dLatOffset;
                double nearbyLon = lon + dLonOffset;
                cells.add(getCellId(nearbyLat, nearbyLon));
            }
        }
        return cells;
    }
        
    //returns the c closest streets to the given point. It returns the point on the street segment and the segment itself. Additionally it does not return the same wayID twice.
    public List<ClosestStreetResult> findClosestStreets(TimedGeoPosition timedGeoPosition, int c) {
        double lat = timedGeoPosition.getPosition().getLatitude();
        double lon = timedGeoPosition.getPosition().getLongitude();
        List<CellId> nearbyCells = getNearbyCells(lat, lon); // Retrieve nearby cells based on coordinates.
    
        PriorityQueue<ClosestStreetResult> closestResults = new PriorityQueue<>(
            Comparator.comparingDouble(result -> pointToMeters(
                lat, lon, 
                result.getPosition().getPosition().getLatitude(), 
                result.getPosition().getPosition().getLongitude()))
        );
    
        // Iterate over each nearby cell and find the closest street segments.
        for (CellId cellId : nearbyCells) {
            GridCell cell = cells.get(cellId); // Get the cell from the grid.
            if (cell != null) {
                for (Segment segment : cell.segments) { // Check each segment in the cell.
                    double[] closestPoint = closestPointOnSegment(lat, lon, segment); // Closest point on the segment.
    
                    // Create ClosestStreetResult
                    ClosestStreetResult result = new ClosestStreetResult(
                        new TimedGeoPosition(
                            new GeoPosition(closestPoint[0], closestPoint[1]), 
                            timedGeoPosition.getTimestamp()
                        ),
                        segment
                    );
    
                    // Add to priority queue
                    closestResults.add(result);
                }
            }
        }
    
        // Retrieve the top 'c' closest streets without duplicate wayIds
        Set<Integer> seenWayIds = new HashSet<>();
        List<ClosestStreetResult> result = new ArrayList<>();
    
        while (!closestResults.isEmpty() && result.size() < c) {
            ClosestStreetResult next = closestResults.poll();
            int wayId = next.getSegment().way_id;
    
            if (!seenWayIds.contains(wayId)) {
                seenWayIds.add(wayId);
                result.add(next);
            }
        }
    
        return result;
    }
    
    
    /**
     * Finds the closest point on the line segment defined by the Segment's endpoints to the given point (lat, lon).
     */
    private double[] closestPointOnSegment(double lat, double lon, Segment segment) {
        Endpoint start = segment.endpoints.get(0);
        Endpoint end = segment.endpoints.get(1);
    
        // Convert latitude and longitude to approximate meters for accurate distance calculation
        double[] startMeters = latLonToMeters(start.lat, start.lon);
        double[] endMeters = latLonToMeters(end.lat, end.lon);
        double[] pointMeters = latLonToMeters(lat, lon);
    
        // Vector from start to end
        double dx = endMeters[0] - startMeters[0];
        double dy = endMeters[1] - startMeters[1];
        double lengthSquared = dx * dx + dy * dy;
    
        if (lengthSquared == 0) {
            // The segment is a point
            System.out.println("Segment is a point at (" + start.lat + ", " + start.lon + ")");
            return new double[]{start.lat, start.lon};
        }
    
        // Projection factor 't' of the point onto the line
        double t = ((pointMeters[0] - startMeters[0]) * dx + (pointMeters[1] - startMeters[1]) * dy) / lengthSquared;
        t = Math.max(0, Math.min(1, t));  // Clamps to segment bounds
    
        // Closest point in meters
        double closestX = startMeters[0] + t * dx;
        double closestY = startMeters[1] + t * dy;
    
        // Convert back to lat/lon for the closest point
        double[] closestLatLon = metersToLatLon(closestX, closestY);
    
        /* This is for debugging purposes
        System.out.println("Point: (" + lat + ", " + lon + "), Closest Point on Segment: (" +
                           closestLatLon[0] + ", " + closestLatLon[1] + "), Distance in Meters: " +
                           pointToMeters(lat, lon, closestLatLon[0], closestLatLon[1]));
         */
        return closestLatLon;
    }
    
    /**
     * Converts latitude and longitude to approximate meters based on the WGS84 ellipsoid.
     */
    private double[] latLonToMeters(double lat, double lon) {
        // Convert degrees to radians
        double latRad = Math.toRadians(lat);
        double lonRad = Math.toRadians(lon);
    
        // Approximate conversion
        double radiusEarth = 6378137.0; // Earth’s radius in meters
        double x = radiusEarth * lonRad * Math.cos(latRad); // Adjust for latitude's impact on longitude
        double y = radiusEarth * latRad;
    
        return new double[]{x, y};
    }
    
    /**
     * Converts meters to latitude and longitude (approximate).
     */
        
    private double[] metersToLatLon(double xMeters, double yMeters) {
        double radiusEarth = 6378137.0;
        double lat = Math.toDegrees(yMeters / radiusEarth);
        double lon = Math.toDegrees(xMeters / (radiusEarth * Math.cos(Math.toRadians(lat))));

        return new double[]{lat, lon};
    }
    
    /**
     * Calculates distance in meters between two latitude/longitude points.
     */
    private double pointToMeters(double lat1, double lon1, double lat2, double lon2) {
        double radiusEarth = 6371000; // Earth’s radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
    
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
    
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return radiusEarth * c;
    }
    

    /**
     * Compares the current grid's structure with a JSON string representation to verify equality.
     */
    public boolean equalsToJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try (BufferedReader reader = new BufferedReader(new StringReader(json))) {
            // Parse and compare grid bounds
            GridBounds jsonBounds = mapper.readValue(reader.readLine(), GridBounds.class);
            if (!jsonBounds.equals(this.bounds)) return false;
    
            // Parse and compare each cell line-by-line
            String line;
            while ((line = reader.readLine()) != null) {
                // Parse the JSON cell
                GridCell jsonCell = mapper.readValue(line, GridCell.class);
                // Create a CellId based on the original coordinates in JSON
                CellId jsonCellId = new CellId(jsonCell.getOriginalRow(), jsonCell.getOriginalCol());
                // Retrieve the loaded cell by the original coordinates
                GridCell loadedCell = cells.get(jsonCellId);
    
                if (!jsonCell.equals(loadedCell)) return false;
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error comparing grid with JSON: " + e.getMessage());
            return false;
        }
    }

    /**
     * Helper method to calculate the CellId for a given latitude and longitude.
     * Interprets coordinates as (lon, lat) in the input.
     */
    private CellId getCellId(double lat, double lon) {
        int row = (int) ((lat - bounds.min_lat) / bounds.cell_size);
        int col = (int) ((lon - bounds.min_lon) / bounds.cell_size);
        return new CellId(row, col);
    }

}
