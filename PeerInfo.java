public class PeerInfo {
    private int peerId;
    private String hostName;
    private int port;
    private boolean hasFile;

    public PeerInfo(int peerId, String hostName, int port, boolean hasFile) {
        this.peerId = peerId;
        this.hostName = hostName;
        this.port = port;
        this.hasFile = hasFile;
    }

    // Getters for PeerInfo attributes
    public int getPeerId() {
        return peerId;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public boolean hasFile() {
        return hasFile;
    }
}
