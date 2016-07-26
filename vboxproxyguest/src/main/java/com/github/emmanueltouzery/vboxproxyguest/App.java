package com.github.emmanueltouzery.vboxproxyguest;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.net.*;
import java.util.Base64;
import javaslang.control.*;

import com.github.emmanueltouzery.vboxproxycommon.*;

public class App {

    private static final String VIRTUALBOX_FOLDER = "C:\\Program Files\\Oracle\\VirtualBox Guest Additions\\";
    private static final String SHARED_KEY = "testkey";
    private static final String REMOTE_SERVER = "192.168.40.4";
    private static final int REMOTE_PORT = 22;

    public static void main(String[] argv) throws Exception {
        Scanner scan = new Scanner(System.in);

        Socket socket = new Socket(REMOTE_SERVER, REMOTE_PORT);

        Thread socketWriterThread = new Thread(() -> Try.run(() -> writeToSocket(socket.getOutputStream())));
        socketWriterThread.start();

        Thread socketReaderThread = new Thread(() -> Try.run(() -> readFromSocket(socket.getInputStream())));
        socketReaderThread.start();
    }

    // we communicate with the host through guest properties.
    private static void writeToSocket(OutputStream stream) {
        while (true) {
            try {
                waitForHost(SHARED_KEY);
                stream.write(readFromHost(SHARED_KEY));
                // System.out.println("guest says: " + new String(readFromHost(SHARED_KEY), "UTF-8"));
                clearFromHost(SHARED_KEY);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    // we communicate with the host by writing to stdout.
    private static void readFromSocket(InputStream stream) throws Exception {
        StreamHelpers.streamHandleAsAvailable(stream, data -> System.out.print(Base64.getEncoder().encodeToString(data.bytes)));
    }

    private static byte[] readFromHost(String key) throws IOException {
        ProcessBuilder proc = new ProcessBuilder(
            VIRTUALBOX_FOLDER + "VBoxControl.exe", "guestproperty", "get", key);
        Process p = proc.start();
        // will probably base64-encode anyway as I use the command-line tools
        String data = new String(StreamHelpers.streamToByteArray(p.getInputStream()), "UTF-8");
        final String discriminator = "Value: ";
        return data.substring(data.indexOf(discriminator) + discriminator.length()).getBytes();
    }

    private static void clearFromHost(String key) throws IOException {
        ProcessBuilder proc = new ProcessBuilder(
            VIRTUALBOX_FOLDER + "VBoxControl.exe", "guestproperty", "delete", key);
        Process p = proc.start();
    }

    private static void waitForHost(String key) throws Exception {
        ProcessBuilder proc = new ProcessBuilder(
            VIRTUALBOX_FOLDER + "VBoxControl.exe", "guestproperty", "wait", key);
        Process p = proc.start();
        p.waitFor();
    }
}
