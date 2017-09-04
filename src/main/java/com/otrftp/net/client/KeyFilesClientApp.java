package com.otrftp.net.client;

import static com.otrftp.common.IOUtils.*;
import java.io.BufferedReader;
import java.io.File;
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
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.util.Scanner;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.otrftp.common.OtrCrypto;

public class KeyFilesClientApp implements Runnable
{

    private static final String EXIT = "exit";
    private static final String LIST = "list";
    private static final String SENDFILE = "send-file";
    private static final String HELPSENDFILE = "help-send-file";

    private static final Logger log = LoggerFactory.getLogger(KeyFilesClientApp.class);

    private String user;
    private int clientServerPort;
    private Socket serverSocket;
    //private Socket clientServerSocket;
    private Scanner scanner = new Scanner(System.in);

    private boolean succeeded = false;

    public static void main(String[] args) throws Exception {

        if(args.length != 7) {
            printUsage();
            return;
        }


        String serverAddress = null;
        int serverPort = -1;
        String user = null;
        int clientServerPort = -1;
        for (int i=0; i < args.length; i++) {
            switch (args[i]) {
            case "--connect":
                serverAddress = args[++i];
                serverPort = Integer.parseInt(args[++i]);
                break;

            case "--username":
                user = args[++i];
                break;

            case "--client-server-port":
                clientServerPort = Integer.parseInt(args[++i]);
                break;

            default:
                printUsage();
            }
        }

        if(serverAddress == null || serverPort < 0 || user == null || clientServerPort < 0) {
            printUsage();
            return;
        }	

        try {
            // launch the client thread
            Socket firstClientSocket = connect(serverAddress, serverPort);
            Thread clientThread = new Thread(new KeyFilesClientApp(firstClientSocket, user, clientServerPort), "KeyFiles client thread");
            clientThread.start();
           
            // launch second client connection for receiving files from server
            Socket secondClientSocket = connect(serverAddress, serverPort);
            Thread secondConnection = new Thread(new KeyFilesSecondConnection(secondClientSocket, user, clientServerPort), "Second connection thread");
            secondConnection.start();
            
            
            // launch the server thread of this client for receiving files
            Thread clientServer = new Thread(new KeyFilesClientServer(clientServerPort), "Client server thread");
            clientServer.start();
            
            clientThread.join();
            secondConnection.join();
            clientServer.join();
            
        } catch (Exception e) {
            log.error("Error starting ClientApp", e);
            return;
        }


    }

    public KeyFilesClientApp(Socket socket, String user, int clientServerPort) throws IOException {
        this.user = user;
        this.clientServerPort = clientServerPort;
        
        serverSocket = socket;
   }

    private static void printUsage() {
        System.err.println("java KeyFilesClientApp --connect server_address server_port --username john  --client-server-port client_server_port");
    }


    public static Socket connect(String serverAddress, int serverPort) throws IOException {
        InetSocketAddress sockAddr = new InetSocketAddress(InetAddress.getByName(serverAddress), serverPort);
        
        Socket socket = new Socket();
        socket.connect(sockAddr, OtrCrypto.CLIENT_CONNECT_TIMEOUT);
        
        return socket;
    }

    @Override
    public void run() {
        
        // login to Keybase
        try {
            KeybaseCommandLine.KeybaseLogin(user);
        }
        catch(Throwable e) {
            log.error("Unable to login to Keybase");
        }

        try (OutputStream os = serverSocket.getOutputStream();
                InputStream is = serverSocket.getInputStream()) {

            appearOnline(os, is);

            boolean repeat = true;
            while (repeat) {
                String command = getCommandFromUser();
                command = command.toLowerCase();
                switch (command) {
                case LIST:
                    list(os, is);
                    break;

                case EXIT:
                    repeat = false;
                    break;

                case SENDFILE:
                    sendFile(os, is);
                    break;

                case HELPSENDFILE:
                    helpSendFile(os, is);
                    break;

                default:
                    break;
                }
            }

            //assert filePath != null;
            //succeeded = true;
        } catch(Throwable e) {
            log.error("Unhandled exception: ", e);
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.error("Closing  failed: " + e.getMessage());
            }
        }
    }

    private void helpSendFile(OutputStream os, InputStream is) throws IOException {

        System.out.print("Enter the receiver's username: ");
        String receiverName = scanner.nextLine();
        
        System.out.print("Enter file name: ");
        String fileName = scanner.nextLine();
        
        File file = new File(fileName);
        
        if(!file.exists()) {
            log.error("A file with this name does not exist");
            return;
        }
        
        long fileSize = file.length();

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
        pw.println(HELPSENDFILE);

        pw.println(receiverName);
        pw.println(fileName);
        pw.println(fileSize);
        pw.flush();
        
        FileInputStream fis = new FileInputStream(fileName);
        copyStream(fis, os, fileSize);
        
        fis.close();

        
    }

    public Socket connectToReceiver(String address, int port) throws IOException {
        InetSocketAddress sockAddr = new InetSocketAddress(InetAddress.getByName(address), port);
        Socket receiverSocket = new Socket();
        receiverSocket.connect(sockAddr, OtrCrypto.CLIENT_CONNECT_TIMEOUT);	
        return receiverSocket;
    }

    public void sendFile(OutputStream os, InputStream is) throws IOException {
        String receiverName;
        String fileName;
        String receiverAddress;
        int receiverPort;

        System.out.print("Enter file name: ");
        fileName = scanner.nextLine();

        System.out.print("Enter the receiver's username: "); 
        receiverName = scanner.next();

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
        pw.println(SENDFILE);

        pw.println(receiverName);
        pw.flush();

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        receiverAddress = br.readLine(); 
        receiverPort = Integer.parseInt(br.readLine());

        if(receiverPort == -1) {
            log.error("User " + receiverName + " not online");
            return;
        }

        // connect to receiver's server here, implemented in KeyFilesClient 
        Socket socket;
        try {
            socket = connectToReceiver(receiverAddress, receiverPort);
        } catch (SocketTimeoutException e) {
            log.error("Could not connect to receiver's server");
            return;
        } 


        OutputStream output = socket.getOutputStream();
        //PrintWriter clientpw = new PrintWriter(new OutputStreamWriter(output));
        //clientpw.println(fileName);
        //clientpw.flush();

        output.write(fileName.getBytes());
        output.write('\n');
        output.flush();
        
        String encryptedMsg;
//        File file = new File(fileName);
        try {
            encryptedMsg = KeybaseCommandLine.encrypt(fileName, receiverName);
            output.write(encryptedMsg.getBytes());
        }
        catch (Exception e){
            log.error("Could not read bytes");
        }
        
        
        output.close();
        
        
//        FileInputStream fis = new FileInputStream(fileName);
//        byte[] buffer = new byte[4096];
//
//        int bytesRead = 0;
//        int totalSent = 0;
//        while ((bytesRead = fis.read(buffer)) > 0) {
//            totalSent += bytesRead;
//            output.write(buffer, 0, bytesRead);
//        }
//
//        fis.close();
//        output.close();	
//
//        System.out.println("client: " + totalSent);
    }

    private void appearOnline(OutputStream os, InputStream is) throws IOException {
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
        pw.println(user);
        pw.println(clientServerPort);
        pw.flush();
    }

    private void list(OutputStream os, InputStream is) throws IOException {
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
        pw.println(LIST);
        pw.flush();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        System.out.println(br.readLine());
    }

    private String getCommandFromUser() {
        System.out.print("Command: ");
        String command = scanner.nextLine();
        return command;
    }

    public boolean succeeded() {
        return succeeded;
    }
}
