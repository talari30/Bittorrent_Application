import java.util.Arrays;

public class BitfieldManager {

    private byte[] bitfield;
    private int totalPieces;
    private int pieceSize;
    private int fileSize;

    // Constructor to initialize the bitfield manager
    public BitfieldManager(int fileSize, int pieceSize) {
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
        this.totalPieces = (int) Math.ceil((double) fileSize / pieceSize); // Calculate total pieces

        this.bitfield = new byte[(int) Math.ceil((double) totalPieces / 8)];
    }

    public void setAllPieces() {
        Arrays.fill(this.bitfield, (byte) 1); // Set all bits to 1 to represent that all pieces are available
    }

    // Set the bit for a specific piece index (mark the piece as downloaded)
    public void setPiece(int pieceIndex) {
        if (pieceIndex >= 0 && pieceIndex < totalPieces) {
            int byteIndex = pieceIndex / 8;
            int bitPosition = pieceIndex % 8;

            bitfield[byteIndex] |= (1 << (7 - bitPosition));
        }
    }

    // Check if a specific piece is already downloaded (if the bit is set to 1)
    public boolean hasPiece(int pieceIndex) {
        if (pieceIndex >= 0 && pieceIndex < totalPieces) {
            int byteIndex = pieceIndex / 8;
            int bitPosition = pieceIndex % 8;

            return (bitfield[byteIndex] & (1 << (7 - bitPosition))) != 0;
        }
        return false;
    }

    // Get the current bitfield (used when sending the bitfield to other peers)
    public byte[] getBitfield() {
        return bitfield;
    }

    // Get the bitfield length (used to ensure the size is correct when exchanging bitfields)
    public int getBitfieldLength() {
        return bitfield.length;
    }

    // Update the peer's bitfield with a new bitfield received from another peer
    public void updatePeerBitfield(byte[] receivedBitfield) {
        // Ensure the received bitfield is the same length as the current one
        if (receivedBitfield.length == bitfield.length) {
            System.arraycopy(receivedBitfield, 0, bitfield, 0, bitfield.length);
        } else {
            System.err.println("Error: Received bitfield length mismatch.");
        }
    }

    // Get the total number of pieces the peer needs to download
    public int getTotalPieces() {
        return totalPieces;
    }

    // Check if the peer has downloaded the entire file (all bits are set to 1)
    public boolean hasCompleteFile() {
        for (int i = 0; i < totalPieces; i++) {
            if (!hasPiece(i)) {
                return false;
            }
        }
        return true;
    }

    // Debugging: Print the bitfield as a binary string
    public void printBitfield() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < totalPieces; i++) {
            sb.append(hasPiece(i) ? "1" : "0");
        }
        System.out.println("Bitfield: " + sb.toString());
    }

    public synchronized void markPieceAsReceived(int pieceIndex) {
        if (pieceIndex < 0 || pieceIndex >= totalPieces) {
            throw new IllegalArgumentException("Invalid piece index: " + pieceIndex);
        }

        int byteIndex = pieceIndex / 8;
        int bitPosition = pieceIndex % 8;

        bitfield[byteIndex] |= (1 << (7 - bitPosition)); // Set the bit at the corresponding position
        System.out.println("Piece " + pieceIndex + " marked as received in the bitfield.");
    }
}
