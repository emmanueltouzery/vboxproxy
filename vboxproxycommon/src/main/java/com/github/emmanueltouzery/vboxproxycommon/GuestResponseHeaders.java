package com.github.emmanueltouzery.vboxproxycommon;

import java.nio.ByteBuffer;

public class GuestResponseHeaders {

    public static final int HEADERS_LENGTH = Integer.BYTES*2;

    public static class HeadersData {
        public final int msgLength;
        public final int socketIdx;

        public HeadersData(int msgLength, int socketIdx) {
            this.msgLength = msgLength;
            this.socketIdx = socketIdx;
        }
    }

    public static HeadersData parseHeaders(ByteArray packet) {
        ByteBuffer headersBuf = ByteBuffer.wrap(packet.bytes);
        int length = headersBuf.getInt();
        int socketIdx = headersBuf.getInt();
        return new HeadersData(length, socketIdx);
    }
}
