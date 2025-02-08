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

	Map<String, String> cache;

	ServerSocket proxySocket;

	String logFileName = "proxy.log";

	public static void main(String[] args) {
		int port;
		try {
			port = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			port = 8080;
		}
		new ProxyServer().startServer(port);
	}

	void startServer(int proxyPort) {

		cache = new ConcurrentHashMap<>(Integer.MAX_VALUE);

		File cacheDir = new File("cached");
		if (!cacheDir.exists() || (cacheDir.exists() && !cacheDir.isDirectory())) {
			cacheDir.mkdirs();
		}

		try {
			proxySocket = new ServerSocket(proxyPort);
			System.out.println("Proxy server started on port " + proxyPort);
			writeLog("Proxy server started on port " + proxyPort);

			while (true) {
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