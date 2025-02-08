package Proxy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyServer {

	// Cache is a Map: the key is the URL and the value is the file name of the file
	// that stores the cached content
	Map<String, String> cache;

	ServerSocket proxySocket;

	String logFileName = "proxy.log";

	public static void main(String[] args) {
		new ProxyServer().startServer(Integer.parseInt(args[0]));
	}

	void startServer(int proxyPort) {

		cache = new ConcurrentHashMap<>();

		// Create the directory to store cached files.
		File cacheDir = new File("cached");
		if (!cacheDir.exists() || (cacheDir.exists() && !cacheDir.isDirectory())) {
			cacheDir.mkdirs();
		}

		try {
			// Create a serverSocket to listen on the port (proxyPort)
			proxySocket = new ServerSocket(proxyPort);
			System.out.println("Proxy server started on port " + proxyPort);
			writeLog("Proxy server started on port " + proxyPort);

			while (true) {
				// Create a thread (RequestHandler) for each new client connection
				Socket clientSocket = proxySocket.accept();
				writeLog("Accepted connection from " + clientSocket.getInetAddress().getHostAddress());
				Thread thread = new RequestHandler(clientSocket, this.cache, this);
				thread.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
			writeLog("Error starting server: " + e.getMessage());
		}
	}

	public synchronized void writeLog(String info) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName, true))) {
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
			String logEntry = timeStamp + " " + info;
			writer.write(logEntry);
			writer.newLine();
			System.out.println(logEntry);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}