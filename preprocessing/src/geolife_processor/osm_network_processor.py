import osmium
from math import radians, sin, cos, sqrt, atan2
import json
import time
from shapely.geometry import LineString, box
from collections import defaultdict
import json
import time

MOTORIZED_HIGHWAY_TYPES = {'motorway', 'motorway_link', 'trunk', 'trunk_link', 'primary', 'primary_link', 'secondary', 'tertiary', 'residential', 'unclassified'}

class HighwayFilterHandler(osmium.SimpleHandler):
    """
    Filters OSM ways and nodes based on specified highway types.

    Two modes of operation:
    - Collect referenced node IDs from ways (`collect_references=True`).
    - Write nodes and ways (`collect_references=False`).
    """
    def __init__(self, motorized_file, discarded_file=None, collect_references=False, referenced_nodes=None):
        super().__init__()
        self.collect_references = collect_references  # Mode for collecting references
        self.referenced_nodes = set() if referenced_nodes is None else referenced_nodes
        if not collect_references:
            self.motorized_writer = osmium.SimpleWriter(motorized_file, overwrite=True)
            self.discarded_writer = osmium.SimpleWriter(discarded_file, overwrite=True) if discarded_file else None
            self.write_discarded = discarded_file is not None

    def way(self, w):
        highway_type = w.tags.get('highway')
        if highway_type in MOTORIZED_HIGHWAY_TYPES:
            # Collect referenced node IDs in collect mode
            if self.collect_references:
                self.referenced_nodes.update(n.ref for n in w.nodes)
            else:
                # Write the way to the motorized file
                self.motorized_writer.add_way(w)
                self.referenced_nodes.update(n.ref for n in w.nodes)
        elif not self.collect_references and self.write_discarded:
            # Write the way to the discarded file
            self.discarded_writer.add_way(w)

    def node(self, n):
        if self.collect_references:
            # Do nothing during reference collection
            return

        if n.id in self.referenced_nodes:
            # Write nodes referenced by motorized ways
            self.motorized_writer.add_node(n)
        elif self.write_discarded:
            # Write other nodes to the discarded file if enabled
            self.discarded_writer.add_node(n)

    def close(self):
        if not self.collect_references:
            # Finalize output files
            self.motorized_writer.close()
            if self.discarded_writer:
                self.discarded_writer.close()



# Function to filter the .osm.pbf file
def filter_highways(input_file, motorized_file, discarded_file=None):
    """
    Filters the input .osm.pbf file into motorized and optionally discarded files, ensuring only
    referenced nodes are included in the output.
    """
    print(f"[INFO] Filtering highways from {input_file}...")

    # Pass 1: Collect referenced node IDs
    print("[INFO] Collecting referenced nodes...")
    reference_handler = HighwayFilterHandler(motorized_file, discarded_file, collect_references=True)
    reference_handler.apply_file(input_file, locations=True)
    referenced_nodes = reference_handler.referenced_nodes
    reference_handler.close()

    # Pass 2: Write nodes and ways
    print("[INFO] Writing nodes and ways...")
    write_handler = HighwayFilterHandler(
        motorized_file, discarded_file, referenced_nodes=referenced_nodes
    )
    write_handler.apply_file(input_file, locations=True)
    write_handler.close()

    print(f"[INFO] Filtering complete.")



######################Verify the integrity of the split with the classes below##############################

class OSMCheckerHandler(osmium.SimpleHandler):
    """
    Collects nodes and ways from an OSM file for verification.
    """
    def __init__(self):
        super().__init__()
        self.nodes = {}  # {node_id: (lon, lat)}
        self.ways = {}   # {way_id: [node_refs]}

    def node(self, n):
        if n.location.valid():
            self.nodes[n.id] = (n.location.lon, n.location.lat)

    def way(self, w):
        self.ways[w.id] = [n.ref for n in w.nodes]


