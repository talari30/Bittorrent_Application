import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.System.out;

public class ConnectionManager {
    private int peerID;
    private ServerSocket serverSocket;
    private final Map<Integer, Socket> connectedPeers;
    private final Map<Integer, Boolean> peerChokingStatus;
    private boolean hasFile;
    private List<PeerInfo> peerInfoList;
    private final Map<Integer, byte[]> peerBitfields;
    private final Map<Socket, Integer> socketToPeerID;
    private final BitfieldManager bitfieldManager;
    private ChokingManager chokingManager;
    private PieceManager pieceManager;
    private final FileManager fileManager;
    byte[] handshakeMessage = new byte[32];
    byte[] protocolHeader = "P2PFILESHARINGPROJ".getBytes(); // 18-byte protocol identifier
    byte[] zeroBits = new byte[10];  // 10-byte zero bits
    private Logger logger;

    private DataInputStream input;
    private DataOutputStream output;
    private static final int CHOKE = 0;
    private static final int UNCHOKE = 1;
    private static final int BITFIELD = 5;

    // Constructor
    public ConnectionManager(int peerID, boolean hasFile,BitfieldManager bitfieldManager,FileManager fileManager ) {
        this.peerID = peerID;
        this.hasFile = hasFile;
        this.connectedPeers = new ConcurrentHashMap<>();
        this.peerBitfields = new ConcurrentHashMap<>();
        this.peerInfoList = new ArrayList<>();
        this.peerChokingStatus= new ConcurrentHashMap<>();
        this.bitfieldManager = bitfieldManager;
        this.fileManager = fileManager;
        this.socketToPeerID = new ConcurrentHashMap<>();
        this.logger = new Logger(peerID);
    }

    // Defer initialization of ChokingManager and PieceManager
    public void initialize(ChokingManager chokingManager, PieceManager pieceManager) {
        this.chokingManager = chokingManager;
        this.pieceManager = pieceManager;
    }




    public synchronized void addPeerConnection(int peerID, Socket socket) {
        connectedPeers.put(peerID, socket);
    }

    public synchronized Socket getPeerConnection(int peerID) {
        return connectedPeers.get(peerID);
    }


