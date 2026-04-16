# 🔐 Secure File Transfer System

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![TCP](https://img.shields.io/badge/Protocol-TCP-blue?style=for-the-badge)
![Security](https://img.shields.io/badge/Security-AES%20%7C%20SHA--256-green?style=for-the-badge)
![Multithreading](https://img.shields.io/badge/Feature-Multithreading-orange?style=for-the-badge)

A Java-based client-server application for secure and reliable file transfer over TCP. It uses AES encryption for credential protection, SHA-256 hashing for integrity validation, and supports multiple concurrent clients through multithreading.

---

## 📂 Project Structure

```
SecureFileTransfer/
├── server/
│   ├── Server.java
│   ├── ClientHandler.java
│   ├── AuthService.java
│   ├── FileService.java
│   ├── CryptoUtil.java
│   ├── users.txt
│   └── server_files/
└── client/
    ├── Client.java
    └── CryptoUtil.java
```

---

## ⚙️ Prerequisites

- Java JDK 8 or later

Verify installation:
```bash
java -version
javac -version
```

## 🔑 Default Credentials

- Username: `meet` / Password: `password123`
- Username: `sanskar` / Password: `securepass`
- Username: `admin` / Password: `admin2024`

> Do not use these credentials in production. Replace them with secure user accounts before deploying.

## 🚀 How to Run

1. Compile the server and client sources:
```bash
cd SecureFileTransfer/server
javac *.java
cd SecureFileTransfer/client
javac *.java
```

2. Start the server:
```bash
cd SecureFileTransfer/server
java Server
```

3. Start the client:
```bash
cd SecureFileTransfer/client
java Client
```

## 🧪 Multi-Client Testing

Open multiple terminals and run:
```bash
java Client
```

Each instance connects to the server simultaneously.

## 🌐 Run on Different Machines

1. Discover server IP:
```bash
# Windows
ipconfig

# Linux / macOS
ifconfig
ip addr
```
2. Update `SERVER_HOST` in `client/Client.java` with the server IP.
3. Recompile the client:
```bash
cd SecureFileTransfer/client
javac *.java
```

## ➕ Add New Users

Generate a SHA-256 password hash:
```bash
python3 -c "import hashlib; print(hashlib.sha256(b'password').hexdigest())"
```

Add new credentials to `server/users.txt` using the format:
```
username:hashed_password
```

## 🔒 Security Features

- AES-128 encryption for credentials
- SHA-256 hashing for file integrity
- No plaintext password storage
- Encrypted client-server credential exchange
- Multithreaded client handling using threads or `ExecutorService`

## 🛠️ Tech Stack

- Java
- TCP Socket Programming
- AES Encryption
- SHA-256 Hashing
- Multithreading

## 🚧 Debugging Tips

- Ensure the server is running before launching the client
- Verify the correct IP address and port configuration
- Confirm user credentials exist in `server/users.txt`
- Ensure Java is installed and available in `PATH`

## 👨‍💻 Authors

- Meet Yemde
- Team Members
