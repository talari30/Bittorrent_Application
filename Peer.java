import java.io.File;
import java.io.IOException;

public class Peer {
    private final int peerID;
    private ConfigManager configManager;
    private ConnectionManager connectionManager;
    private BitfieldManager bitfieldManager;
    private ChokingManager chokingManager;
    private PieceManager pieceManager;
    private FileManager fileManager;

    // Constructor
    public Peer(int peerID) {
        this.peerID = peerID;
        this.configManager = new ConfigManager(peerID);
    }

    public void initialize(String configFilePath, String peerInfoFilePath) {
        configManager.loadConfig(configFilePath);
        configManager.loadPeerInfo(peerInfoFilePath);

        int fileSize = configManager.getFileSize();
        int pieceSize = configManager.getPieceSize();
        PeerInfo peerInfo = configManager.getPeerInfo().get(peerID);

        this.bitfieldManager = new BitfieldManager(fileSize, pieceSize);
        this.fileManager = new FileManager();

        if (peerInfo.hasFile()) {
            bitfieldManager.setAllPieces();
        }

        this.connectionManager = new ConnectionManager(peerID, configManager.getPeerInfo().get(peerID).hasFile(), bitfieldManager, fileManager);
        this.chokingManager = new ChokingManager(
                peerID,
                configManager.getNumberOfPreferredNeighbors(),
                configManager.getUnchokingInterval(),
                configManager.getOptimisticUnchokingInterval()
        );

        this.pieceManager = new PieceManager(bitfieldManager, connectionManager, chokingManager);

        connectionManager.initialize(chokingManager, pieceManager); // Inject dependencies
        chokingManager.initialize(connectionManager); // Inject dependency

        connectionManager.startServer(configManager.getPort());

        connectionManager.connectToPeers(configManager.getPeerInfo());



    }

    public void run() {
        System.out.println("Peer " + peerID + " started");
        // Start the choking management process
        chokingManager.initialize();
    }


    // Finalize the download process
    public void finalizeDownload() {
        System.out.println("Peer " + peerID + " finalized");
        connectionManager.sendHaveMessageToAll(bitfieldManager.getBitfield());
        System.out.println("Peer " + peerID + ": Download complete!");
    }

    // Main method to start the peer
    public static void main(String[] args) throws InterruptedException {
        if (args.length != 1) {
            System.out.println("Usage: java Peer <peerID>");
            return;
        }

        int peerID = Integer.parseInt(args[0]);
        String configFilePath = "Common.cfg";
        String peerInfoFilePath = "PeerInfo.cfg";

        Peer peer = new Peer(peerID);
        peer.initialize(configFilePath, peerInfoFilePath);
        System.out.println("Peer " + peerID + " initialized!");

        synchronized (peer.getConnectionManager()) {
            while (!peer.getConnectionManager().allPeersConnected()) {
                peer.getConnectionManager().wait();
            }
        }
        peer.run();
        System.out.println("End and Start of RUN");

    }

    public ConnectionManager getConnectionManager() {
        return this.connectionManager;
    }
}
