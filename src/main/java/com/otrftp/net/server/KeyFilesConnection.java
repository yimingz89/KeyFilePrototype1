package com.otrftp.net.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyFilesConnection implements Runnable {
	
	private static final String EXIT = "exit";
	private static final String LIST = "list";
    private static final Logger log = LoggerFactory.getLogger(KeyFilesConnection.class);


	private Socket socket;
	private BufferedReader br;
	private PrintWriter pw;
	
	public KeyFilesConnection(Socket socket) {
		this.socket = socket;
		try {
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
		} catch (IOException e) {
			log.error("Unhandled exception: ", e);
		}
		
	}

	public void run() {
		try {	
			boolean repeat = true;
			while (repeat) {
				String command = getCommandFromClient();
				command = command.toLowerCase();
				switch (command) {
				case LIST:
					list(command);
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

	private void list(String command) {
		pw.println(Arrays.toString(KeyFilesServerApp.usernames));
	}

	private String getCommandFromClient() throws IOException {
		return br.readLine();
	}
}
