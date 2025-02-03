import java.util.*;
import java.util.concurrent.*;

public class ChokingManager {
    private final int peerID;
    private ConnectionManager connectionManager;
    private final int numberOfPreferredNeighbors;
    private final int unchokingInterval;
    private final int optimisticUnchokingInterval;

    private final Map<Integer, Double> downloadRates = new ConcurrentHashMap<>();
    private final Set<Integer> interestedPeers = ConcurrentHashMap.newKeySet();
    private final List<Integer> preferredNeighbors = Collections.synchronizedList(new ArrayList<>());
    private int optimisticallyUnchokedPeer = -1;

    public ChokingManager(int peerID, int numberOfPreferredNeighbors, int unchokingInterval, int optimisticUnchokingInterval) {
        this.peerID = peerID;
        this.numberOfPreferredNeighbors = numberOfPreferredNeighbors;
        this.unchokingInterval = unchokingInterval;
        this.optimisticUnchokingInterval = optimisticUnchokingInterval;
    }

    public void initialize(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void initialize() {
        Timer unchokeTimer = new Timer();
        Timer optimisticUnchokeTimer = new Timer();

        unchokeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                selectPreferredNeighbors();
            }
        }, 0, unchokingInterval * 1000);

        optimisticUnchokeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                selectOptimisticUnchoke();
            }
        }, 0, optimisticUnchokingInterval * 1000);
    }

    private void selectPreferredNeighbors() {
        synchronized (interestedPeers) {
            // Remove the current peer from interested peers
            interestedPeers.remove(peerID);

            if (downloadRates.isEmpty() || downloadRates.values().stream().allMatch(rate -> rate == 0.0)) {
                // If all download rates are 0.0, select random peers as preferred neighbors
                List<Integer> randomPeers = new ArrayList<>(interestedPeers);
                Collections.shuffle(randomPeers); // Shuffle the list for random selection

                preferredNeighbors.clear();
                for (int i = 0; i < numberOfPreferredNeighbors && i < randomPeers.size(); i++) {
                    preferredNeighbors.add(randomPeers.get(i));
                }
            } else {
                List<Map.Entry<Integer, Double>> sortedPeers = new ArrayList<>(downloadRates.entrySet());
                sortedPeers.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

                preferredNeighbors.clear();
                for (int i = 0; i < numberOfPreferredNeighbors && i < sortedPeers.size(); i++) {
                    int peer = sortedPeers.get(i).getKey();
                    if (peer != peerID) { // Make sure not to add itself
                        preferredNeighbors.add(peer);
                    }
                }
            }

            logPreferredNeighbors();
            connectionManager.sendChokeUnchokeMessages();
        }
    }




    private void selectOptimisticUnchoke() {
        synchronized (interestedPeers) {
            List<Integer> chokedInterestedPeers = new ArrayList<>(interestedPeers);
            chokedInterestedPeers.removeAll(preferredNeighbors);
            if (!chokedInterestedPeers.isEmpty()) {
                optimisticallyUnchokedPeer = chokedInterestedPeers.get(new Random().nextInt(chokedInterestedPeers.size()));
                logOptimisticUnchoke();
                connectionManager.sendUnchokeMessage(optimisticallyUnchokedPeer);
            }
        }
    }



    public boolean isInterestedInPeer(int peerID) {
        return interestedPeers.contains(peerID);
    }

    public void updateDownloadRate(int peerID, double rate) {
        downloadRates.put(peerID, rate);
    }

    public void markInterested(int peerID) {
        interestedPeers.add(peerID);
    }

    public void markNotInterested(int peerID) {
        interestedPeers.remove(peerID);
    }

    public int getoptimisticallyUnchokedPeer(){return optimisticallyUnchokedPeer;}

    public List<Integer> getPreferredNeighbors(){return preferredNeighbors;}


    private void logPreferredNeighbors() {
        System.out.println("Preferred neighbors for peer " + peerID + ": " + preferredNeighbors);
    }

    private void logOptimisticUnchoke() {
        System.out.println("Optimistically unchoked peer for peer " + peerID + ": " + optimisticallyUnchokedPeer);
    }
}
