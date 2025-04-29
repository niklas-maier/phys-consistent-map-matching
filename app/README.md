# GPX Trajectory Debug & Exploration GUI

> ⚠️ **Important:** Please use a **dark system theme** when running the GUI.  
> The interface may display visual glitches or broken elements with light themes due to fixed styling assumptions.

---

##  Overview

This GUI tool is designed to **debug, inspect, and explore individual GPX trajectories** interactively.  
It supports a variety of features to load, filter, and visualize trajectory data and to assist in map matching workflows.

---

##  Features

###  Drag & Drop Trajectory Loading
- Supports loading `.gpx` files by **drag and drop** directly into the map interface.
- Multiple trajectories can be loaded simultaneously.

---

###  Trajectory List Management
- All loaded trajectories are displayed in a **scrollable list** at the bottom left of the interface.
- Each trajectory can be **hidden** (temporarily removed from the view) or **deleted** (permanently removed from the session).

--- 

###  Find Location by Coordinates
- Use the input field at the top to enter coordinates.
- Accepted formats:

  ```
  48.028570460155606, 7.827331982553005
  ```

  or

  ```xml
  <trkpt lat="48.028570460155606" lon="7.827331982553005">
  ```

- Click **“Find Location”** to place a marker on the map.
- Use **“Clear Location”** to remove all placed location markers.

---

###  Show Waypoints
- Press **“Show Waypoints”** to display all trackpoints from the loaded trajectories as markers.

---

###  Visualize Edge Speed
- Press **“Show Edge Speed”** to color the trajectory edges by speed using the **Inferno colormap**:
  - **Dark colors = slow speed**
  - **Bright colors = high speed**

---

###  Speed-Based Pruning
- You can define a **maximum speed** for each loaded trajectory.
- All GPS points exceeding this threshold will be automatically removed, and the pruned trajectory will be displayed in a distinct color on the map.

---

###  Map Matching Support
- Load your **preprocessed graph and grid files** using the **“Load Graph”** and **“Load Grid”** buttons.
- Then perform **map matching** on any selected trajectory.
- The result is a file listing, per timestamp, the **locations considered for snapping** to the road network.
- The map matcher saves its output to the cleanedfiles directory as CLOSESTSTREETSRESULT.txt

---

##  Requirements

- Java (21 or newer)
- Linux system recommended
- Dark system theme for proper GUI rendering

---

##  Use Case

This tool is intended for:
- Visual debugging of GPX data  
- Speed analysis and anomaly detection  
- Preparing and validating map matching workflows  
- Educational and research use with geospatial trajectory data

---
