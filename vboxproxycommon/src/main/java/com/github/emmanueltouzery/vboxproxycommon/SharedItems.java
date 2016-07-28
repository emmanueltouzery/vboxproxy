package com.github.emmanueltouzery.vboxproxycommon;

public class SharedItems {

    public static final int KEYS_COUNT = 16;

    public static String getKillSwitchPropName(String baseKey) {
        return baseKey + "_killswitch";
    }

    public static String getSocketsCountPropName(String baseKey) {
        return baseKey + "_socketscount";
    }

    public static String getDataPropName(String baseKey, int socket, int register) {
        return baseKey + "_" + socket + "_" + register;
    }

}
