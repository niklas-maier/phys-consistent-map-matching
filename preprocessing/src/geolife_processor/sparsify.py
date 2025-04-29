import xml.etree.ElementTree as ET
from datetime import datetime, timedelta
import os

def parse_gpx(file_path):
    tree = ET.parse(file_path)
    root = tree.getroot()
    
    # Detect namespace from the root element (if declared)
    namespace = {}
    if '}' in root.tag:
        # Extract namespace from the root tag (e.g., '{http://...}gpx')
        namespace_uri = root.tag.split('}', 1)[0].strip('{')
        namespace = {'gpx': namespace_uri}
    else:
        # No namespace declared; use empty prefix
        namespace = {'gpx': ''}
    
    # Extract all track points (trkpt) with or without namespace
    trkpts = root.findall('.//gpx:trkpt', namespace)
    
    # If no track points found, try again without namespace
    if not trkpts:
        trkpts = root.findall('.//trkpt')
    
    points = []
    for trkpt in trkpts:
        lat = float(trkpt.attrib['lat'])
        lon = float(trkpt.attrib['lon'])
        
        # Handle elevation (ele)
        ele_element = trkpt.find('gpx:ele', namespace) if namespace['gpx'] else trkpt.find('ele')
        ele = float(ele_element.text) if ele_element is not None else None
        
        # Handle time
        time_element = trkpt.find('gpx:time', namespace) if namespace['gpx'] else trkpt.find('time')
        if time_element is not None:
            time_str = time_element.text
            try:
                time = datetime.strptime(time_str, '%Y-%m-%dT%H:%M:%SZ')
            except ValueError:
                time = datetime.strptime(time_str, '%Y-%m-%dT%H:%M:%S.%fZ')
        else:
            time = None
        
        points.append({
            'lat': lat,
            'lon': lon,
            'ele': ele,
            'time': time
        })
    
    return points

def sparsify_points(points, interval_seconds):
    if not points:
        return []
    
    sparsified_points = [points[0]]
    last_time = points[0]['time']
    
    for point in points[1:]:
        current_time = point['time']
        time_diff = (current_time - last_time).total_seconds()
        
        if time_diff >= interval_seconds:
            sparsified_points.append(point)
            last_time = current_time
    
    return sparsified_points

import xml.dom.minidom as minidom

def write_gpx(file_path, points):
    if not points:
        raise ValueError("Empty points list. Cannot write GPX file.")
    
    # Define the root element with proper namespace for GPX 1.1
    root = ET.Element('gpx', {
        'version': '1.1',
        'creator': 'OutlierRemover',
        'xmlns': 'http://www.topografix.com/GPX/1/1',
        'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
        'xsi:schemaLocation': 'http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd'
    })
    
    trk = ET.SubElement(root, 'trk')
    trkseg = ET.SubElement(trk, 'trkseg')
    
    for point in points:
        trkpt = ET.SubElement(trkseg, 'trkpt', {'lat': str(point['lat']), 'lon': str(point['lon'])})
        if point['ele'] is not None:
            ET.SubElement(trkpt, 'ele').text = str(point['ele'])
        if point['time'] is not None:
            ET.SubElement(trkpt, 'time').text = point['time'].strftime('%Y-%m-%dT%H:%M:%SZ')
    
    # Format the XML with indentation and newlines
    rough_xml = ET.tostring(root, encoding='UTF-8')
    reparsed = minidom.parseString(rough_xml)
    pretty_xml = reparsed.toprettyxml(indent="  ", encoding='UTF-8')
    
    # Remove empty lines
    pretty_xml = b'\n'.join([line for line in pretty_xml.splitlines() if line.strip()])
    
    with open(file_path, 'wb') as f:
        f.write(pretty_xml)

def main():
    input_folder = '/home/niklas/TestData2'  # Folder containing GPX files
    output_folder = '/home/niklas/TestData3'  # Folder to save sparsified GPX files
    interval_seconds = 30  # Desired interval in seconds

    # Ensure the output folder exists
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    # Process each GPX file in the input folder
    for filename in os.listdir(input_folder):
        if filename.endswith('.gpx'):
            input_file = os.path.join(input_folder, filename)
            output_file = os.path.join(output_folder, f"sparsified_{filename}")

            # Parse the GPX file
            points = parse_gpx(input_file)

            # Check if points were parsed successfully
            if not points:
                print(f"No track points found in {filename}. Skipping.")
                continue

            # Sparsify the points
            sparsified_points = sparsify_points(points, interval_seconds)

            # Check if sparsified_points is not empty
            if not sparsified_points:
                print(f"No points left after sparsifying {filename}. Skipping.")
                continue

            # Write the sparsified points to a new GPX file
            write_gpx(output_file, sparsified_points)

            print(f"Sparsified {filename} saved to {output_file}")

if __name__ == '__main__':
    main()