def read_osm_data(file_path):
    """
    Reads nodes and ways from an OSM file and returns them as dictionaries.
    """
    handler = OSMCheckerHandler()
    handler.apply_file(file_path, locations=True)
    return handler.nodes, handler.ways


def verify_split_files(original_file, motorized_file, discarded_file):
    """
    Verifies that the union of the motorized and discarded files equals the original file.
    """
    print(f"[INFO] Reading original file: {original_file}")
    original_nodes, original_ways = read_osm_data(original_file)

    print(f"[INFO] Reading motorized file: {motorized_file}")
    motorized_nodes, motorized_ways = read_osm_data(motorized_file)

    print(f"[INFO] Reading discarded file: {discarded_file}")
    discarded_nodes, discarded_ways = read_osm_data(discarded_file)

    # Combine motorized and discarded data
    combined_nodes = {**motorized_nodes, **discarded_nodes}
    combined_ways = {**motorized_ways, **discarded_ways}

    # Verify nodes
    if original_nodes != combined_nodes:
        print("[ERROR] Nodes do not match!")
        print(f"Original: {len(original_nodes)}")
        print(f"Combined: {len(combined_nodes)}")
        return False

    # Verify ways
    if original_ways != combined_ways:
        print("[ERROR] Ways do not match!")
        print(f"Original: {len(original_ways)}")
        print(f"Combined: {len(combined_ways)}")
        return False

    print("[INFO] Verification successful! The split files match the original file.")
    return True

##########################Extract Streetnetwork as Adjaceny List###########################################


class GraphBuilderHandler(osmium.SimpleHandler):
    """Builds the graph as an adjacency list."""
    def __init__(self):
        super().__init__()
        self.adjacency_list = defaultdict(lambda: {"lat": None, "lon": None, "neighbors": {}})

    def node(self, n):
        if n.location.valid():
            self.adjacency_list[n.id]["lat"] = n.location.lat
            self.adjacency_list[n.id]["lon"] = n.location.lon

    def way(self, w):
        if "highway" in w.tags and w.tags["highway"] in MOTORIZED_HIGHWAY_TYPES:
            maxspeed = w.tags.get("maxspeed", "Unknown")
            street_type = w.tags["highway"]
            nodes = [n.ref for n in w.nodes]

            for i in range(len(nodes) - 1):
                node1 = nodes[i]
                node2 = nodes[i + 1]

                # Add node2 as a neighbor of node1
                self.adjacency_list[node1]["neighbors"][node2] = {
                    "distance": self.haversine_distance(
                        self.adjacency_list[node1]["lat"],
                        self.adjacency_list[node1]["lon"],
                        self.adjacency_list[node2]["lat"],
                        self.adjacency_list[node2]["lon"],
                    ),
                    "street_type": street_type,
                    "maxspeed": maxspeed,
                    "way_id": w.id,
                }

                # Add node1 as a neighbor of node2 (undirected graph)
                self.adjacency_list[node2]["neighbors"][node1] = {
                    "distance": self.haversine_distance(
                        self.adjacency_list[node2]["lat"],
                        self.adjacency_list[node2]["lon"],
                        self.adjacency_list[node1]["lat"],
                        self.adjacency_list[node1]["lon"],
                    ),
                    "street_type": street_type,
                    "maxspeed": maxspeed,
                    "way_id": w.id,
                }


    @staticmethod
    def haversine_distance(lat1, lon1, lat2, lon2):
        R = 6371000
        lat1, lon1, lat2, lon2 = map(radians, [lat1, lon1, lat2, lon2])
        dlat = lat2 - lat1
        dlon = lon2 - lon1
        a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
        c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c

    def _add_edge(self, node1, node2, distance, highway, maxspeed, way_id):
        if node1 not in self.adjacency_list:
            self.adjacency_list[node1] = {"neighbors": {}}
        self.adjacency_list[node1]["neighbors"][node2] = {
            "distance": round(distance, 2),
            "street_type": highway,
            "maxspeed": maxspeed,
            "way_id": way_id
        }

