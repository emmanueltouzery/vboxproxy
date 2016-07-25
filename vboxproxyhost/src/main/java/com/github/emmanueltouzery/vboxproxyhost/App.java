package com.github.emmanueltouzery.vboxproxyhost;

import java.io.*;
import java.util.function.*;
import javaslang.control.*;

public class App {

    private static final String GUEST_ID = "bb74cf65-d9af-40a6-804b-44162d8795dd";
    private static final String GUEST_USERNAME = "Mitja Resman";
    private static final String GUEST_JAVA_PATH = "c:\\Program Files\\Java\\jdk1.7.0_40\\bin\\java.exe";
    private static final String GUEST_APP_PATH = "e:";

    public static void main(String[] args) throws Exception {
        Consumer<ByteArray> handler = bytes -> Try.run(
            () -> System.out.println("just received: " + new String(bytes.bytes, "UTF-8")));
        Thread receiverThread = new Thread(
            () -> Try.run(() -> runGuestApp(GUEST_ID, GUEST_USERNAME, GUEST_APP_PATH, handler)));
        receiverThread.start();
        sendMessage(GUEST_ID, "testkey", "my test value");
    }

    private static void sendMessage(String guestId, String key, String value) throws IOException {
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
                                    Consumer<ByteArray> handler) throws IOException {
        ProcessBuilder proc = new ProcessBuilder(
            "VBoxManage", "guestcontrol", "--username", guestUser, guestId, "run",
            "--exe", GUEST_JAVA_PATH, "--wait-stdout", "--", "java", "-cp", guestAppPath, "Main");
        Process p = proc.start();
        InputStream stream = p.getInputStream();
        while (true) {
            byte[] read = streamReadAvailable(stream);
            if (read.length > 0) {
                handler.accept(new ByteArray(read));
            }
        }
    }

    private static byte[] streamReadAvailable(InputStream stream) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        int byteCount = 0;
        // read bytes from stream, and store them in buffer
        while ((byteCount = stream.read(buffer)) > 0) {
            // Writes bytes from byte array (buffer) into output stream.
            os.write(buffer, 0, byteCount);
        }
        return os.toByteArray();
    }

}
