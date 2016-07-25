import java.io.*;

public class Host {

    private static final String GUEST_ID = "bb74cf65-d9af-40a6-804b-44162d8795dd";
    private static final String GUEST_USERNAME = "Mitja Resman";
    private static final String GUEST_JAVA_PATH = "c:\\Program Files\\Java\\jdk1.7.0_40\\bin\\java.exe";
    private static final String GUEST_APP_PATH = "e:";

    public static void main(String[] args) throws Exception {
        runGuestApp(GUEST_ID, GUEST_USERNAME, GUEST_APP_PATH);
        sendMessage(GUEST_ID, "testkey", "my test value");
    }

    private static void sendMessage(String guestId, String key, String value) throws IOException {
        ProcessBuilder proc = new ProcessBuilder(
            "VBoxManage", "guestproperty", "set", guestId, key, value);
        Process p = proc.start();
    }

    // TODO emits responses from the guest
    private static void runGuestApp(String guestId, String guestUser,
                                    String guestAppPath) throws IOException {
        ProcessBuilder proc = new ProcessBuilder(
            "VBoxManage", "guestcontrol", "--username", guestUser, guestId, "run",
            "--exe", GUEST_JAVA_PATH, "--wait-stdout", "--", "java", "-cp", guestAppPath, "Main");
        Process p = proc.inheritIO().start();
    }

}
