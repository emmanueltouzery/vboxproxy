package com.github.emmanueltouzery.vboxproxyguest;

import com.beust.jcommander.Parameter;

public class CommandlineParams {

    @Parameter(names="--remoteServerIp", required=true)
    private String remoteServerIp;

    @Parameter(names="--remoteServerPort", required=true)
    private Integer remoteServerPort;

    @Parameter(names = { "--help", "-help", "-?", "/?" }, help = true)
    private boolean help;

    public String getRemoteServerIp() {
        return remoteServerIp;
    }

    public int getRemoteServerPort() {
        return remoteServerPort;
    }

    public boolean isHelp() {
        return help;
    }
}