def export_adjacency_list_to_jsonl(adjacency_list, output_file):
    """
    Exports the adjacency list to a .jsonl file with node coordinates.

    Args:
        adjacency_list (dict): The adjacency list data structure.
        output_file (str): Path to the .jsonl output file.
    """
    print(f"[INFO] Exporting adjacency list to {output_file}...")
    print(f"[INFO] Total nodes in adjacency list: {len(adjacency_list)}")

    try:
        with open(output_file, 'w') as f:
            # Add format description at the top
            f.write("# Example Output Format:\n")
            f.write("# {\n")
            f.write("#     \"node_id\": <int>,\n")
            f.write("#     \"lat\": <float>,\n")
            f.write("#     \"lon\": <float>,\n")
            f.write("#     \"neighbors\": {\n")
            f.write("#         <neighbor_node_id:int>: {\n")
            f.write("#             \"distance\": <float>,\n")
            f.write("#             \"street_type\": <str>,\n")
            f.write("#             \"maxspeed\": <str>,\n")
            f.write("#             \"way_id\": <int>\n")
            f.write("#         }\n")
            f.write("#     }\n")
            f.write("# }\n")
            f.write("# Note: Each following line contains a single JSON record.\n")

            # Write adjacency list data
            for node_id, data in adjacency_list.items():
                node_data = {
                    "node_id": node_id,
                    "lat": data["lat"],
                    "lon": data["lon"],
                    "neighbors": data["neighbors"],
                }
                f.write(json.dumps(node_data) + '\n')

        print(f"[INFO] Successfully exported adjacency list to {output_file}")
    except Exception as e:
        print(f"[ERROR] Failed to write to file: {e}")

def process_osm_to_Streetjsonl(input_pbf, output_jsonl):
    """
    Process an .osm.pbf file into a .jsonl adjacency list file.

    Args:
        input_pbf (str): Path to the input OSM PBF file.
        output_jsonl (str): Path to the output JSONL file.
    """
    print(f"[INFO] Processing {input_pbf}...")
    
    # Step 1: Build the graph
    handler = GraphBuilderHandler()
    handler.apply_file(input_pbf, locations=True)
    
    # Step 2: Export the adjacency list to JSONL
    export_adjacency_list_to_jsonl(handler.adjacency_list, output_jsonl)
    
    print(f"[INFO] Finished processing. Output saved to {output_jsonl}")


############################Generate the grid for spatial serach############################################
def calculate_data_bounds(file_path):
    """Calculate min and max latitude and longitude by scanning the input data."""
    min_lon, min_lat = float('inf'), float('inf')
    max_lon, max_lat = float('-inf'), float('-inf')
    
    with open(file_path, 'r') as f:
        next(f)  # Skip header line
        for line in f:
            parts = line.strip().split()
            lat1, lon1 = float(parts[1]), float(parts[2])
            lat2, lon2 = float(parts[4]), float(parts[5])
            
            min_lon = min(min_lon, lon1, lon2)
            min_lat = min(min_lat, lat1, lat2)
            max_lon = max(max_lon, lon1, lon2)
            max_lat = max(max_lat, lat1, lat2)
    
    print(f"[INFO] Calculated data bounds: min_lat={min_lat}, max_lat={max_lat}, min_lon={min_lon}, max_lon={max_lon}")
    return min_lat, max_lat, min_lon, max_lon

class NodeCollectorHandler(osmium.SimpleHandler):
    """Collects node locations with valid latitude and longitude."""
    def __init__(self):
        super().__init__()
        self.node_locations = {}  # {node_id: (lon, lat)}

    def node(self, n):
        if n.location.valid():
            self.node_locations[n.id] = (n.location.lon, n.location.lat)


