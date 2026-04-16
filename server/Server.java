import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 9090;
    private static final int MAX_THREADS = 10;
    private static final String CREDENTIALS_FILE = "users.txt";
    private static final String STORAGE_DIR = "server_files";

    public static void main(String[] args) {
        AuthService authService;
        FileService fileService;

        try {
            authService = new AuthService(CREDENTIALS_FILE);
            fileService = new FileService(STORAGE_DIR);
        } catch (Exception e) {
            System.out.println("[Server] Initialization failed: " + e.getMessage());
            return;
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);

        System.out.println("[Server] Secure File Transfer Server started on port " + PORT);
        System.out.println("[Server] Storage directory: " + STORAGE_DIR);
        System.out.println("[Server] Max concurrent clients: " + MAX_THREADS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, authService, fileService);
                threadPool.submit(handler);
            }
        } catch (Exception e) {
            System.out.println("[Server] Fatal error: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }
}
