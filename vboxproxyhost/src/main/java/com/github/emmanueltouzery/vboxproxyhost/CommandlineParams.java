package com.github.emmanueltouzery.vboxproxyhost;

import com.beust.jcommander.Parameter;

public class CommandlineParams {

    @Parameter(names="--port", required=true)
    private Integer port;

    @Parameter(names="--remoteServerIp", required=true)
    private String remoteServerIp;

    @Parameter(names="--remoteServerPort")
    private Integer remoteServerPort = 22;

    @Parameter(names = { "--help", "-help", "-?", "/?" }, help = true)
    private boolean help;

    public int getPort() {
        return port;
    }

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
