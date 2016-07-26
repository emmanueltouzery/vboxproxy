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

import com.github.emmanueltouzery.vboxproxycommon.*;

public class App {

    private static final String GUEST_ID = "bb74cf65-d9af-40a6-804b-44162d8795dd";
    private static final String GUEST_USERNAME = "Mitja Resman";
    private static final String GUEST_JAVA_PATH = "C:\\Program Files\\Java\\jre1.8.0_102\\bin\\java.exe";
    private static final String GUEST_APP_PATH = "e:\\vboxproxyguest-1.0-SNAPSHOT.jar";
    private static final String GUEST_LOGBACK_PATH = "e:";
    private static final String GUEST_APP_CLASS = "com.github.emmanueltouzery.vboxproxyguest.App";
    private static final int PORT = 2222;
    private static final String SHARED_KEY = "testkey";

    private static ConcurrentLinkedQueue<StreamHelpers.ByteArray> pendingMessages = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) throws Exception {
        // pazi convert the byte[] to string.
        Socket clientSocket = openServerSocket(PORT);
        InputStream clientIs = clientSocket.getInputStream();
        Consumer<StreamHelpers.ByteArray> msgProcessor = bytes ->
            Try.run(() -> queueMessage(GUEST_ID, SHARED_KEY, bytes));
        Thread readerThread = new Thread(
            () -> StreamHelpers.streamHandleAsAvailable(clientIs, msgProcessor, t -> t.printStackTrace()));
        readerThread.start();
        Consumer<StreamHelpers.ByteArray> toClientWriter = data ->
            Try.run(() -> {
                    System.out.println("writing to downsocket: " + new String(data.bytes, "UTF-8"));
                    clientSocket.getOutputStream().write(data.bytes);
                    clientSocket.getOutputStream().flush();
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

    private static void queueMessage(String guestId, String key, StreamHelpers.ByteArray value) throws IOException {
        System.out.println("got message from socket client, forwarding to guest => " + new String(value.bytes, "UTF-8"));
        pendingMessages.add(value);
    }

    private static void sendMessage(String guestId, String key, StreamHelpers.ByteArray value) throws IOException {
        String encoded = Base64.getEncoder().encodeToString(value.bytes);
        System.out.println("Sending to guest => " + value.bytes + " (length: " + encoded.length() + ")");
        ProcessBuilder proc = new ProcessBuilder(
            "VBoxManage", "guestproperty", "set", guestId, key, encoded);
        Process p = proc.start();
    }

    private static void messageSender(String guestId, String key) {
        while (true) {
            try {
                while (!guestDidReadPreviousMessage(guestId, key)) {
                    Thread.sleep(10);
                }
                StreamHelpers.ByteArray msg = pendingMessages.peek();
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
        String output = new String(StreamHelpers.streamToByteArray(stream), "UTF-8");
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

    private static void runGuestApp(String guestId, String guestUser, String guestAppPath,
                                    Consumer<StreamHelpers.ByteArray> handler) throws Exception {
        ProcessBuilder proc = new ProcessBuilder(
            "VBoxManage", "guestcontrol", "--username", guestUser, guestId, "run",
            "--exe", GUEST_JAVA_PATH, "--wait-stdout", "--", "java", "-cp", guestAppPath + ";" + GUEST_LOGBACK_PATH, GUEST_APP_CLASS);
        Process p = proc.start();
        InputStream stream = p.getInputStream();
        StreamHelpers.streamHandleAsAvailable(
            stream, bytes -> {
                Try.run(() -> {
                        String base64 = new String(bytes.bytes, "UTF-8");
                        System.out.println("will give to down socket => <" + base64 + ">");
                        System.out.println("will give to down socket2 => " + Base64.getDecoder().decode(base64));
                        handler.accept(new StreamHelpers.ByteArray(Base64.getDecoder().decode(base64)));
                    }).orElseRun(x -> x.printStackTrace());
            },
            t -> t.printStackTrace());
    }
}
