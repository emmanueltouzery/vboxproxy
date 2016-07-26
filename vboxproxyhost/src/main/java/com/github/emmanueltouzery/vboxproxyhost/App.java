package com.github.emmanueltouzery.vboxproxyhost;

import java.io.*;
import java.util.Base64;
import java.util.function.*;
import java.util.concurrent.*;
import javaslang.control.*;
import javaslang.*;
import javaslang.collection.*;
import java.net.*;
import java.util.regex.*;

public class App {

    private static final String GUEST_ID = "bb74cf65-d9af-40a6-804b-44162d8795dd";
    private static final String GUEST_USERNAME = "Mitja Resman";
    private static final String GUEST_JAVA_PATH = "c:\\Program Files\\Java\\jdk1.7.0_40\\bin\\java.exe";
    private static final String GUEST_APP_PATH = "e:";
    private static final int PORT = 2222;
    private static final String SHARED_KEY = "testkey";

    private static ConcurrentLinkedQueue<ByteArray> pendingMessages = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) throws Exception {
        // pazi convert the byte[] to string.
        Socket clientSocket = openServerSocket(PORT);
        InputStream clientIs = clientSocket.getInputStream();
        Consumer<ByteArray> msgProcessor = bytes ->
            Try.run(() -> queueMessage(GUEST_ID, SHARED_KEY, bytes));
        Thread readerThread = new Thread(
            () -> Try.run(() -> streamHandleAsAvailable(clientIs, msgProcessor))
            .orElseRun(Throwable::printStackTrace));
        readerThread.start();
        Consumer<ByteArray> toClientWriter = data ->
            Try.run(() -> {
                    System.out.println("writing to downsocket: " + new String(data.bytes, "UTF-8"));
                    clientSocket.getOutputStream().write(data.bytes);
                });

        System.out.println("before receiver thread");
        Thread receiverThread = new Thread(
            () -> Try.run(() -> runGuestApp(GUEST_ID, GUEST_USERNAME, GUEST_APP_PATH, toClientWriter)));
        receiverThread.start();

        Thread sendingThread = new Thread(() -> messageSender(GUEST_ID, SHARED_KEY));
        sendingThread.start();
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

    private static void queueMessage(String guestId, String key, ByteArray value) throws IOException {
        pendingMessages.add(value);
    }

    private static void sendMessage(String guestId, String key, ByteArray value) throws IOException {
        System.out.println("Sending to guest => " + value.bytes);
        ProcessBuilder proc = new ProcessBuilder(
            "VBoxManage", "guestproperty", "set", guestId, key, Base64.getEncoder().encodeToString(value.bytes));
        Process p = proc.start();
    }

    private static void messageSender(String guestId, String key) {
        while (true) {
            try {
                while (!guestDidReadPreviousMessage(guestId, key)) {
                    Thread.sleep(10);
                }
                ByteArray msg = pendingMessages.peek();
                if (msg != null) {
                    sendMessage(guestId, key, msg);
                    pendingMessages.remove();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static boolean guestDidReadPreviousMessage(String guestId, String key) throws IOException {
        ProcessBuilder proc = new ProcessBuilder(
            "VBoxManage", "guestproperty", "enumerate", guestId);
        Process p = proc.start();
        InputStream stream = p.getInputStream();
        String output = new String(streamToByteArray(stream), "UTF-8");
        return !List.of(output.split("\n"))
            .map(App::parseEnumerateProp)
            .map(Tuple2::_1)
            .contains(key);
    }

    /*package*/ static Tuple2<String, String> parseEnumerateProp(String outputLine) {
        Pattern p = Pattern.compile("Name: (?<name>[^,]+), value: (?<value>[^,]*), timestamp: [^,]+, flags:.*$");
        Matcher m = p.matcher(outputLine);
        if (m.find()) {
            return Tuple.of(m.group("name"), m.group("value"));
        } else {
            throw new IllegalStateException("Can't parse line: " + outputLine);
        }
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
        streamHandleAsAvailable(stream, handler);
    }

    /**
     * WILL block the current thread!!!
     */
    private static void streamHandleAsAvailable(InputStream stream,
                                                Consumer<ByteArray> handler) throws Exception {
        System.out.println("streamHandleAsAvailable");
        while (true) {
            byte[] read = streamReadAvailable(stream);
            if (read.length > 0) {
                System.out.println("stream got sth => " + new String(read, "UTF-8"));
                handler.accept(new ByteArray(read));
            }
        }
    }

    private static byte[] streamReadAvailable(InputStream stream) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        int byteCount;
        // read bytes from stream, and store them in buffer
        while ((byteCount = stream.read(buffer)) > 0) {
            // Writes bytes from byte array (buffer) into output stream.
            os.write(buffer, 0, byteCount);
            if (stream.available() == 0) {
                // we would be blocking otherwise. let's process
                // what we got now immediately.
                break;
            }
        }
        return os.toByteArray();
    }

    // http://stackoverflow.com/a/30618794/516188
    // fix when java9 is released http://stackoverflow.com/a/37681322/516188
    private static byte[] streamToByteArray(InputStream stream) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        int byteCount;
        // read bytes from stream, and store them in buffer
        while ((byteCount = stream.read(buffer)) != -1) {
            // Writes bytes from byte array (buffer) into output stream.
            os.write(buffer, 0, byteCount);
        }
        stream.close();
        os.flush();
        os.close();
        return os.toByteArray();
    }
}
