import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
    private File logFile;
    private int peerID;
    private boolean logComplete = false;

    public Logger(int peerID) {
        this.peerID = peerID;
        logFile = new File(String.format("log_peer_%s.log", peerID));
        try {
            if (logFile.createNewFile()) {
                System.out.println("Log file created: " + logFile.getName());
            } else {
                System.out.println("Log file already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred while creating the log file: " + e.getMessage());
        }
    }

    public void log(String message) {
        try (FileWriter fw = new FileWriter(logFile, true)) {
            fw.write(String.format("%s: %s.%n", LocalDateTime.now(), message));
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the log file: " + e.getMessage());
        }
    }

    public void logConnection(int peerID, boolean initiatedConnection) {
        if (initiatedConnection) {
            log(String.format("Peer %d makes a connection to Peer %d", this.peerID, peerID));
        } else {
            log(String.format("Peer %d is connected from Peer %d", this.peerID, peerID));
        }
    }

    public void logPreferredNeighbors(List<Integer> neighborIDs) {
        StringBuilder message = new StringBuilder(String.format("Peer %d has the preferred neighbors: ", peerID));
        String delimiter = "";
        for (int neighborID : neighborIDs) {
            message.append(delimiter).append(neighborID);
            delimiter = ", ";
        }
        log(message.toString());
    }

    public void logOptimisticallyUnchokedNeighbor(int neighborID) {
        log(String.format("Peer %d has the optimistically unchoked neighbor %d", peerID, neighborID));
    }

    public void logUnchoked(int byPeerID) {
        log(String.format("Peer %d is unchoked by %d", peerID, byPeerID));
    }

    public void logChoked(int byPeerID) {
        log(String.format("Peer %d is choked by %d", peerID, byPeerID));
    }

    public void logHave(int fromPeerID) {
        log(String.format("Peer %d received the 'have' message  %d", peerID, fromPeerID));
    }

    public void logInterested(int fromPeerID) {
        log(String.format("Peer %d received the 'interested' message from %d", peerID, fromPeerID));
    }

    public void logNotInterested(int fromPeerID) {
        log(String.format("Peer %d received the 'not interested' message from %d", peerID, fromPeerID));
    }

    public void logDownload(int fromPeerID) {
        log(String.format("Peer %d has downloaded the file from %d", peerID, fromPeerID));
    }

    // Log when a peer has completed downloading the entire file
    public void logComplete() {
        if (logComplete) return;
        log(String.format("Peer %d has downloaded the complete file", peerID));
        logComplete = true;
    }
}
