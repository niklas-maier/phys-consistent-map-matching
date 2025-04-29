package com.mycompany.masterproject.ui;

import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;

import javax.swing.JOptionPane;

import com.mycompany.masterproject.data.GPXData;
import com.mycompany.masterproject.gpx.GPXLoader;

import java.awt.dnd.DnDConstants;


public class FileDropListener extends DropTargetAdapter {

    private final GPXLoader gpxLoader;
    private final DrawingLogic drawingLogic;

    public FileDropListener(GPXLoader gpxLoader, DrawingLogic drawingLogic) {
        this.gpxLoader = gpxLoader;
        this.drawingLogic=drawingLogic;
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        try {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);

                @SuppressWarnings("unchecked")
                List<File> droppedFiles = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                File file = droppedFiles.get(0);

                if (file.getName().toLowerCase().endsWith(".gpx")) {
                    // Load the GPX file using GPXLoader
                    GPXData gpxData = gpxLoader.loadGPXTrack(file);

                    if (gpxData != null) {
                        // Notify App to update the map
                        drawingLogic.handleGPXData(gpxData);
                    }
                    dtde.dropComplete(true);
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid file format. Please drop a .gpx file.");
                    dtde.dropComplete(false);
                }
            } else {
                dtde.rejectDrop();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error handling dropped file: " + ex.getMessage());
            ex.printStackTrace();
            dtde.dropComplete(false);
        }
    }
}