class WayProcessorHandler(osmium.SimpleHandler):
    """Processes ways and resolves them into edges."""
    def __init__(self, node_locations):
        super().__init__()
        self.node_locations = node_locations
        self.edges = []  # Stores edge data

    def way(self, w):
        # Assume all ways are motorized streets (no filtering needed)
        node_refs = [n.ref for n in w.nodes]

        for i in range(len(node_refs) - 1):
            node1_id = node_refs[i]
            node2_id = node_refs[i + 1]
            
            if node1_id in self.node_locations and node2_id in self.node_locations:
                lon1, lat1 = self.node_locations[node1_id]
                lon2, lat2 = self.node_locations[node2_id]
                edge = {
                    "way_id": w.id,
                    "node1_id": node1_id,
                    "node2_id": node2_id,
                    "lat1": lat1,
                    "lon1": lon1,
                    "lat2": lat2,
                    "lon2": lon2
                }
                self.edges.append(edge)


def save_populated_grid_to_jsonl(populated_grid, output_file, min_lat, max_lat, min_lon, max_lon, cell_size):
    """Save grid metadata and populated cells to JSONL format with status update."""
    with open(output_file, 'w') as f:
        grid_metadata = {
            "min_lat": min_lat,
            "max_lat": max_lat,
            "min_lon": min_lon,
            "max_lon": max_lon,
            "cell_size": cell_size
        }
        f.write(json.dumps(grid_metadata) + '\n')
        
        for cell_num, (cell, segments) in enumerate(populated_grid.items(), start=1):
            cell_data = {"cell_id": cell, "segments": segments}
            f.write(json.dumps(cell_data) + '\n')
            if cell_num % 1000 == 0:
                print(f"[INFO] Written {cell_num} populated cells to file...")
    
    print(f"[INFO] Populated grid saved to {output_file} in JSONL format with metadata.")


def process_osm_to_Gridjsonl(input_pbf, output_jsonl, cell_size=0.0005):
    """
    Process an .osm.pbf file into a grid-based .jsonl file.

    Args:
        input_pbf (str): Path to the input OSM PBF file.
        output_jsonl (str): Path to the output JSONL file.
        cell_size (float): Size of the grid cells for spatial segmentation (default: 0.0005).
    """
    print(f"[INFO] Processing {input_pbf}...")
    
    # Step 1: Collect all nodes
    print("[INFO] Collecting nodes...")
    node_handler = NodeCollectorHandler()
    node_handler.apply_file(input_pbf, locations=True)

    # Step 2: Process ways into edges
    print("[INFO] Processing ways...")
    way_handler = WayProcessorHandler(node_handler.node_locations)
    way_handler.apply_file(input_pbf)

    # Step 3: Retrieve edges from way handler
    edges = way_handler.edges

    # Step 4: Calculate data bounds
    print("[INFO] Calculating data bounds...")
    min_lat = min(edge['lat1'] for edge in edges)
    max_lat = max(edge['lat1'] for edge in edges)
    min_lon = min(edge['lon1'] for edge in edges)
    max_lon = max(edge['lon1'] for edge in edges)

    # Step 5: Map edges to grid cells
    print("[INFO] Mapping edges to grid...")
    populated_grid = map_edges_to_populated_grid_with_node_ids(edges, cell_size, min_lat, min_lon)

    # Step 6: Save the grid as JSONL with a format explainer
    print("[INFO] Saving to JSONL...")
    with open(output_jsonl, 'w') as f:
        # Add format explainer at the top
        f.write("# Example Output Format:\n")
        f.write("# {\n")
        f.write("#     \"cell_id\": [<int>, <int>],\n")
        f.write("#     \"segments\": [\n")
        f.write("#         {\n")
        f.write("#             \"way_id\": <int>,\n")
        f.write("#             \"node_ids\": [<int>, <int>],\n")
        f.write("#             \"endpoints\": [\n")
        f.write("#                 {\"lat\": <float>, \"lon\": <float>},\n")
        f.write("#                 {\"lat\": <float>, \"lon\": <float>}\n")
        f.write("#             ]\n")
        f.write("#         }\n")
        f.write("#     ]\n")
        f.write("# }\n")
        f.write("# Note: Each following line contains a single JSON record.\n")

        # Write grid metadata as the first JSONL line
        grid_metadata = {
            "min_lat": min_lat,
            "max_lat": max_lat,
            "min_lon": min_lon,
            "max_lon": max_lon,
            "cell_size": cell_size
        }
        f.write(json.dumps(grid_metadata) + '\n')

        # Write each populated grid cell
        for cell_num, (cell, segments) in enumerate(populated_grid.items(), start=1):
            cell_data = {"cell_id": cell, "segments": segments}
            f.write(json.dumps(cell_data) + '\n')
            if cell_num % 1000 == 0:
                print(f"[INFO] Written {cell_num} populated cells to file...")
    
    print(f"[INFO] Finished processing. Output saved to {output_jsonl}")


