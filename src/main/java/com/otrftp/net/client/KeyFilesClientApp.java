package com.otrftp.net.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.otrftp.common.OtrConfig;
import com.otrftp.common.OtrCrypto;

public class KeyFilesClientApp implements Runnable
{
	private static final String EXIT = "exit";

	private static final String LIST = "list";

	private static final Logger log = LoggerFactory.getLogger(KeyFilesClientApp.class);

	private String serverAddress;
	private Socket socket;

	private Path filePath;
	private int port;

	private Thread clientThread;

	private boolean succeeded = false;

	public static void main(String[] args) throws Exception {

		if(args.length != 2) {
			printUsage();
		}

		KeyFilesClientApp sender;

		if(args[0].equalsIgnoreCase("--connect")) {
			try {
				sender = new KeyFilesClientApp(args[0], Integer.parseInt(args[1]));
				sender.connect();
				sender.start();
				sender.join();
			} catch (Exception e) {
				printUsage();
			}
		}
		else {
			printUsage();
		}


	}

	private static void printUsage() {
		System.err.println("Usage: java KeyFilesClientApp --connect server_address server_port");
	}

	public KeyFilesClientApp(String serverAddress, int port) {
		this.serverAddress = serverAddress;
		this.port = port;
	}

	public void connect() throws IOException
	{
		//InetSocketAddress sockAddr = new InetSocketAddress(InetAddress.getByName(serverAddress), OtrConfig.PORT);
		InetSocketAddress sockAddr = new InetSocketAddress(InetAddress.getByName(serverAddress), port);

		socket = new Socket();
		socket.connect(sockAddr, OtrCrypto.CLIENT_CONNECT_TIMEOUT);
	}

	public void start() {
		clientThread = new Thread(this, "OtrFTP client thread");
		clientThread.start();
	}

	public void join() throws InterruptedException {
		clientThread.join();
	}

	@Override
	public void run() {

		try (OutputStream os = socket.getOutputStream();
				InputStream is = socket.getInputStream()) {
			
			
			boolean repeat = true;
			while (repeat) {
				String command = getCommandFromUser();
				command = command.toLowerCase();
				switch (command) {
				case LIST:
					list(command, os, is);
					break;
				
				case EXIT:
					repeat = false;
					break;
					
				default:
					break;
				}
				//assert filePath != null;
			}
			// TODO: send file here!
			//succeeded = true;
		} catch(Throwable e) {
			log.error("Unhandled exception: ", e);
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				log.error("Closing connection failed: " + e.getMessage());
			}
		}
	}

	private void list(String command, OutputStream os, InputStream is) throws IOException {
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
		pw.println(command);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		System.out.println(br.readLine());
	}

	private String getCommandFromUser() {
		System.out.print("Command: ");

		Scanner scanner = new Scanner(System.in);
		String command = scanner.nextLine();
		scanner.close();

		return command;
	}

	public boolean succeeded() {
		return succeeded;
	}
}