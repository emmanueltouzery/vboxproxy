package com.github.emmanueltouzery.vboxproxycommon;

import java.util.function.*;
import javaslang.collection.*;
import java.io.*;
import javaslang.control.*;

public class StreamHelpers {

    /**
     * WILL block the current thread!!!
     */
    public static void streamHandleAsAvailable(InputStream stream,
                                               Consumer<ByteArray> handler,
                                               Consumer<Throwable> logger) {
        while (true) {
            try {
                byte[] read = streamReadAvailable(stream);
                if (read.length > 0) {
                    handler.accept(new ByteArray(read));
                }
            } catch (Throwable t) {
                logger.accept(t);
            }
        }
    }

    public static byte[] streamReadAvailable(InputStream stream) throws IOException {
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
    public static byte[] streamToByteArray(InputStream stream) throws IOException {
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

    public static String summarize(String data) {
        Array<String> lines = Array.of(data.split("\n"));
        return lines.head() + ((lines.length() > 5) ? ("\n<...>\n" + lines.drop(lines.length() - 4).mkString("\n")) : "");
    }
}
