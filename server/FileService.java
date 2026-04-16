import java.io.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class FileService {

    private static final int CHUNK_SIZE = 4096;
    private final String storageDirectory;

    public FileService(String storageDirectory) {
        this.storageDirectory = storageDirectory;
        File dir = new File(storageDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public String computeFileHash(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(data);
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    public String computeFileHash(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[CHUNK_SIZE];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
        }
        fis.close();
        byte[] hash = md.digest();
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    public void saveFile(String filename, byte[] data) throws IOException {
        File outFile = new File(storageDirectory, filename);
        FileOutputStream fos = new FileOutputStream(outFile);
        fos.write(data);
        fos.flush();
        fos.close();
    }

    public byte[] loadFile(String filename) throws IOException {
        File file = new File(storageDirectory, filename);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filename);
        }
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

    public List<String> listFiles() {
        List<String> files = new ArrayList<>();
        File dir = new File(storageDirectory);
        File[] fileList = dir.listFiles();
        if (fileList != null) {
            for (File f : fileList) {
                if (f.isFile()) {
                    files.add(f.getName() + " (" + f.length() + " bytes)");
                }
            }
        }
        return files;
    }

    public boolean fileExists(String filename) {
        return new File(storageDirectory, filename).exists();
    }

    public int getChunkSize() {
        return CHUNK_SIZE;
    }
}
