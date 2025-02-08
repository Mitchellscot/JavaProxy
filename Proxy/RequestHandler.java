package Proxy;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Map;

// RequestHandler is a thread that processes requests of one client connection
public class RequestHandler extends Thread {

	Socket clientSocket;
	InputStream inFromClient;
	OutputStream outToClient;
	byte[] request = new byte[1024];
	private Map<String, String> cache;
	private ProxyServer server;

	public RequestHandler(Socket clientSocket, Map<String, String> cache, ProxyServer server) {
		this.clientSocket = clientSocket;
		this.cache = cache;
		this.server = server;

		try {
			clientSocket.setSoTimeout(5000); // Increase timeout to 5 seconds
			inFromClient = clientSocket.getInputStream();
			outToClient = clientSocket.getOutputStream();
		} catch (Exception e) {
			e.printStackTrace();
			server.writeLog("Error initializing client socket: " + e.getMessage());
		}
	}

	@Override
	public void run() {
		try {
			int bytesRead = inFromClient.read(request);
			if (bytesRead == -1) {
				return;
			}

			String requestString = new String(request, 0, bytesRead);
			if (!requestString.startsWith("GET")) {
				return; // Only process GET requests
			}

			String[] requestParts = requestString.split(" ");
			if (requestParts.length < 2) {
				return;
			}

			String url = requestParts[1];
			String clientIP = clientSocket.getInetAddress().getHostAddress();
			server.writeLog("Received GET request for URL: " + url + " from " + clientIP);

			// Parse headers
			String[] lines = requestString.split("\r\n");
			boolean connectionKeepAlive = false;
			for (String line : lines) {
				if (line.toLowerCase().startsWith("connection: keep-alive")) {
					connectionKeepAlive = true;
					break;
				}
			}

			String cachedFileName = cache.get(url);
			if (cachedFileName != null) {
				server.writeLog("Cache hit for URL: " + url);
				sendCachedInfoToClient(cachedFileName, connectionKeepAlive);
			} else {
				server.writeLog("Cache miss for URL: " + url);
				proxyServerToClient(url, requestString.getBytes(), connectionKeepAlive);
			}
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
			server.writeLog("Socket timeout: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			server.writeLog("Error processing client request: " + e.getMessage());
		} finally {
			try {
				if (clientSocket != null && clientSocket.isClosed()) {
					clientSocket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
				server.writeLog("Error closing client socket: " + e.getMessage());
			}
		}
	}

	private void proxyServerToClient(String url, byte[] clientRequest, boolean connectionKeepAlive) {
		FileOutputStream fileWriter = null;
		Socket toWebServerSocket = null;
		InputStream inFromServer;
		OutputStream outToServer;

		String fileName = "cached/" + generateRandomFileName() + ".dat";
		byte[] serverReply = new byte[4096];

		try {
			server.writeLog("Connecting to web server: " + url);
			URL urlObj = new URL(url);
			toWebServerSocket = new Socket(urlObj.getHost(), 80);
			outToServer = toWebServerSocket.getOutputStream();
			inFromServer = toWebServerSocket.getInputStream();

			server.writeLog("Sending request to web server: " + url);
			outToServer.write(clientRequest);
			outToServer.flush();

			cache.put(url, fileName);
			server.writeLog("Cached content for URL: " + url + " to file: " + fileName);

			fileWriter = new FileOutputStream(fileName);
			int bytesRead;
			while ((bytesRead = inFromServer.read(serverReply)) != -1) {
				outToClient.write(serverReply, 0, bytesRead);
				fileWriter.write(serverReply, 0, bytesRead);
			}

		} catch (IOException e) {
			e.printStackTrace();
			server.writeLog("Error communicating with web server for URL: " + url + ": " + e.getMessage());
		} finally {
			try {
				if (fileWriter != null) {
					fileWriter.close();
				}
				if (toWebServerSocket != null && toWebServerSocket.isClosed()) {
					toWebServerSocket.close();
				}
				if (clientSocket != null && !connectionKeepAlive && clientSocket.isClosed()) {
					clientSocket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
				server.writeLog("Error closing resources: " + e.getMessage());
			}
		}
	}

	private void sendCachedInfoToClient(String fileName, boolean connectionKeepAlive) {
		try {
			server.writeLog("Sending cached content from file: " + fileName);
			byte[] bytes = Files.readAllBytes(Paths.get(fileName));
			outToClient.write(bytes);
			outToClient.flush();
		} catch (Exception e) {
			e.printStackTrace();
			server.writeLog("Error sending cached content to client: " + e.getMessage());
		} finally {
			try {
				if (clientSocket != null && !connectionKeepAlive && clientSocket.isClosed()) {
					clientSocket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
				server.writeLog("Error closing client socket: " + e.getMessage());
			}
		}
	}

	public String generateRandomFileName() {
		String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
		SecureRandom RANDOM = new SecureRandom();
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < 10; ++i) {
			sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}
}