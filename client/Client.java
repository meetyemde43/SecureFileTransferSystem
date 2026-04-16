import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Scanner;

public class Client {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9090;
    private static final int CHUNK_SIZE = 4096;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    public void connect() throws IOException {
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        System.out.println("[Client] Connected to server at " + SERVER_HOST + ":" + SERVER_PORT);
    }

    public boolean authenticate(String username, String password) throws Exception {
        String encryptedUser = CryptoUtil.encrypt(username);
        String encryptedPass = CryptoUtil.encrypt(password);

        dos.writeUTF(encryptedUser);
        dos.writeUTF(encryptedPass);
        dos.flush();

        String response = dis.readUTF();
        return "AUTH_OK".equals(response);
    }

    public void uploadFile(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("[Client] File not found: " + filePath);
            return;
        }

        byte[] fileData = readFile(file);
        String fileHash = computeHash(fileData);

        dos.writeUTF("UPLOAD");
        dos.flush();

        String encryptedFilename = CryptoUtil.encrypt(file.getName());
        dos.writeUTF(encryptedFilename);
        dos.writeLong(fileData.length);
        dos.writeUTF(fileHash);
        dos.flush();

        System.out.println("[Client] Uploading: " + file.getName() + " (" + fileData.length + " bytes)");
        System.out.println("[Client] Local SHA-256: " + fileHash);

        int offset = 0;
        int chunkNumber = 0;
        while (offset < fileData.length) {
            int length = Math.min(CHUNK_SIZE, fileData.length - offset);
            byte[] chunk = new byte[length];
            System.arraycopy(fileData, offset, chunk, 0, length);
            byte[] encryptedChunk = CryptoUtil.encryptBytes(chunk);
            dos.writeInt(encryptedChunk.length);
            dos.write(encryptedChunk);
            dos.flush();
            offset += length;
            chunkNumber++;
            System.out.print("\r[Client] Sent chunk " + chunkNumber + " | " + offset + "/" + fileData.length + " bytes");
        }
        System.out.println();

        String response = dis.readUTF();
        if ("UPLOAD_OK".equals(response)) {
            System.out.println("[Client] Upload successful. Integrity verified by server.");
        } else if ("INTEGRITY_FAIL".equals(response)) {
            System.out.println("[Client] UPLOAD FAILED: Integrity check failed on server.");
        } else {
            System.out.println("[Client] Unexpected server response: " + response);
        }
    }

    public void downloadFile(String filename, String savePath) throws Exception {
        dos.writeUTF("DOWNLOAD");
        dos.flush();

        String encryptedFilename = CryptoUtil.encrypt(filename);
        dos.writeUTF(encryptedFilename);
        dos.flush();

        String response = dis.readUTF();
        if ("FILE_NOT_FOUND".equals(response)) {
            System.out.println("[Client] File not found on server: " + filename);
            return;
        }

        if (!"DOWNLOAD_READY".equals(response)) {
            System.out.println("[Client] Unexpected response: " + response);
            return;
        }

        long totalSize = dis.readLong();
        String serverHash = dis.readUTF();

        System.out.println("[Client] Downloading: " + filename + " (" + totalSize + " bytes)");

        ByteArrayOutputStream fileBuffer = new ByteArrayOutputStream();
        long bytesReceived = 0;

        while (bytesReceived < totalSize) {
            int chunkLen = dis.readInt();
            byte[] encryptedChunk = new byte[chunkLen];
            dis.readFully(encryptedChunk);
            byte[] chunk = CryptoUtil.decryptBytes(encryptedChunk);
            fileBuffer.write(chunk);
            bytesReceived += chunk.length;
            System.out.print("\r[Client] Received: " + bytesReceived + "/" + totalSize + " bytes");
        }
        System.out.println();

        byte[] fileData = fileBuffer.toByteArray();
        String localHash = computeHash(fileData);

        System.out.println("[Client] Server SHA-256: " + serverHash);
        System.out.println("[Client] Local  SHA-256: " + localHash);

        if (!localHash.equals(serverHash)) {
            System.out.println("[Client] INTEGRITY CHECK FAILED. File may be corrupted.");
            return;
        }

        File outFile = new File(savePath, filename);
        FileOutputStream fos = new FileOutputStream(outFile);
        fos.write(fileData);
        fos.flush();
        fos.close();
        System.out.println("[Client] File saved to: " + outFile.getAbsolutePath());
        System.out.println("[Client] Integrity verified. Download complete.");
    }

    public void listFiles() throws Exception {
        dos.writeUTF("LIST");
        dos.flush();

        int count = dis.readInt();
        if (count == 0) {
            System.out.println("[Client] No files on server.");
            return;
        }
        System.out.println("[Client] Files on server (" + count + "):");
        for (int i = 0; i < count; i++) {
            System.out.println("  " + (i + 1) + ". " + dis.readUTF());
        }
    }

    public void quit() throws IOException {
        dos.writeUTF("QUIT");
        dos.flush();
        String response = dis.readUTF();
        System.out.println("[Client] Server says: " + response);
    }

    public void disconnect() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("[Client] Disconnected.");
        } catch (IOException e) {
            System.out.println("[Client] Disconnect error: " + e.getMessage());
        }
    }

    private byte[] readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[CHUNK_SIZE];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        fis.close();
        return baos.toByteArray();
    }

    private String computeHash(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(data);
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Client client = new Client();

        try {
            client.connect();

            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            boolean authenticated = client.authenticate(username, password);
            if (!authenticated) {
                System.out.println("[Client] Authentication failed. Exiting.");
                client.disconnect();
                return;
            }
            System.out.println("[Client] Authentication successful. Welcome, " + username + "!");

            while (true) {
                System.out.println("\n--- Secure File Transfer Menu ---");
                System.out.println("1. Upload file");
                System.out.println("2. Download file");
                System.out.println("3. List files");
                System.out.println("4. Quit");
                System.out.print("Choice: ");
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        System.out.print("Enter full path of file to upload: ");
                        String uploadPath = scanner.nextLine().trim();
                        client.uploadFile(uploadPath);
                        break;
                    case "2":
                        System.out.print("Enter filename to download: ");
                        String dlFilename = scanner.nextLine().trim();
                        System.out.print("Enter directory to save file (leave blank for current dir): ");
                        String savePath = scanner.nextLine().trim();
                        if (savePath.isEmpty()) savePath = ".";
                        client.downloadFile(dlFilename, savePath);
                        break;
                    case "3":
                        client.listFiles();
                        break;
                    case "4":
                        client.quit();
                        client.disconnect();
                        return;
                    default:
                        System.out.println("[Client] Invalid choice.");
                }
            }

        } catch (Exception e) {
            System.out.println("[Client] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.disconnect();
            scanner.close();
        }
    }
}
