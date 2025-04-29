#  Geospatial Tools

This repository contains two Python modules:

1. **GPX Track Sparsifier** – Reduces the number of GPS points in `.gpx` files based on time intervals.
2. **OSM Network Processor** – Extracts and structures motorized street networks from OpenStreetMap `.osm.pbf` files.

---

##  GPX Track Sparsifier

Simplifies `.gpx` files by retaining only one point every _N_ seconds. Useful for reducing noise and file size for GPS data.

### ✅ Features

- Parses `.gpx` files (with or without XML namespaces)
- Handles elevation and timestamp metadata
- Filters track points by a time interval (e.g. every 30 seconds)
- Outputs clean, GPX 1.1-compliant files

### 📂 Output

- `sparsified_<filename>.gpx` — Cleaned GPS tracks

### ▶️ How to Run

```bash
python gpx_sparsifier.py
```

Configure input/output folders and interval inside `main()`.

### 🔧 Dependencies

No external dependencies beyond Python standard library.

---

## OSM Network Processor

Processes OpenStreetMap `.osm.pbf` data to build structured representations of street networks for spatial analysis or map matching.

### ✅ Features

- Filters motorized roads only (e.g. `motorway`, `residential`)
- Builds an adjacency list graph with distances and metadata
- Segments street geometry into grid cells
- Outputs clean, line-by-line `.jsonl` formats

### 📂 Output

- `motorized.osm.pbf` — Filtered OSM data (only motorized roads)
- `graph.jsonl` — Street network as adjacency list
- `grid.jsonl` — Road segments organized by lat/lon grid cells

### ▶️ How to Run

```bash
python osm_network_processor.py
```

Paths and parameters are set inside `main()`.

### 🔧 Dependencies

```bash
pip install osmium shapely
```

---