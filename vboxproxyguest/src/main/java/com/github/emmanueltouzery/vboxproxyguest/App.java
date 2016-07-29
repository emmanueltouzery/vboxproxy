package com.github.emmanueltouzery.vboxproxyguest;

import com.beust.jcommander.JCommander;
import java.io.*;
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
    private static final int WAIT_TIMEOUT_MS = 500;

    private static int nextKeyIndex = 0;

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static Map<Integer,RemoteSocketHandler> socketHandlersPerSocketId = HashMap.empty();

    public static void main(String[] args) throws Exception {
        logger.info("\n\n\n######## vboxproxyguest starting!");

        CommandlineParams params = new CommandlineParams();
        try {
            JCommander jc = new JCommander(params);
            jc.parse(args);
            if (params.isHelp()) {
                jc.usage();
                System.exit(0);
            }
        } catch (Exception ex) {
            logger.error("Error parsing command-line parameters", ex);
            System.exit(1);
        }

        Thread socketRegistrationThread = new Thread(
            () -> listenForSocketRegister(params.getCommunicationKey(),
                                          params.getRemoteServerIp(),
                                          params.getRemoteServerPort()));
        socketRegistrationThread.start();

        String killSwitchKey = SharedItems.getKillSwitchPropName(params.getCommunicationKey());
        while (true) {
            waitForHost(killSwitchKey);
            if (readFromHost(killSwitchKey)
                .map(ba -> ba.bytes)
                .contains("bye".getBytes())) {
                logger.info("Received the kill switch, going away.");
                clearFromHost(killSwitchKey);
                System.exit(0);
            }
        }
    }

    private static void listenForSocketRegister(String baseKey,
                                              String remoteServerIp, int remotePort) {
        final String socketKey = SharedItems.getActiveSocketsPropName(baseKey);
        while (true) {
            try {
                waitForHost(socketKey);
                readFromHost(socketKey).forEach(socketList -> {
                        Set<Integer> activeOnHost =
                            HashSet.of(socketList.toString().split(","))
                            .map(Integer::parseInt);
                        Set<Integer> activeHere = socketHandlersPerSocketId.keySet();

                        Set<Integer> toAdd = activeOnHost.diff(activeHere);
                        toAdd.forEach(
                            sid -> Try.run(() -> addSocketHandler(
                                               baseKey, sid, remoteServerIp,
                                               remotePort))
                            .orElseRun(t -> logger.error("error adding socket handler", t)));

                        Set<Integer> toRemove = activeHere.diff(activeOnHost);
                        toRemove.forEach(sid -> removeSocketHandler(sid));
                    });
            } catch (Throwable t) {
                logger.error("Error in listenForSocketRegister", t);
            }
        }
    }

    private static synchronized void addSocketHandler(String baseKey, int socketId,
                                               String remoteServerIp, int remotePort) throws Exception {
        Socket socket = new Socket(remoteServerIp, remotePort);
        RemoteSocketHandler handler = new RemoteSocketHandler(baseKey, socketId, socket);
        socketHandlersPerSocketId = socketHandlersPerSocketId.put(socketId, handler);
        handler.startThreads();
    }

    private static synchronized void removeSocketHandler(int socketId) {
        // TODO
    }

    private static class RemoteSocketHandler {

        private final Socket socket;
        private final int socketId;

        private final OutputStream socketOs;
        private final InputStream socketIs;

        private final Thread socketWriterThread;
        private final Thread socketReaderThread;

        public RemoteSocketHandler(String baseKey, int socketId, Socket socket) throws IOException {
            this.socket = socket;
            this.socketId = socketId;
            socketOs = socket.getOutputStream();
            socketIs = socket.getInputStream();

            socketWriterThread = new Thread(() -> writeToSocket(baseKey));
            socketReaderThread = new Thread(() -> readFromSocket());
        }

        public void startThreads() {
            socketWriterThread.start();
            socketReaderThread.start();
        }

        // we communicate with the host through guest properties.
        private void writeToSocket(String baseKey) {
            while (true) {
                try {
                    String actualKey = SharedItems.getDataPropName(baseKey, socketId, nextKeyIndex);
                    nextKeyIndex = (nextKeyIndex + 1) % SharedItems.KEYS_COUNT;
                    logger.info("waiting for host message " + actualKey);
                    Option<ByteArray> hostMsg;
                    while ((hostMsg = readFromHost(actualKey)).isEmpty()) {
                        waitForHost(actualKey);
                    }
                    socketOs.write(hostMsg.get().bytes);
                    socketOs.flush();
                    logger.info("host says: " + new String(hostMsg.get().bytes, "UTF-8"));
                    clearFromHost(actualKey);
                    logger.info("successfully cleared from host");
                } catch (Throwable t) {
                    logger.error("error in writeToSocket", t);
                }
            }
        }

        // we communicate with the host by writing to stdout.
        private void readFromSocket() {
            StreamHelpers.streamHandleAsAvailable(socketIs, data -> {
                    Try.run(() -> logger.info("remote says: {}.",
                                              StreamHelpers.summarize(new String(data.bytes, "UTF-8"))));
                    // merge headers & data in one byte[]
                    // as it's critical we write both atomically
                    // -- other threads are also writing stuff!
                    byte[] toWrite = new GuestResponseHeaders.HeadersData(
                        data.bytes.length, socketId).toBytesWithData(data.bytes);
                    Try.run(() -> System.out.write(toWrite));
                    System.out.flush();
                }, t -> logger.error("error reading from socket", t));
        }
    }

    private static Option<ByteArray> readFromHost(String key) throws IOException {
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
            new ByteArray(Base64.getDecoder().decode(base64)));
    }

    private static void clearFromHost(String key) throws Exception {
        ProcessBuilder proc = new ProcessBuilder(
            VIRTUALBOX_FOLDER + "VBoxControl.exe", "guestproperty", "delete", key);
        Process p = proc.start();
        // p.waitFor(); // optimization.
    }

    private static void waitForHost(String key) throws Exception {
        logger.info("Waiting for host, key {}", key);
        ProcessBuilder proc = new ProcessBuilder(
            VIRTUALBOX_FOLDER + "VBoxControl.exe", "guestproperty", "wait", key,
            "--timeout", Integer.toString(WAIT_TIMEOUT_MS));
        Process p = proc.start();
        p.waitFor();
    }
}
