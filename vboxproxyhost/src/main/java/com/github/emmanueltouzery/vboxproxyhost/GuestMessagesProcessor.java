package com.github.emmanueltouzery.vboxproxyhost;

import javaslang.*;
import javaslang.control.*;
import javaslang.collection.*;
import java.nio.ByteBuffer;

import com.github.emmanueltouzery.vboxproxycommon.*;

public class GuestMessagesProcessor {

    private Vector<ByteArray> undecodedGuestMessages = Vector.empty();
    private Option<ByteArray> lastPartialGuestMessage = Option.none();

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
        List<ByteArray> toConsider = lastPartialGuestMessage.isDefined()
            ? List.ofAll(undecodedGuestMessages).prepend(lastPartialGuestMessage.get())
            : List.ofAll(undecodedGuestMessages);

        Option<Tuple3<GuestMessage, ByteArray, List<ByteArray>>> msgAndRestOpt =
            mergePacketsToGetFirstPacketLength(toConsider, GuestResponseHeaders.HEADERS_LENGTH)
            .flatMap(headersInFirstPacket -> {
                    GuestResponseHeaders.HeadersData headersData = GuestResponseHeaders.parseHeaders(headersInFirstPacket.head());
                    return mergePacketsToGetFirstPacketLength(headersInFirstPacket, Integer.BYTES*(2+headersData.msgLength))
                        .flatMap(messageInFirstPacket ->
                                 headersInFirstPacket
                                 .drop(GuestResponseHeaders.HEADERS_LENGTH)
                                 .split(headersData.msgLength*Integer.BYTES)
                                 .transform((msg, rest) -> Tuple.of(new GuestMessage(headersData.socketIdx, msg), rest, messageInFirstPacket)));
                });

        return msgAndRestOpt.map(msgAndRest -> {
                lastPartialGuestMessage = Option.when(msgAndRest._2.isEmpty(), () -> msgAndRest._2);
                undecodedGuestMessages = msgAndRest._3.drop(1).toVector();
                return msgAndRest._1;
            });
    }

    /**
     * returns Option.none() if there are not enough bytes in all
     * the packets together.
     */
    private static Option<List<ByteArray>> mergePacketsToGetFirstPacketLength(
        List<ByteArray> packets, int length) {
        List<ByteArray> result = packets.foldLeft(
            List.of(packets.head()),
            (sofar, cur) -> sofar.head().length() >= length
            ? sofar.prepend(cur)
            : List.of(sofar.head().append(cur)))
            .reverse();
        return Option.when(result.head().length() >= length, () -> result);
    }
}
