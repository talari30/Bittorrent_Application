import java.io.*;
import java.util.*;

public class ConfigManager {
    private int peerID;
    private int numberOfPreferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private int fileSize;
    private int pieceSize;
    private Map<Integer, PeerInfo> peerInfoMap;
    private int port;

    public ConfigManager(int peerID) {
        this.peerID = peerID;
        this.peerInfoMap =new LinkedHashMap<>();
    }

    // Load Common.cfg
    public void loadConfig(String configFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] config = line.split("\\s+");

                switch (config[0]) {
                    case "NumberOfPreferredNeighbors":
                        numberOfPreferredNeighbors = Integer.parseInt(config[1]);
                        break;
                    case "UnchokingInterval":
                        unchokingInterval = Integer.parseInt(config[1]);
                        break;
                    case "OptimisticUnchokingInterval":
                        optimisticUnchokingInterval = Integer.parseInt(config[1]);
                        break;
                    case "FileName":
                        fileName = config[1];
                        break;
                    case "FileSize":
                        fileSize = Integer.parseInt(config[1]);
                        break;
                    case "PieceSize":
                        pieceSize = Integer.parseInt(config[1]);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown config parameter: " + config[0]);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading config file: " + e.getMessage());
        }
    }

    // Load PeerInfo.cfg
    public void loadPeerInfo(String peerInfoFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(peerInfoFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] info = line.split("\\s+");

                int peerId = Integer.parseInt(info[0]);
                String hostName = info[1];
                int peerPort = Integer.parseInt(info[2]);
                boolean hasFile = info[3].equals("1");

                PeerInfo peer = new PeerInfo(peerId, hostName, peerPort, hasFile);
                peerInfoMap.put(peerId, peer);

                if (peerId == this.peerID) {
                    this.port = peerPort;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading peer info file: " + e.getMessage());
        }
    }

    public int getNumberOfPreferredNeighbors() {
        return this.numberOfPreferredNeighbors;
    }

    public int getUnchokingInterval() {
        return unchokingInterval;
    }

    public int getOptimisticUnchokingInterval() {
        return optimisticUnchokingInterval;
    }

    public String getFileName() {
        return fileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getPieceSize() {
        return pieceSize;
    }

    public int getPort() {
        return port;
    }

    public Map<Integer, PeerInfo> getPeerInfo() {
        return peerInfoMap;
    }
}

// Getters for config values


