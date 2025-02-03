import java.io.*;
import java.net.Socket;
import java.util.*;

public class PieceManager {
    private final BitfieldManager bitfieldManager;
    private final ConnectionManager connectionManager;
    private final ChokingManager chokingManager;

    public PieceManager(BitfieldManager bitfieldManager, ConnectionManager connectionManager, ChokingManager chokingManager ) {
        this.bitfieldManager = bitfieldManager;
        this.connectionManager = connectionManager;
        this.chokingManager = chokingManager;
    }

    public int getNextPieceToRequest() {
        for (int i = 0; i < bitfieldManager.getTotalPieces(); i++) {
            if (!bitfieldManager.hasPiece(i)) {
                return i;
            }
        }
        return -1; // No pieces left to request
    }


    public void handleFullFileResponse(int peerID, byte[] fileData) {
        try {
            long startTime = System.nanoTime();

            // Mark all pieces as received in the bitfield since we got the entire file
            bitfieldManager.setAllPieces();

            // Write the full file to disk
            FileManager.writeFullFile(peerID, fileData);

            long endTime = System.nanoTime();

            // Calculate download rate (bytes per millisecond)
            double downloadRate = fileData.length / ((endTime - startTime) / 1_000_000.0);
            chokingManager.updateDownloadRate(peerID, downloadRate);

            System.out.println("Full file successfully received and handled for Peer " + peerID);
        } catch (Exception e) {
            System.err.println("Error handling full file from Peer " + peerID + ": " + e.getMessage());
        }
    }


}
