package com.github.emmanueltouzery.vboxproxyguest;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.net.*;
import java.util.Base64;
import javaslang.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javaslang.control.*;
import javaslang.collection.*;

import com.github.emmanueltouzery.vboxproxycommon.*;

public class App {

    private static final String VIRTUALBOX_FOLDER = "C:\\Program Files\\Oracle\\VirtualBox Guest Additions\\";
    // private static final String REMOTE_SERVER = "10.5.5.77";
    // private static final int REMOTE_PORT = 9080;
    // private static final String REMOTE_SERVER = "192.168.40.4";
    private static final String REMOTE_SERVER = "10.5.5.69";
    private static final int REMOTE_PORT = 22;
    private static final int WAIT_TIMEOUT_MS = 500;

    private static final String SHARED_KEY = "testkey";
    private static int nextKeyIndex = 0;
    private static final int KEYS_COUNT = 16;

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] argv) throws Exception {
        logger.info("\n\n\n######## vboxproxyguest starting!");
        Socket socket = new Socket(REMOTE_SERVER, REMOTE_PORT);
        final OutputStream socketOs = socket.getOutputStream();
        final InputStream socketIs = socket.getInputStream();

        Thread socketWriterThread = new Thread(() -> writeToSocket(socketOs));
        socketWriterThread.start();

        Thread socketReaderThread = new Thread(() -> readFromSocket(socketIs));
        socketReaderThread.start();
    }

    // we communicate with the host through guest properties.
    private static void writeToSocket(OutputStream stream) {
        while (true) {
            try {
                String actualKey = SHARED_KEY + nextKeyIndex;
                nextKeyIndex = (nextKeyIndex + 1) % KEYS_COUNT;
                logger.info("waiting for host message " + actualKey);
                Option<StreamHelpers.ByteArray> hostMsg;
                while ((hostMsg = readFromHost(actualKey)).isEmpty()) {
                    waitForHost(actualKey);
                }
                stream.write(hostMsg.get().bytes);
                stream.flush();
                logger.info("host says: " + new String(hostMsg.get().bytes, "UTF-8"));
                clearFromHost(actualKey);
                logger.info("successfully cleared from host");
            } catch (Throwable t) {
                logger.error("error in writeToSocket", t);
            }
        }
    }

    // we communicate with the host by writing to stdout.
    private static void readFromSocket(InputStream stream) {
        StreamHelpers.streamHandleAsAvailable(stream, data -> {
                String base64 = Base64.getEncoder().encodeToString(data.bytes);
                Try.run(() -> logger.info("remote says: {} -- that's {} bytes long in base64.",
                                          StreamHelpers.summarize(new String(data.bytes, "UTF-8")), base64.length()));
                Try.run(() -> System.out.write(data.bytes));
                System.out.flush();
            }, t -> logger.error("error reading from socket", t));
    }

    private static Option<StreamHelpers.ByteArray> readFromHost(String key) throws IOException {
        ProcessBuilder proc = new ProcessBuilder(
            VIRTUALBOX_FOLDER + "VBoxControl.exe", "guestproperty", "get", key);
        Process p = proc.start();
        // must base64-encode as I use the command-line tools
        String data = new String(StreamHelpers.streamToByteArray(p.getInputStream()), "UTF-8");
        final String discriminator = "Value: ";
        if (data.indexOf(discriminator) < 0) {
            return Option.none();
        }
        final String base64 = data
            .substring(data.indexOf(discriminator) + discriminator.length())
            .trim();
        return Option.of(
            new StreamHelpers.ByteArray(Base64.getDecoder().decode(base64)));
    }

    private static void clearFromHost(String key) throws Exception {
        ProcessBuilder proc = new ProcessBuilder(
            VIRTUALBOX_FOLDER + "VBoxControl.exe", "guestproperty", "delete", key);
        Process p = proc.start();
        // p.waitFor(); // optimization.
    }

    private static void waitForHost(String key) throws Exception {
        logger.info("Waiting for host");
        ProcessBuilder proc = new ProcessBuilder(
            VIRTUALBOX_FOLDER + "VBoxControl.exe", "guestproperty", "wait", key,
            "--timeout", Integer.toString(WAIT_TIMEOUT_MS));
        Process p = proc.start();
        p.waitFor();
    }
}
