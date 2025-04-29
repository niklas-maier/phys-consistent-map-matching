# Geospatial Trajectory Processing and Map Matching Toolkit

---

## Overview

This repository provides a full toolkit for **processing, analyzing, matching, and visualizing geospatial trajectory data**, with a focus on OpenStreetMap-based networks and GPX files.

It consists of three main components:
- **Map Matching Core (Java)**: Physically consistent matching of GPS trajectories to a transportation network.
- **GUI for Trajectory Debugging (Java)**: Interactive exploration and inspection of GPX data.
- **Preprocessing and Auxiliary Scripts (Python)**: Preparing network and geospatial data for map matching workflows.

Each component is documented separately.

---

##  Map Matcher (Java)

The core map matching logic is implemented in Java.  
The primary entry point is the `main` method located in:

```
data/ConsistencyCheck.java
```

This module:
- Loads a trajectory file from the `Testdata/` directory (filename specified in code).
- Loads the preprocessed **street grid** and **street graph** files, generated based on OpenStreetMap data.
- Performs physically consistent map matching of trajectories onto the network.
- Outputs the best matched route into the `viableRoutes/` folder.
- Additionally generates a log file `CHECK.txt` detailing the matching steps and results.

The necessary preprocessing steps to generate the graph and grid structures are explained separately.

---

## Trajectory Debugging GUI (Java)

A Java-based graphical user interface supports:
- Drag-and-drop loading of GPX files.
- Interactive display of trajectory points, waypoints, and speed visualizations.
- Speed-based pruning of trajectories.
- Loading of preprocessed graph and grid files for manual map matching tests.
- Simple location finding and waypoint visualization tools.

**Important:** Use a **dark system theme** when launching the GUI for optimal rendering.  
More details are provided in the [GUI README](./gui/README.md).

---

##  Preprocessing and Auxiliary Scripts (Python)

The Python components provide:
- **Preprocessing tools** for preparing OSM-based graph and grid data.
- **Utility scripts** for parsing, cleaning, and formatting geospatial datasets (such as LiDAR, DGM, and GPX data).
- Functions for:
  - Generating graph structures from OpenStreetMap exports
  - Simplifying or validating network data
  - Preparing input files for the map matching engine

These scripts enable efficient preparation of the datasets required for the Java-based Map Matcher and GUI.

Further documentation is available in the [Preprocessing Tools README](./preprocessing/README.md).

---

##  Further Documentation

- [Preprocessing Tools README](./preprocessing/README.md)  
- [Trajectory Debug GUI README](./gui/README.md)

---

## 📦Requirements

- Java 21 or newer (for Map Matcher and GUI)
- Python 3.8+ (for Preprocessing Scripts)
- Recommended OS: Linux

---
