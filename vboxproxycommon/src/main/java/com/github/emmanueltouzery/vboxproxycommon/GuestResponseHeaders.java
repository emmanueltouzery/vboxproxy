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

        public byte[] toBytesWithData(byte[] data) {
            if (data.length != msgLength) {
                throw new IllegalArgumentException(
                    String.format("packet length %d != %d", data.length, msgLength));
            }
            return ByteBuffer.allocate(Integer.BYTES*2+data.length)
                .putInt(msgLength)
                .putInt(socketIdx)
                .put(data)
                .array();
        }
    }

    public static HeadersData parseHeaders(ByteArray packet) {
        ByteBuffer headersBuf = ByteBuffer.wrap(packet.bytes);
        int length = headersBuf.getInt();
        int socketIdx = headersBuf.getInt();
        return new HeadersData(length, socketIdx);
    }
}
