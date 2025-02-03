import java.io.*;

public class FileManager {

    // Create a folder for the peer to store pieces
    public static void createPeerFolder(int peerID) {
        String folderName = "peer_" + peerID;
        File folder = new File(folderName);
        if (!folder.exists()) {
            if (folder.mkdir()) {
                System.out.println("Folder created for Peer " + peerID + ": " + folderName);
            } else {
                System.err.println("Failed to create folder for Peer " + peerID);
            }
        }
    }

    public static void splitFileIntoPieces(File file, int pieceSize, int peerID) throws IOException {
        String folderName = "peer_" + peerID;

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[pieceSize];
            int partNumber = 0;

            while (true) {
                int bytesRead = bis.read(buffer);
                if (bytesRead == -1) break;

                File piece = new File(folderName + File.separator + "piece_" + partNumber);
                try (FileOutputStream fos = new FileOutputStream(piece)) {
                    fos.write(buffer, 0, bytesRead);
                }

                //System.out.println("Created piece_" + partNumber + " for Peer " + peerID);
                partNumber++;
            }
            System.out.println("File segmented for Peer " + peerID);
        }
    }




    public byte[] getFullFileData(int peerID) {
        // Enter File name here
        String fileName = "D:\\Grad Sem Material\\Sem 1\\CN - CNT5106C\\Project\\development\\BitTorrent-Application---Java\\thefile";

        File file = new File(fileName);

        if (!file.exists()) {
            System.err.println("Error: The specified file does not exist - " + file.getAbsolutePath());
            return null; // Exit early if the file doesn't exist
        }

        byte[] fileData = new byte[(int) file.length()];

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            bis.read(fileData);
            System.out.println("File read successfully: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error reading full file data: " + e.getMessage());
            return null;
        }

        return fileData;
    }


    public static void writeFullFile(int peerID, byte[] fileData) {
        String folderName = "peer_" + peerID; // Each peer's data in a separate folder
        String fileName = folderName + File.separator + "received_file"; // The file name for the received file

        createPeerFolder(peerID);

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileName))) {
            bos.write(fileData);
            System.out.println("Full file written to disk by Peer " + peerID);
        } catch (IOException e) {
            System.err.println("Error writing full file to disk for Peer " + peerID + ": " + e.getMessage());
        }
    }
}
