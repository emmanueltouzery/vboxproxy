package com.github.emmanueltouzery.vboxproxyhost;

import java.io.*;
import java.util.function.*;
import javaslang.control.*;
import java.net.*;

public class App {

    private static final String GUEST_ID = "bb74cf65-d9af-40a6-804b-44162d8795dd";
    private static final String GUEST_USERNAME = "Mitja Resman";
    private static final String GUEST_JAVA_PATH = "c:\\Program Files\\Java\\jdk1.7.0_40\\bin\\java.exe";
    private static final String GUEST_APP_PATH = "e:";
    private static final int PORT = 2222;

    public static void main(String[] args) throws Exception {
        // pazi convert the byte[] to string.
        Socket clientSocket = openServerSocket(PORT);
        InputStream clientIs = clientSocket.getInputStream();
        Consumer<ByteArray> msgProcessor = bytes ->
            Try.run(() -> sendMessage(GUEST_ID, "testkey", new String(bytes.bytes, "UTF-8")));
        Thread readerThread = new Thread(
            () -> Try.run(() -> streamHandleAsAvailable(clientIs, msgProcessor, StreamReadMode.Socket))
            .orElseRun(Throwable::printStackTrace));
        readerThread.start();
        Consumer<ByteArray> toClientWriter = data ->
            Try.run(() -> clientSocket.getOutputStream().write(data.bytes));

        System.out.println("before receiver thread");
        Thread receiverThread = new Thread(
            () -> Try.run(() -> runGuestApp(GUEST_ID, GUEST_USERNAME, GUEST_APP_PATH, toClientWriter)));
        receiverThread.start();
    }

    /**
     * @param msgProcessor a consumer that'll be called with incoming data
     *                     from the client socket.
     * @return a consumer allowing you to write to the
     *         client socket.
     */
    private static Socket openServerSocket(int port) throws IOException {
        // TODO try-with-resources?
        ServerSocket serverSocket = new ServerSocket(port);
        return serverSocket.accept();
    }

    private static void sendMessage(String guestId, String key, String value) throws IOException {
        System.out.println("Sending to guest => " + value);
        ProcessBuilder proc = new ProcessBuilder(
            "VBoxManage", "guestproperty", "set", guestId, key, value);
        Process p = proc.start();
    }

    private static class ByteArray {
        public final byte[] bytes;

        public ByteArray(byte[] data) {
            bytes = data;
        }
    }

    private static void runGuestApp(String guestId, String guestUser, String guestAppPath,
                                    Consumer<ByteArray> handler) throws Exception {
        ProcessBuilder proc = new ProcessBuilder(
            "VBoxManage", "guestcontrol", "--username", guestUser, guestId, "run",
            "--exe", GUEST_JAVA_PATH, "--wait-stdout", "--", "java", "-cp", guestAppPath, "Main");
        Process p = proc.start();
        InputStream stream = p.getInputStream();
        streamHandleAsAvailable(stream, handler, StreamReadMode.Normal);
    }

    private enum StreamReadMode {
        Socket,
        Normal
    }

    /**
     * WILL block the current thread!!!
     */
    private static void streamHandleAsAvailable(InputStream stream,
                                                Consumer<ByteArray> handler,
                                                StreamReadMode readMode) throws Exception {
        System.out.println("streamHandleAsAvailable");
        while (true) {
            byte[] read = streamReadAvailable(stream, readMode);
            if (read.length > 0) {
                System.out.println("stream got sth => " + new String(read, "UTF-8"));
                handler.accept(new ByteArray(read));
            }
        }
    }

    private static byte[] streamReadAvailable(InputStream stream, StreamReadMode readMode) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        int byteCount = 0;
        // read bytes from stream, and store them in buffer
        while ((byteCount = stream.read(buffer)) > 0) {
            // Writes bytes from byte array (buffer) into output stream.
            os.write(buffer, 0, byteCount);
            if (readMode == StreamReadMode.Socket && stream.available() == 0) {
                // we would be blocking otherwise. let's process
                // what we got now immediately.
                break;
            }
        }
        return os.toByteArray();
    }

}
