================================================================
  SECURE FILE TRANSFER SYSTEM
  Java Socket Programming | TCP | AES | SHA-256 | Multithreading
================================================================

----------------------------------------------------------------
  PROJECT STRUCTURE
----------------------------------------------------------------

SecureFileTransfer/
├── server/
│   ├── Server.java          <- Main server entry point
│   ├── ClientHandler.java   <- Thread per connected client
│   ├── AuthService.java     <- Login + SHA-256 password check
│   ├── FileService.java     <- File I/O + SHA-256 integrity
│   ├── CryptoUtil.java      <- AES-CBC encrypt/decrypt
│   ├── users.txt            <- Hashed credential store
│   └── server_files/        <- Uploaded files land here (auto-created)
└── client/
    ├── Client.java          <- Interactive client (UPLOAD/DOWNLOAD/LIST)
    └── CryptoUtil.java      <- Shared crypto (same as server copy)

----------------------------------------------------------------
  PRE-REQUISITES
----------------------------------------------------------------

1. Java JDK 8 or later must be installed.

   Check with:
       java -version
       javac -version

   Install on Ubuntu/Debian:
       sudo apt install default-jdk

   Install on macOS (Homebrew):
       brew install openjdk

   Install on Windows:
       Download from https://adoptium.net and add to PATH.

2. Two terminal windows (or two machines on the same network).

----------------------------------------------------------------
  DEFAULT CREDENTIALS (users.txt)
----------------------------------------------------------------

  Username : alice     Password : password123
  Username : bob       Password : securepass
  Username : admin     Password : admin2024

  Passwords are stored as SHA-256 hashes — NEVER in plain text.

----------------------------------------------------------------
  STEP 1 — COMPILE
----------------------------------------------------------------

Open a terminal in the project root.

--- Compile Server ---
  cd SecureFileTransfer/server
  javac CryptoUtil.java AuthService.java FileService.java ClientHandler.java Server.java

--- Compile Client ---
  cd ../client
  javac CryptoUtil.java Client.java

  (Windows users: replace / with \ in paths)

----------------------------------------------------------------
  STEP 2 — RUN THE SERVER
----------------------------------------------------------------

  cd SecureFileTransfer/server
  java Server

  Expected output:
      [Server] Secure File Transfer Server started on port 9090
      [Server] Storage directory: server_files
      [Server] Max concurrent clients: 10

  The server listens on TCP port 9090.
  Keep this terminal open — it must stay running.

----------------------------------------------------------------
  STEP 3 — RUN THE CLIENT
----------------------------------------------------------------

Open a NEW terminal:

  cd SecureFileTransfer/client
  java Client

  Expected output:
      [Client] Connected to server at localhost:9090
      Username:

  Enter credentials, e.g.:
      Username: meet
      Password: password123

  Then use the menu:
      1 -> Upload a file   (enter full path, e.g. /home/user/doc.pdf)
      2 -> Download a file (enter filename as stored on server)
      3 -> List files      (shows all files on the server)
      4 -> Quit

----------------------------------------------------------------
  STEP 4 — RUN MULTIPLE CLIENTS (Testing Multithreading)
----------------------------------------------------------------

Open 2 or 3 additional terminals and run:

  cd SecureFileTransfer/client
  java Client

Each client session runs in its own thread on the server.
You can transfer different files simultaneously.

----------------------------------------------------------------
  RUNNING ON TWO DIFFERENT MACHINES (LAN)
----------------------------------------------------------------

1. Find the server machine's IP address:
       Linux/Mac:  ifconfig   or   ip addr
       Windows:    ipconfig

2. Edit client/Client.java, change line:
       private static final String SERVER_HOST = "localhost";
   to:
       private static final String SERVER_HOST = "192.168.x.x";  // server IP

3. Recompile the client:
       javac CryptoUtil.java Client.java

4. Make sure port 9090 is open on the server machine's firewall:
       Linux:    sudo ufw allow 9090
       Windows:  Allow inbound rule for port 9090 in Windows Firewall

----------------------------------------------------------------
  ADDING NEW USERS
----------------------------------------------------------------

