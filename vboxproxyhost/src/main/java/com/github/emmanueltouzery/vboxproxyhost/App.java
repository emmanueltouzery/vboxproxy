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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.emmanueltouzery.vboxproxycommon.*;

public class App {

    private static final String GUEST_ID = "bb74cf65-d9af-40a6-804b-44162d8795dd";
    private static final String GUEST_USERNAME = "Mitja Resman";
    private static final String GUEST_JAVA_PATH = "C:\\Program Files\\Java\\jre1.8.0_102\\bin\\java.exe";
    private static final String GUEST_APP_PATH = "e:\\vboxproxyguest-1.0-SNAPSHOT.jar";
    private static final String GUEST_LOGBACK_PATH = "e:";
    private static final String GUEST_APP_CLASS = "com.github.emmanueltouzery.vboxproxyguest.App";
    private static final int PORT = 2222;
    private static final int VBOX_GUEST_PROPERTIES_MAX_LENGTH = 80; // it's 128 according to GuestPropertySvc.h -- need to add 33% base64 overhead. That's 107 so still margins.

    private static final String SHARED_KEY = "testkey";
    private static int nextKeyIndex = 0;
    private static final int KEYS_COUNT = 16;

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static ConcurrentLinkedQueue<StreamHelpers.ByteArray> pendingMessages = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) throws Exception {
        // pazi convert the byte[] to string.
        Socket clientSocket = openServerSocket(PORT);
        InputStream clientIs = clientSocket.getInputStream();
        Consumer<StreamHelpers.ByteArray> msgProcessor = bytes ->
            Try.run(() -> queueMessage(GUEST_ID, bytes));
        Thread readerThread = new Thread(
            () -> StreamHelpers.streamHandleAsAvailable(clientIs, msgProcessor, t -> t.printStackTrace()));
        readerThread.start();
        Consumer<StreamHelpers.ByteArray> toClientWriter = data ->
            Try.run(() -> {
                    System.out.println("writing to downsocket: " + StreamHelpers.summarize(new String(data.bytes, "UTF-8")));
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

    private static void queueMessage(String guestId, StreamHelpers.ByteArray value) throws IOException {
        System.out.println("got message from socket client, forwarding to guest => " + StreamHelpers.summarize(new String(value.bytes, "UTF-8")));
        Iterator<StreamHelpers.ByteArray> items = Array.ofAll(value.bytes)
            .grouped(VBOX_GUEST_PROPERTIES_MAX_LENGTH)
            .map(StreamHelpers.ByteArray::new);
        // System.out.println(items.toList());
        pendingMessages.addAll(items.toJavaList());
    }

    private static void sendMessage(String guestId, String key, StreamHelpers.ByteArray value) throws Exception {
        String encoded = Base64.getEncoder().encodeToString(value.bytes);
        logger.info("Sending to guest => {} (length: {})",
                    StreamHelpers.summarize(value.toString()), encoded.length());
        ProcessBuilder proc = new ProcessBuilder(
            "VBoxManage", "guestproperty", "set", guestId, key, encoded);
        Process p = proc.start();
        // must wait until it's actually sent, or in my checks after this
        // i'll see it's not present and think the guest already read it!
        p.waitFor();
    }

    private static void messageSender(String guestId, String key) {
        while (true) {
            try {
                String actualKey = key + nextKeyIndex;
                while (!guestDidReadPreviousMessage(guestId, actualKey)) {
                    Thread.sleep(10);
                }
                StreamHelpers.ByteArray msg = pendingMessages.peek();
                if (msg != null) {
                    logger.info("The guest read the previous message, putting the next one! " + actualKey);
                    sendMessage(guestId, actualKey, msg);
                    pendingMessages.remove();
                    nextKeyIndex = (nextKeyIndex + 1) % KEYS_COUNT;
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
                        System.out.println("will give to down socket2 => " + StreamHelpers.summarize(new String(bytes.bytes, "UTF-8")));
                        handler.accept(bytes);
                    }).orElseRun(x -> x.printStackTrace());
            },
            t -> t.printStackTrace());
    }
}
