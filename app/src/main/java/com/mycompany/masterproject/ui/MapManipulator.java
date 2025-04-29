package com.mycompany.masterproject.ui;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.*;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

public class MapManipulator implements MouseListener, MouseMotionListener, MouseWheelListener {

    private JXMapViewer mapViewer;
    private Point startPoint;

    public MapManipulator(JXMapViewer mapViewer) {
        this.mapViewer = mapViewer;
        this.startPoint = null;

        // Attach this as listener
        mapViewer.addMouseListener(this);
        mapViewer.addMouseMotionListener(this);
        mapViewer.addMouseWheelListener(this);

        
    }

    // Implement MouseListener methods
    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            startPoint = e.getPoint();
            mapViewer.requestFocusInWindow();//makes it so that no other buttons maintain focus
            mapViewer.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            mapViewer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    // Other MouseListener methods you may not need
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    // Implement MouseMotionListener methods
    @Override
    public void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && startPoint != null) {
            Point endPoint = e.getPoint();
            int dx = endPoint.x - startPoint.x;
            int dy = endPoint.y - startPoint.y;

            // Calculate the new center position based on the drag distance
            GeoPosition currentPosition = mapViewer.getCenterPosition();
            Point2D currentPoint = mapViewer.getTileFactory().geoToPixel(currentPosition, mapViewer.getZoom());

            Point2D newPoint = new Point2D.Double(currentPoint.getX() - dx, currentPoint.getY() - dy);
            GeoPosition newPosition = mapViewer.getTileFactory().pixelToGeo(newPoint, mapViewer.getZoom());

            mapViewer.setCenterPosition(newPosition);

            startPoint = endPoint;
        }
    }

    @Override public void mouseMoved(MouseEvent e) {}

    // Implement MouseWheelListener methods
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double rotation = e.getPreciseWheelRotation();
        if (rotation < 0) {
            mapViewer.setZoom(mapViewer.getZoom() - 1); // Zoom in
        } else {
            mapViewer.setZoom(mapViewer.getZoom() + 1); // Zoom out
        }
    }

    public void centerOnPoint(GeoPosition position) {
        if (position != null) {
            mapViewer.setCenterPosition(position);
            mapViewer.setZoom(5);
        }
    }
}