def map_edges_to_populated_grid_with_node_ids(edges, cell_size, min_lat, min_lon):
    """Map edges to grid cells, including original node IDs."""
    populated_grid = defaultdict(list)
    
    for i, edge in enumerate(edges, start=1):
        segments = split_line_at_grid_with_node_ids(edge, cell_size, min_lat, min_lon)
        for (cell_y, cell_x), segment in segments:
            populated_grid[(cell_y, cell_x)].append(segment)
        
        if i % 1000 == 0:
            print(f"[INFO] Processed {i} edges...")

    print(f"[INFO] Finished mapping all edges to grid cells.")
    return populated_grid


def split_line_at_grid_with_node_ids(line, cell_size, min_lat, min_lon):
    """Splits a line at grid cell boundaries and returns segments with grid cell references and node IDs."""
    segments = []
    line_geom = LineString([(line['lon1'], line['lat1']), (line['lon2'], line['lat2'])])
    
    min_y = int((line['lat1'] - min_lat) / cell_size)
    min_x = int((line['lon1'] - min_lon) / cell_size)
    max_y = int((line['lat2'] - min_lat) / cell_size)
    max_x = int((line['lon2'] - min_lon) / cell_size)

    for y in range(min(min_y, max_y), max(min_y, max_y) + 1):
        for x in range(min(min_x, max_x), max(min_x, max_x) + 1):
            cell_bounds = box(
                min_lon + x * cell_size, min_lat + y * cell_size,
                min_lon + (x + 1) * cell_size, min_lat + (y + 1) * cell_size
            )
            intersection = line_geom.intersection(cell_bounds)
            if intersection.is_empty:
                continue
            if intersection.geom_type == 'LineString':
                segment = {
                    "way_id": line['way_id'],
                    "node_ids": [line['node1_id'], line['node2_id']],
                    "endpoints": [{"lat": coord[1], "lon": coord[0]} for coord in intersection.coords]
                }
                segments.append(((y, x), segment))
    
    return segments


#########################################################################################
import time

def main():
    # Input and output file paths
    input_pbf = '/media/niklas/SSD2/geodata/freiburg-regbez-latest.osm.pbf'  # Replace with your input file
    motorized_pbf = '/media/niklas/SSD2/geodata/moto.osm.pbf'  # Output file for motorized data
    street_jsonl = '/media/niklas/SSD2/geodata/graph.jsonl' 
    grid_jsonl = '/media/niklas/SSD2/geodata/grid.jsonl'  # Output file for grid data

    # Run the filter
    filter_highways(input_pbf, motorized_pbf)

    # Build and export street network
    process_osm_to_Streetjsonl(motorized_pbf, street_jsonl)

    # Export Grid to .jsonl
    process_osm_to_Gridjsonl(motorized_pbf, grid_jsonl, 0.0005)

if __name__ == "__main__":
    num_runs = 1
    execution_times = []

    for i in range(num_runs):
        start_time = time.time()  # Start timing
        main()  # Run your code
        end_time = time.time()  # End timing
        execution_time = end_time - start_time
        execution_times.append(execution_time)
        print(f"Run {i+1}: {execution_time:.2f} seconds")

    average_time = sum(execution_times) / num_runs
    print(f"\nAverage execution time over {num_runs} runs: {average_time:.2f} seconds")
