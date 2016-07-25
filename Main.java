import java.io.*;
import java.util.*;

public class Main {

    private static final String VIRTUALBOX_FOLDER = "C:\\Program Files\\Oracle\\VirtualBox Guest Additions\\";

    public static void main(String[] argv) throws Exception {
        System.out.println("Hello world!");
        Scanner scan = new Scanner(System.in);

        System.out.print(new String(readFromHost("testkey"), "UTF-8"));
    }

    private static byte[] readFromHost(String key) throws IOException {
        ProcessBuilder proc = new ProcessBuilder(
            VIRTUALBOX_FOLDER + "VBoxControl.exe", "guestproperty", "get", key);
        Process p = proc.start();
        // will probably base64-encode anyway as I use the command-line tools
        String data = new String(streamToByteArray(p.getInputStream()), "UTF-8");
        final String discriminator = "Value: ";
        return data.substring(data.indexOf(discriminator) + discriminator.length()).getBytes();
    }

    // http://stackoverflow.com/a/30618794/516188
    // fix when java9 is released http://stackoverflow.com/a/37681322/516188
    private static byte[] streamToByteArray(InputStream stream) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        int byteCount = 0;
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
