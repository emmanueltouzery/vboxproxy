package com.github.emmanueltouzery.vboxproxyguest;

import com.beust.jcommander.Parameter;

public class CommandlineParams {

    @Parameter(names="--remoteServerIp", required=true)
    private String remoteServerIp;

    @Parameter(names="--remoteServerPort", required=true)
    private Integer remoteServerPort;

    @Parameter(names="--communicationKey", required=true)
    private String communicationKey;

    @Parameter(names = { "--help", "-help", "-?", "/?" }, help = true)
    private boolean help;

    public String getRemoteServerIp() {
        return remoteServerIp;
    }

    public int getRemoteServerPort() {
        return remoteServerPort;
    }

    public String getCommunicationKey() {
        return communicationKey;
    }

    public boolean isHelp() {
        return help;
    }
}
