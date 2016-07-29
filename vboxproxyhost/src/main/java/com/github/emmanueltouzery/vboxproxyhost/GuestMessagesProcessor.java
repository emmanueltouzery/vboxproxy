package com.github.emmanueltouzery.vboxproxyhost;

import javaslang.*;
import javaslang.control.*;
import javaslang.collection.*;
import java.nio.ByteBuffer;

import com.github.emmanueltouzery.vboxproxycommon.*;

public class GuestMessagesProcessor {

    private Vector<ByteArray> undecodedGuestMessages = Vector.empty();

    public synchronized void receivedFromGuest(ByteArray data) {
        undecodedGuestMessages = undecodedGuestMessages.append(data);
    }

    public static class GuestMessage {
        public final int socketIdx;
        public ByteArray msg;

        public GuestMessage(int socketIdx, ByteArray msg) {
            this.socketIdx = socketIdx;
            this.msg = msg;
        }
    }

    /**
     * returns socket index + data
     */
    public synchronized Option<GuestMessage> decodeNext() {
        Option<Tuple2<GuestMessage, List<ByteArray>>> msgAndRestOpt =
            mergePacketsToGetFirstPacketLength(undecodedGuestMessages, GuestResponseHeaders.HEADERS_LENGTH)
            .flatMap(headersInFirstPacket -> {
                    GuestResponseHeaders.HeadersData headersData = GuestResponseHeaders.parseHeaders(headersInFirstPacket.head());
                    return mergePacketsToGetFirstPacketLength(headersInFirstPacket, Integer.BYTES*(2+headersData.msgLength))
                        .map(messageInFirstPacket ->
                                 messageInFirstPacket.head()
                                 .drop(GuestResponseHeaders.HEADERS_LENGTH)
                                 .split(headersData.msgLength*Integer.BYTES)
                                 .transform((msg, rest) -> Tuple.of(
                                                new GuestMessage(headersData.socketIdx, msg),
                                                rest.isEmpty() ? messageInFirstPacket.drop(1) : messageInFirstPacket.drop(1).prepend(rest))));
                });

        return msgAndRestOpt.map(msgAndRest -> {
                undecodedGuestMessages = msgAndRest._2.toVector();
                return msgAndRest._1;
            });
    }

    /**
     * returns Option.none() if there are not enough bytes in all
     * the packets together.
     */
    private static Option<List<ByteArray>> mergePacketsToGetFirstPacketLength(
        Traversable<ByteArray> packets, int length) {
        List<ByteArray> result = packets.foldLeft(
            List.of(packets.head()),
            (sofar, cur) -> sofar.head().length() >= length
            ? sofar.prepend(cur)
            : List.of(sofar.head().append(cur)))
            .reverse();
        return Option.when(result.head().length() >= length, () -> result);
    }
}
