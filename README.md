**Peer-to-Peer File Sharing System**

**Overview**
This project is a peer-to-peer (P2P) file-sharing system. The implementation allows multiple peers to connect and exchange files in a
distributed manner. Each peer in the system can either initiate a file transfer or request data from other peers, maintaining a
decentralized network architecture inspired by BitTorrent.

The implemented P2P network handles functionalities such as:

Peer Connection and Handshake: Establishing initial peer connections and exchanging information.
Choking/Unchoking Mechanism: Selective bandwidth allocation among peers for efficient file sharing.
Interest Management: Handling interest and disinterest in different pieces of files.
File Transfer: Sending and receiving files between peers.
Logging System: Logging the activities of each peer for monitoring purposes.

**Detailed Workflow**
1. Initialization Phase
2. Handshake and Bitfield Exchange
3. Choking and Interest Management
4. File Request and Transfer
5. Logging
Logging Specifications
**Features**
Distributed File Transfer: Distribute file transfer workload across peers.
Choking/Unchoking Management: Controls who has access to downloading to maintain fairness.
Interest Management: Efficient handling of which peers are interested in downloading pieces.
Piece-based File Transfer: Files are divided into pieces and shared between peers, ensuring faster downloads.
Automated Logging: All activities, including connections, transfers, choking, and piece downloads, are logged.

**Key Components:
**Peer: Represents an instance in the P2P network.
ConnectionManager: Manages incoming and outgoing peer connections.
ChokingManager: Manages choking and unchoking based on download speeds.
PieceManager: Handles piece selection and requests.
FileManager: Splits and assembles files and manages storage.
Logger: Logs peer activity for debugging and auditing.

**Classes**
Peer Class
The main entry point for each peer.
Initializes components such as ConnectionManager, BitfieldManager, ChokingManager, etc.
Starts the server for accepting incoming peer connections.
Handles file sharing tasks such as connecting to other peers, requesting pieces, and managing downloads.
Key Methods:
initialize(): Sets up configuration and initializes various components.
run(): The main loop of the peer, constantly requests pieces until the file is completely downloaded.
finalizeDownload(): Handles actions after the full file is received.

**ConfigManager Class**
Loads configuration data (Common.cfg) and peer information (PeerInfo.cfg).
Stores configurations like file size, piece size, and peer details.
Key Methods:
loadConfig(): Loads global settings.
loadPeerInfo(): Loads information about all peers.

**ConnectionManager Class**
Manages all peer connections, including both incoming and outgoing.
Handles the handshake, bitfield exchange, and message passing.
Key Methods:
startServer(): Starts a server to listen for incoming connections.
connectToPeers(): Initiates connections to other peers.
handleIncomingConnection(): Handles connection requests received by the peer.
sendHaveMessageToAll(): Sends 'have' messages to all connected peers to notify that a piece is downloaded.

**ChokingManager Class**
Implements the choking/unchoking mechanism to manage available bandwidth among peers.
Selects preferred and optimistically unchoked neighbors.
Key Methods:
initialize(): Starts the choking management timer.
selectPreferredNeighbors(): Chooses the peers that can download.
PieceManager Class
Handles the process of requesting and receiving pieces of the file.
Decides which piece to request from which peer.
Key Methods:
getNextPieceToRequest(): Determines the next piece to be requested.
handlePieceRequest(): Handles the actual piece request to other peers.

**BitfieldManager Class**
Manages the bitfield representing the pieces of the file that the peer has.
Updates bitfield as pieces are downloaded.
Key Methods:
setAllPieces(): Marks all pieces as available if the peer starts with the complete file.
markPieceAsReceived(): Marks a specific piece as received.

**FileManager Class**
Handles file operations such as reading, writing, splitting, and assembling.
Key Methods:
getFullFileData(): Reads the full file data to be sent.
writeFullFile(): Writes the fully received file to disk.

**Logger Class**
Handles the logging of all key activities performed by a peer.
Each peer has its log file (log_peer_<peerID>.log).
Key Methods:
logConnection(): Logs connections between peers.
logPreferredNeighbors(): Logs the preferred neighbors.
logChoked() / logUnchoked(): Logs choke/unchoke events.
logDownload(): Logs piece download events.
Setup and Installation
Prerequisites
Java JDK 17
IntelliJ IDEA (or any preferred IDE)

Installation Steps
Clone the repository:

**git clone https://github.com/Abhinavasai/BitTorrent-Application---Java/tree/feature-intrestedAlgo**

Open Project: Import the project into IntelliJ IDEA or any Java IDE.

Configure Build: Ensure the project's dependencies and JDK are configured correctly.

Compile: Build the project using your IDE's build tools.

Prepare Configuration Files:

Create Common.cfg for general configuration.
Create PeerInfo.cfg for peer information.


How to Run
Run all the classes using :

    javac <class.java>

To start a peer, use:

    java Peer <peerID>

Where <peerID> is the unique identifier for the peer.

Example:
**java Peer 1001**


**Detailed Workflow**
1. Initialization Phase
Each peer initializes its components (ConfigManager, FileManager, ConnectionManager).
The peer starts the server to listen for connections from other peers.
2. Handshake and Bitfield Exchange
Peers initiate handshake messages when connecting.
Each peer exchanges its bitfield to communicate which pieces of the file it already has.
3. Choking and Interest Management
The ChokingManager selects preferred peers based on download speed.
If a peer is interested in a piece, it sends an "INTERESTED" message; otherwise, a "NOT INTERESTED" message is sent.
4. File Request and Transfer
If unchoked, a peer requests a piece of the file.
FileManager manages file operations such as reading pieces from disk and writing received data.
5. Logging
All activities, including connections, downloads, and choking events, are logged using the Logger class.

**Logging Specifications**
Connections: Logs whenever a peer makes or receives a connection.
Choking: Logs when a peer is choked or unchoked by another peer.
Downloads: Logs each piece downloaded, including the source peer.
Completion: Logs when the entire file is successfully downloaded.
Important Notes
Synchronization: Proper synchronization is implemented where necessary to avoid race conditions between threads.
Choking Algorithm: The choking algorithm ensures bandwidth is distributed efficiently among interested peers.
Logging: The Logger class is utilized extensively to ensure all key events are captured, aiding debugging.

**Example Log Entry**
Peer 1001 makes a connection to Peer 1002.
Peer 1001 is unchoked by Peer 1002.
Peer 1001 has downloaded the piece 4 from Peer 1002.
Peer 1001 has downloaded the complete file.


**Future Improvements**
Simple BitTorrent applicaiton: can only support downloads of one Peer at a time and also cannot send multiple pieces to multiple peers at a time.
File Integrity Verification: Implement hash checks to ensure file pieces are correctly transferred.
# Bittorrent_Application
