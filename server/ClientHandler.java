import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final AuthService authService;
    private final FileService fileService;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String authenticatedUser = null;

    public ClientHandler(Socket socket, AuthService authService, FileService fileService) {
        this.clientSocket = socket;
        this.authService = authService;
        this.fileService = fileService;
    }

    @Override
    public void run() {
        try {
            dis = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            dos = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));

            System.out.println("[Server] Client connected: " + clientSocket.getInetAddress());

            if (!handleAuthentication()) {
                sendResponse("AUTH_FAIL");
                cleanup();
                return;
            }

            sendResponse("AUTH_OK");
            System.out.println("[Server] Authenticated user: " + authenticatedUser);

            handleCommands();

        } catch (Exception e) {
            System.out.println("[Server] Client session error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private boolean handleAuthentication() throws Exception {
        String encryptedUsername = dis.readUTF();
        String encryptedPassword = dis.readUTF();

        String username = CryptoUtil.decrypt(encryptedUsername);
        String password = CryptoUtil.decrypt(encryptedPassword);

        if (authService.authenticate(username, password)) {
            authenticatedUser = username;
            return true;
        }
        return false;
    }

    private void handleCommands() throws Exception {
        while (true) {
            String command = dis.readUTF();
            System.out.println("[Server] Command from " + authenticatedUser + ": " + command);

            switch (command.toUpperCase()) {
                case "UPLOAD":
                    handleUpload();
                    break;
                case "DOWNLOAD":
                    handleDownload();
                    break;
                case "LIST":
                    handleList();
                    break;
                case "QUIT":
                    sendResponse("BYE");
                    return;
                default:
                    sendResponse("UNKNOWN_COMMAND");
            }
        }
    }

    private void handleUpload() throws Exception {
        String encryptedFilename = dis.readUTF();
        String filename = CryptoUtil.decrypt(encryptedFilename);

        long totalSize = dis.readLong();
        String clientHash = dis.readUTF();

        System.out.println("[Server] Receiving file: " + filename + " (" + totalSize + " bytes)");

        ByteArrayOutputStream fileBuffer = new ByteArrayOutputStream();
        long bytesReceived = 0;
        int chunkSize = fileService.getChunkSize();

        while (bytesReceived < totalSize) {
            int chunkLen = dis.readInt();
            byte[] encryptedChunk = new byte[chunkLen];
            dis.readFully(encryptedChunk);
            byte[] chunk = CryptoUtil.decryptBytes(encryptedChunk);
            fileBuffer.write(chunk);
            bytesReceived += chunk.length;
        }

        byte[] fileData = fileBuffer.toByteArray();
        String serverHash = fileService.computeFileHash(fileData);

        if (!serverHash.equals(clientHash)) {
            sendResponse("INTEGRITY_FAIL");
            System.out.println("[Server] Integrity check FAILED for: " + filename);
            return;
        }

        fileService.saveFile(filename, fileData);
        sendResponse("UPLOAD_OK");
        System.out.println("[Server] File saved successfully: " + filename);
    }

    private void handleDownload() throws Exception {
        String encryptedFilename = dis.readUTF();
        String filename = CryptoUtil.decrypt(encryptedFilename);

        if (!fileService.fileExists(filename)) {
            sendResponse("FILE_NOT_FOUND");
            return;
        }

        sendResponse("DOWNLOAD_READY");

        byte[] fileData = fileService.loadFile(filename);
        String fileHash = fileService.computeFileHash(fileData);

        dos.writeLong(fileData.length);
        dos.writeUTF(fileHash);
        dos.flush();

        int chunkSize = fileService.getChunkSize();
        int offset = 0;

        while (offset < fileData.length) {
            int length = Math.min(chunkSize, fileData.length - offset);
            byte[] chunk = new byte[length];
            System.arraycopy(fileData, offset, chunk, 0, length);
            byte[] encryptedChunk = CryptoUtil.encryptBytes(chunk);
            dos.writeInt(encryptedChunk.length);
            dos.write(encryptedChunk);
            dos.flush();
            offset += length;
        }

        System.out.println("[Server] File sent: " + filename);
    }

    private void handleList() throws Exception {
        List<String> files = fileService.listFiles();
        dos.writeInt(files.size());
        for (String entry : files) {
            dos.writeUTF(entry);
        }
        dos.flush();
    }

    private void sendResponse(String response) throws IOException {
        dos.writeUTF(response);
        dos.flush();
    }

    private void cleanup() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            System.out.println("[Server] Connection closed for: " +
                (authenticatedUser != null ? authenticatedUser : "unauthenticated client"));
        } catch (IOException e) {
            System.out.println("[Server] Cleanup error: " + e.getMessage());
        }
    }
}