    // Start the server and handle incoming connections
    public void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Peer " + peerID + " started server on port " + port);
            logger.log(String.format("Peer %d started server on port %d", peerID, port)); // Log server start
            // Continuously listen for incoming connections
            new Thread(() -> {
                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("Peer " + peerID + " accepted a connection from " + clientSocket.getRemoteSocketAddress());
                        handleIncomingConnection(clientSocket);
                    } catch (IOException e) {
                        System.err.println("Error accepting connection: " + e.getMessage());
                    }
                }
            }).start();
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }


    // Connect to peers that appear before this peer in the peer map (by index)
    public void connectToPeers(Map<Integer, PeerInfo> peers) {
        this.peerInfoList = new ArrayList<>(peers.values());

        int currentPeerIndex = -1;
        for (int i = 0; i < peerInfoList.size(); i++) {
            if (peerInfoList.get(i).getPeerId() == this.peerID) {
                currentPeerIndex = i;
                break;
            }
        }

        if (currentPeerIndex == -1) {
            System.err.println("Error: Current peer ID not found in the peer list.");
            return;
        }

        // Connect to peers that come before this peer in the list (i.e., peers with lower index)
        for (int i = 0; i < currentPeerIndex; i++) {
            PeerInfo peer = peerInfoList.get(i);
            try {
                Socket socket = new Socket(peer.getHostName(), peer.getPort());
                addPeerConnection(peer.getPeerId(), socket);
                logger.logConnection(peer.getPeerId(), true);
                System.out.println("Peer " + this.peerID + " connected to peer " + peer.getPeerId());

                handleOutgoingConnection(socket, peer.getPeerId());
            } catch (IOException e) {
                System.err.println("Error connecting to peer " + peer.getPeerId() + ": " + e.getMessage());
            }
        }
    }

    public boolean allPeersConnected() {
        int expectedConnections = 0;

        for (PeerInfo peerInfo : peerInfoList) {
            if (peerInfo.getPeerId() < peerID) {
                expectedConnections++;
            }
        }

        // Check if all expected peers are connected
        return connectedPeers.size() == expectedConnections;
    }



    private void handleIncomingConnection(Socket clientSocket) {
        try {
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

            // Set the streams to class-level variables if needed
            this.input = in;
            this.output = out;

            // Proceed with the handshake and bitfield
            byte[] handshake = new byte[32];
            in.readFully(handshake);
            int remotePeerID = ByteBuffer.wrap(handshake, 28, 4).getInt();
            System.out.println("Received handshake from peer: " + remotePeerID);
            socketToPeerID.put(clientSocket, remotePeerID);
            addPeerConnection(remotePeerID, clientSocket);
            logger.logConnection(remotePeerID, false);  // Log accepted connection

            // Send handshake response
            out.write(createHandshakeMessage());
            out.flush();

            // Exchange bitfields
            sendBitfield(out);
            receiveBitfield(in, remotePeerID);
        } catch (IOException e) {
            System.err.println("Error handling incoming connection: " + e.getMessage());
        }
    }

    private void handleOutgoingConnection(Socket socket, int peerID) {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            this.input = in;
            this.output = out;

            out.write(createHandshakeMessage());
            out.flush();
            System.out.println("Sent handshake to peer: " + peerID);
            logger.logConnection(peerID, true);  // Log handshake initiated

            // Receive handshake response
            byte[] handshake = new byte[32];
            in.readFully(handshake);
            System.out.println("Handshake response received from peer: " + peerID);

            // Exchange bitfields
            sendBitfield(out);
            receiveBitfield(in, peerID);
        } catch (IOException e) {
            System.err.println("Error handling outgoing connection to peer " + peerID + ": " + e.getMessage());
        }
    }


    private void sendBitfield(DataOutputStream out) throws IOException {
        byte[] bitfield = bitfieldManager.getBitfield();
        ByteBuffer messageBuffer = ByteBuffer.allocate(4 + 1 + bitfield.length);
        messageBuffer.putInt(1 + bitfield.length);
        messageBuffer.put((byte) BITFIELD);
        messageBuffer.put(bitfield);
        out.write(messageBuffer.array());
        out.flush();

        logger.log(String.format("Peer %d sent bitfield of length %d to connected peer.", peerID, bitfield.length));
        logger.log(String.format("Bitfield sent: %s", Arrays.toString(bitfield)));

        System.out.println("Sent bitfield to peer");
    }

    private void receiveBitfield(DataInputStream in, int peerID) throws IOException {
        int length = in.readInt();
        byte type = in.readByte();
        if (type == BITFIELD) {
            byte[] bitfield = new byte[length - 1];
            in.readFully(bitfield);
            peerBitfields.put(peerID, bitfield);
            System.out.println("Received bitfield from peer " + peerID);
            System.out.println("Bitfield received from peer is : " + Arrays.toString(bitfield));

            logger.log(String.format("Peer %d received bitfield of length %d from peer %d", this.peerID, bitfield.length, peerID));
            logger.log(String.format("Bitfield received from peer %d: %s", peerID, Arrays.toString(bitfield)));

            exchangeInterestMessages(peerID);
        }
    }

    public void exchangeInterestMessages(int receiverPeerID) {
        try {
            // Get the peer's bitfield from the stored map
            byte[] peerBitfield = peerBitfields.get(receiverPeerID);
            if (peerBitfield == null) {
                System.err.println("Bitfield for peer " + receiverPeerID + " is not available. Skipping...");
                return;
            }

            // Determine interest
            boolean interested = false;
            for (int i = 0; i < bitfieldManager.getTotalPieces(); i++) {
                if (!bitfieldManager.hasPiece(i) && peerHasPiece(peerBitfield, i)) {
                    interested = true;
                    break;
                }
            }

            if (this.output != null) {
                byte messageType = (byte) (interested ? 2 : 3); // 2 for INTERESTED, 3 for NOT INTERESTED
                int messageLength = 1; // The message length is just the type for INTERESTED/NOT INTERESTED

                ByteBuffer messageBuffer = ByteBuffer.allocate(4 + messageLength);
                messageBuffer.putInt(messageLength);
                messageBuffer.put(messageType);
                byte[] fullMessage = messageBuffer.array();

                this.output.write(fullMessage);
                this.output.flush();

                System.out.println("Sent " + (interested ? "INTERESTED" : "NOT INTERESTED") + " message to peer " + receiverPeerID);
                System.out.println("Bitfield sent along with " + (interested ? "INTERESTED" : "NOT INTERESTED") + " message: " + Arrays.toString(fullMessage));

                if (interested) {
                    logger.logInterested(receiverPeerID);
                } else {
                    logger.logNotInterested(receiverPeerID);
                }


                processIncomingMessage(receiverPeerID);


            } else {
                System.err.println("Output stream is not initialized. Cannot send interest message.");
            }
        } catch (IOException e) {
            System.err.println("Error sending interest message to peer " + receiverPeerID + ": " + e.getMessage());
        }
    }

    public void startListeningForMessages( int receiverPeerID) {
        while (true) {
            processIncomingMessage(receiverPeerID);
        }
    }





    public void processIncomingMessage(int receiverPeerID) {
//        System.out.println("inside ProcessIncomingmessage");
        try  {

            int length = this.input.readInt();

            byte[] bitfield;

            byte type = this.input.readByte();

            switch (type) {
                case 0: // Choke message
                    System.out.println("Received CHOKE message from peer "+receiverPeerID );
                    handleChokeUnchokeMessage(receiverPeerID, true); // Update choking status
                    break;

                case 1: // Unchoke message
                    System.out.println("Received UNCHOKE message from peer "+receiverPeerID );
                    handleChokeUnchokeMessage(receiverPeerID, false); // Update choking status
                    break;

                case 2: // Interested message
                    System.out.println("Received INTERESTED message from peer "+receiverPeerID );
                    chokingManager.markInterested(receiverPeerID); // Mark the peer as interested
                    break;

                case 3: // Not Interested message
                    System.out.println("Received NOT INTERESTED message from peer " +receiverPeerID);
                    chokingManager.markNotInterested(receiverPeerID); // Mark the peer as not interested
                    break;

                case 4: // Have message
                    System.out.println("Received HAVE message from peer " +receiverPeerID);
                    bitfield = new byte[length - 1];
                    this.input.readFully(bitfield);
                    peerBitfields.put(peerID, bitfield);
                    break;

                case 5: // Bitfield message
                    bitfield = new byte[length - 1]; // Payload length is (length - 1 byte for type)
                    this.input.readFully(bitfield); // Read the bitfield
                    System.out.println("Received BITFIELD message from peer "+receiverPeerID );
//                    peerBitfields.put(senderPeerID, bitfield);
//                    exchangeInterestMessages();
                    break;


                case 6: // Request message
                    System.out.println("Received REQUEST message from peer "+ receiverPeerID );
                    byte[] fullFileData = fileManager.getFullFileData(peerID);
                    sendFullFile(receiverPeerID, fullFileData);
                    break;

                case 7: // Piece message
                    System.out.println("Received FULL FILE message from peer " + receiverPeerID);
                    byte[] fileData = new byte[length - 1]; // Remaining payload is the file data
                    this.input.readFully(fileData);
                    // Handle the received file data
                    pieceManager.handleFullFileResponse(peerID, fileData);
                    finalizeDownload();
                    break;

                default:
                    System.err.println("Received unknown message type: " + type + " from peer "+receiverPeerID );
                    break;
            }

//            System.out.println("Out of ProcessIncomingmessage");
        } catch (IOException e) {
            System.err.println("Error processing message "+ e.getMessage());
        }
    }

    public void sendFullFile(int receiverPeerID, byte[] fullFileData) {
        try {
            int messageLength = fullFileData.length + 1; // Length of the full file data + 1 byte for message type
            this.output.writeInt(messageLength);
            this.output.writeByte(7); // Message type for full file response
            this.output.write(fullFileData);
            this.output.flush();
            logger.log(String.format("Sent complete file to peer %d", peerID));
            chokingManager.markNotInterested(receiverPeerID);
            System.out.println("Sent full file to peer " + receiverPeerID);

        } catch (IOException e) {
            System.err.println("Error sending full file to peer " + receiverPeerID + ": " + e.getMessage());
        }
    }

    public void finalizeDownload() {
        System.out.println("Peer " + peerID + " finalized");

        // Notify all connected peers that this peer now has the complete file
        sendHaveMessageToAll(bitfieldManager.getBitfield());
        this.hasFile = true;
        System.out.println("Peer " + peerID + ": Download complete!");
        logger.logComplete();


    }

    // Create the handshake message
    private byte[] createHandshakeMessage() {


        byte[] peerIDBytes = ByteBuffer.allocate(4).putInt(peerID).array();

        System.arraycopy(protocolHeader, 0, handshakeMessage, 0, protocolHeader.length);
        System.arraycopy(zeroBits, 0, handshakeMessage, protocolHeader.length, zeroBits.length);
        System.arraycopy(peerIDBytes, 0, handshakeMessage, protocolHeader.length + zeroBits.length, peerIDBytes.length);

        return handshakeMessage;
    }


    // Helper method to check if a peer has a specific piece
    private boolean peerHasPiece(byte[] bitfield, int pieceIndex) {
        int byteIndex = pieceIndex / 8;
        int bitPosition = pieceIndex % 8;
        return (bitfield[byteIndex] & (1 << (7 - bitPosition))) != 0;
    }

    // Send a "have" message to all connected peers indicating the peer has downloaded a piece or the file
    public void sendHaveMessageToAll(byte[] bitfield) {
        for (Map.Entry<Integer, Socket> entry : connectedPeers.entrySet()) {
            int peerID = entry.getKey();
            Socket socket = entry.getValue();

            try {
                // Prepare the message to send the whole bitfield
                int messageLength = 1 + bitfield.length; // 1 byte for the message type + bitfield size
                ByteBuffer messageBuffer = ByteBuffer.allocate(4 + messageLength);
                messageBuffer.putInt(messageLength); // Add message length
                messageBuffer.put((byte) 4); // Message type for 'Have' (4)
                messageBuffer.put(bitfield); // Add the entire bitfield

                byte[] fullMessage = messageBuffer.array();

                // Send the message
                this.output.write(fullMessage);
                this.output.flush();

                System.out.println("Peer " + this.peerID + " sent Have message to peer " + peerID);
                logger.logHave(peerID);
                logger.logDownload(peerID);

            } catch (IOException e) {
                System.err.println("Error sending bitfield to peer " + peerID + ": " + e.getMessage());
            }
        }
    }


    void sendChokeUnchokeMessages() {
        for (int peerID : getConnectedPeers().keySet()) {
            if (chokingManager.getPreferredNeighbors().contains(peerID) || peerID == chokingManager.getoptimisticallyUnchokedPeer()) {
                sendUnchokeMessage(peerID);
            } else {
                sendChokeMessage(peerID);
            }
            //new Thread(() -> startListeningForMessages(peerID)).start();
            processIncomingMessage(peerID);

        }
    }

    public void sendChokeMessage(int peerID) {
        try {
            Socket socket = getPeerConnection(peerID);
            if (socket == null || socket.isClosed()) {
                System.err.println("Cannot send CHOKE message to peer " + peerID + ": Socket is closed or unavailable.");
                return;
            }
            sendMessage(new DataOutputStream(socket.getOutputStream()), CHOKE, new byte[0]);

            System.out.println("Sent CHOKE message to peer " + peerID);
        } catch (IOException e) {
            System.err.println("Error sending CHOKE message to peer " + peerID + ": " + e.getMessage());
        }
    }

    public void sendUnchokeMessage(int peerID) {
        try {
            Socket socket = getPeerConnection(peerID);
            if (socket == null || socket.isClosed()) {
                System.err.println("Cannot send UNCHOKE message to peer " + peerID + ": Socket is closed or unavailable.");
                return;
            }

            sendMessage(new DataOutputStream(socket.getOutputStream()), UNCHOKE, new byte[0]);

            System.out.println("Sent UNCHOKE message to peer " + peerID);
        } catch (IOException e) {
            System.err.println("Error sending UNCHOKE message to peer " + peerID + ": " + e.getMessage());
        }
    }

    private void sendMessage(DataOutputStream out, int type, byte[] payload) throws IOException {
        int length = 1 + payload.length;
        output. writeInt(length);
        output. writeByte(type);
        output. write(payload);
        output. flush();
        System.out.println("Sending message: length=" + (1 + payload.length) + ", type=" + type + ", payload size=" + payload.length);

    }


    public void updateChokingStatus(int peerID, boolean isChoked) {
        peerChokingStatus.put(peerID, isChoked);
        System.out.println("Updated choking status for peer " + peerID + ": " + (isChoked ? "Choked" : "Unchoked"));
        onChokeStatusChange(peerID, isChoked);
    }

    public boolean isChokedByPeer(int peerID) {
        return peerChokingStatus.getOrDefault(peerID, true); // Default to true (choked) if unknown
    }

    // Handle choke/unchoke messages in the connection manager
    public void handleChokeUnchokeMessage(int peerID, boolean isChoked) {
        updateChokingStatus(peerID, isChoked);

        if (isChoked) {
            logger.logChoked(peerID);
        } else {
            logger.logUnchoked(peerID);
        }
    }

    // Event listener for choke/un-choke actions
    private void onChokeStatusChange(int peerID, boolean isChoked) {
        // If the peer is unchoked, we can now request the whole file from it
        if (!isChoked && !this.hasFile) {
            new Thread(() -> {
                if (!isChokedByPeer(peerID)) {
                    // Request all pieces (in this case, equivalent to requesting the entire file)
                    handlePieceRequest(peerID);
                }
            }).start();
        }
    }


    public void handlePieceRequest(int peerID) {
        if (isChokedByPeer(peerID)) {
            System.out.println("Cannot request file from peer " + peerID + ": Peer has choked us.");
            return; // Do not send the request if choked
        }

        try {
            int totalFileRequestLength = 5; // Adjust this value if necessary
            this.output.writeInt(totalFileRequestLength); // Message length
            this.output.writeByte(6); // Piece request message type (assuming `6` means request for all pieces)
            this.output.flush();
            logger.log(String.format("Requested full file from peer %d", peerID));


            System.out.println("Requested the complete file from peer " + peerID);
        } catch (IOException e) {
            System.err.println("Error requesting file from peer " + peerID + ": " + e.getMessage());
        }
    }


    public void stopServer() {
        try {
            serverSocket.close();
            for (Socket socket : connectedPeers.values()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
    }

    public Map<Integer, Socket> getConnectedPeers() {
        return connectedPeers;
    }
}