Passwords must be stored as SHA-256 hashes. To generate a hash:

  Linux/Mac:
      echo -n "yourpassword" | sha256sum

  Python (any OS):
      python3 -c "import hashlib; print(hashlib.sha256(b'yourpassword').hexdigest())"

  Then add a line to users.txt:
      newuser:thehashyougenerated

  Restart the server after editing users.txt.

----------------------------------------------------------------
  CHANGING PORT OR SETTINGS
----------------------------------------------------------------

  Server port:       Edit Server.java        -> PORT = 9090
  Max clients:       Edit Server.java        -> MAX_THREADS = 10
  Chunk size:        Edit FileService.java   -> CHUNK_SIZE = 4096
  Storage folder:    Edit Server.java        -> STORAGE_DIR = "server_files"
  AES key:           Edit CryptoUtil.java    -> FIXED_KEY (must be exactly 16 bytes)
                     *** Update BOTH server and client copies ***

----------------------------------------------------------------
  DEBUGGING GUIDE
----------------------------------------------------------------

PROBLEM: "javac: command not found"
FIX:     JDK not installed or not in PATH.
         Install JDK and ensure JAVA_HOME/bin is in your PATH.

PROBLEM: "Credentials file not found: users.txt"
FIX:     You must run the server FROM the server/ directory.
         cd SecureFileTransfer/server
         java Server
         (Do NOT run as: java server/Server from the parent folder)

PROBLEM: "Connection refused" on client
FIX:     Server is not running, or wrong HOST/PORT.
         - Confirm server terminal shows "started on port 9090"
         - Check SERVER_HOST in Client.java matches actual server IP
         - Check firewall is not blocking port 9090

PROBLEM: "Authentication failed"
FIX:     Wrong username or password.
         - Check users.txt has the correct SHA-256 hash
         - Regenerate hash with: python3 -c "import hashlib; print(hashlib.sha256(b'PASSWORD').hexdigest())"
         - Make sure there are no trailing spaces in users.txt

PROBLEM: "INTEGRITY_FAIL" on upload
FIX:     File was corrupted in transit (very rare on localhost).
         - Try again; if persistent, check network stability.
         - Verify CHUNK_SIZE matches between client and server.

PROBLEM: Port 9090 already in use
FIX:     Another process is using the port.
         Linux/Mac:  lsof -i :9090   then   kill <PID>
         Windows:    netstat -ano | findstr :9090   then   taskkill /PID <PID> /F
         Or change PORT in Server.java to another value (e.g. 9191).

PROBLEM: "Class not found" or "NoClassDefFoundError"
FIX:     Compile all files together from the correct directory.
         Server: javac CryptoUtil.java AuthService.java FileService.java ClientHandler.java Server.java
         Client: javac CryptoUtil.java Client.java

PROBLEM: File uploaded but looks empty or wrong size
FIX:     Ensure you provided the FULL path when uploading.
         Example: /home/alice/documents/report.pdf
         Not just: report.pdf (unless you are in that directory)

----------------------------------------------------------------
  QUICK REFERENCE — TERMINAL COMMANDS
----------------------------------------------------------------

  # Compile server
  cd SecureFileTransfer/server && javac *.java

  # Compile client
  cd SecureFileTransfer/client && javac *.java

  # Run server
  cd SecureFileTransfer/server && java Server

  # Run client
  cd SecureFileTransfer/client && java Client

  # Check files uploaded to server
  ls SecureFileTransfer/server/server_files/

  # Generate SHA-256 hash for a new password
  python3 -c "import hashlib; print(hashlib.sha256(b'mypassword').hexdigest())"

----------------------------------------------------------------
  SECURITY NOTES
----------------------------------------------------------------

- AES-128 CBC mode with a random IV per encryption call.
- IV is prepended to ciphertext so it travels with the data.
- Credentials (username + password) are AES-encrypted on the wire.
- File chunks are each individually AES-encrypted before sending.
- Passwords are never stored or transmitted in plain text.
- SHA-256 hash is computed BEFORE encryption and verified AFTER
  decryption to confirm end-to-end file integrity.

================================================================
  END OF README
================================================================
