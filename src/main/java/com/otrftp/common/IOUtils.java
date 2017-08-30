package com.otrftp.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is a utility class for handling i/o.
 */
public class IOUtils {
    
    /**
     * Reads a line from provided input stream
     * 
     * @param is
     * @return a string with the line terminator removed
     * @throws IOException
     */
    public static String readLine(InputStream is) throws IOException {
        int read = 0;
        
        byte[] bytes = new byte[100];
        int index = 0;
        while((read = is.read()) != '\n') {
            bytes[index++] = (byte) read;
        }
        
        int length = index;
        if (bytes[index-1] == '\r') {
            length--;
        }
        
        
        return new String(bytes, 0, length);
    }
    
    /**
     * Copies specified number of bytes from the provided input stream to the provided output stream
     * 
     * @param is
     * @param os
     * @param totalBytes
     * @throws IOException
     */
    public static void copyStream(InputStream is, OutputStream os, long totalBytes) throws IOException {
        byte[] buffer = new byte[4096];

        long remainingBytes = totalBytes;
        int bytesRead = 0;
        while (remainingBytes > 0) {
            if(remainingBytes < buffer.length) {
                bytesRead = is.read(buffer, 0, (int) remainingBytes);
            }
            else {
                bytesRead = is.read(buffer, 0, buffer.length);
            }
            os.write(buffer, 0, bytesRead);
            remainingBytes -= bytesRead;
        }

    }
}
