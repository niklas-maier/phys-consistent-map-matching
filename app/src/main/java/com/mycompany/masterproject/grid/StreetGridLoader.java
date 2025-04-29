package com.mycompany.masterproject.grid;

import java.io.*;
import java.util.*;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mycompany.masterproject.graph.Segment;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StreetGridLoader {

    /**
     * Static method to load a StreetGrid from a JSONL file.
     *
     * @param filePath The path to the JSONL file.
     * @return A StreetGrid object representing the loaded grid.
     * @throws IOException If an error occurs while reading the file.
     */
    public static StreetGrid loadStreetGrid(String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
    
        // Register custom deserializer for CellId
        SimpleModule module = new SimpleModule();
        module.addDeserializer(CellId.class, new CellIdDeserializer());
        mapper.registerModule(module);
    
        StreetGrid grid;
    
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
    
            // Skip description/comment lines until the grid bounds line
            while ((line = reader.readLine()) != null) {
                if (isValidJson(line)) {
                    break; // Found the grid bounds JSON line
                }
            }
    
            if (line == null) {
                throw new IOException("No grid bounds found in the file.");
            }
    
            // Parse grid bounds
            GridBounds bounds = mapper.readValue(line, GridBounds.class);
            grid = new StreetGrid(bounds);
    
            // Parse each cell data line-by-line
            while ((line = reader.readLine()) != null) {
                if (isValidJson(line)) {
                    JsonNode cellNode = mapper.readTree(line);
                    int row = cellNode.get("cell_id").get(0).asInt();
                    int col = cellNode.get("cell_id").get(1).asInt();
    
                    List<Segment> segments = new ArrayList<>();
                    for (JsonNode segmentNode : cellNode.get("segments")) {
                        int wayId = segmentNode.get("way_id").asInt();
    
                        // Parse node IDs and endpoints
                        long startNodeId = segmentNode.get("node_ids").get(0).asLong();
                        long endNodeId = segmentNode.get("node_ids").get(1).asLong();
    
                        JsonNode endpointsNode = segmentNode.get("endpoints");
                        Endpoint startEndpoint = new Endpoint(
                            startNodeId,
                            endpointsNode.get(0).get("lat").asDouble(),
                            endpointsNode.get(0).get("lon").asDouble()
                        );
                        Endpoint endEndpoint = new Endpoint(
                            endNodeId,
                            endpointsNode.get(1).get("lat").asDouble(),
                            endpointsNode.get(1).get("lon").asDouble()
                        );
    
                        // Create and add segment
                        segments.add(new Segment(wayId, List.of(startEndpoint, endEndpoint)));
                    }
    
                    GridCell cell = new GridCell(row, col, segments);
                    grid.addCell(cell);
                }
            }
        }
        return grid;
    }

    /**
     * Utility method to check if a string is a valid JSON object.
     *
     * @param line The line to check.
     * @return true if the line is valid JSON; false otherwise.
     */
    private static boolean isValidJson(String line) {
        try {
            new ObjectMapper().readTree(line);
            return true;
        } catch (IOException e) {
            return false; // Line is not valid JSON
        }
    }
}

// Custom deserializer for CellId to ensure the correct reading order of [lon, lat]
class CellIdDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<CellId> {
    @Override
    public CellId deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        int col = node.get(0).asInt();  // Interpret the first value as longitude index
        int row = node.get(1).asInt();  // Interpret the second value as latitude index
        return new CellId(row, col);
    }
}

// Data structure classes

class GridBounds {
    public double min_lon, max_lon, min_lat, max_lat, cell_size;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GridBounds)) return false;
        GridBounds that = (GridBounds) o;
        return Double.compare(that.min_lon, min_lon) == 0 &&
               Double.compare(that.max_lon, max_lon) == 0 &&
               Double.compare(that.min_lat, min_lat) == 0 &&
               Double.compare(that.max_lat, max_lat) == 0 &&
               Double.compare(that.cell_size, cell_size) == 0;
    }
}

class CellId {
    public int row, col;

    public CellId() {} // Default constructor for Jackson

    public CellId(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CellId)) return false;
        CellId cellId = (CellId) o;
        return row == cellId.row && col == cellId.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
}

class GridCell {
    private int originalRow;  // Stores the original row from input
    private int originalCol;  // Stores the original column from input
    public List<Segment> segments;

    public GridCell(int originalRow, int originalCol, List<Segment> segments) {
        this.originalRow = originalRow;
        this.originalCol = originalCol;
        this.segments = segments;
    }

    public int getOriginalRow() {
        return originalRow;
    }

    public int getOriginalCol() {
        return originalCol;
    }

    @Override
    public String toString() {
        return "GridCell(originalRow=" + originalRow + ", originalCol=" + originalCol + ", segments=" + segments + ")";
    }
}



