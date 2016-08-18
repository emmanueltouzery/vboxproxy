package com.github.emmanueltouzery.vboxproxyhost;

import javaslang.*;
import javaslang.control.*;
import javaslang.collection.*;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.emmanueltouzery.vboxproxycommon.*;

public class GuestMessagesProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GuestMessagesProcessor.class);

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
            mergePacketsToGetFirstPacketLength(undecodedGuestMessages, GuestResponseHeaders.HEADERS_LENGTH_BYTES)
            .flatMap(headersInFirstPacket -> {
                    GuestResponseHeaders.HeadersData headersData = GuestResponseHeaders.parseHeaders(headersInFirstPacket.head());
                    logger.info("Received a message from socket {} of length {}",
                                headersData.socketIdx, headersData.msgLength);
                    return mergePacketsToGetFirstPacketLength(headersInFirstPacket,
                                                              GuestResponseHeaders.HEADERS_LENGTH_BYTES + headersData.msgLength)
                    .map(messageInFirstPacket -> extractFirstMessage(headersData, messageInFirstPacket));
                });

        return msgAndRestOpt.map(msgAndRest -> {
                logger.info("the message was |{}|", msgAndRest._1.msg);
                undecodedGuestMessages = msgAndRest._2.toVector();
                return msgAndRest._1;
            });
    }

    static Tuple2<GuestMessage, List<ByteArray>> extractFirstMessage(
        GuestResponseHeaders.HeadersData headersData, List<ByteArray> messageInFirstPacket) {
        logger.info("messageInFirstPacket length is {}", messageInFirstPacket.head().bytes.length);
        return messageInFirstPacket.head()
            .drop(GuestResponseHeaders.HEADERS_LENGTH_BYTES)
            .split(headersData.msgLength)
            .transform((msg, rest) -> Tuple.of(
                           new GuestMessage(headersData.socketIdx, msg),
                           rest.isEmpty() ? messageInFirstPacket.drop(1) : messageInFirstPacket.drop(1).prepend(rest)));
    }

    /**
     * returns Option.none() if there are not enough bytes in all
     * the packets together.
     */
    static Option<List<ByteArray>> mergePacketsToGetFirstPacketLength(
        Traversable<ByteArray> packets, int length) {
        if (packets.isEmpty()) {
            return Option.none();
        }
        List<ByteArray> result = packets.foldLeft(
            List.of(new ByteArray(new byte[0])),
            (sofar, cur) -> sofar.head().length() >= length
            ? sofar.prepend(cur)
            : List.of(sofar.head().append(cur)))
            .reverse();
        return Option.when(result.head().length() >= length, () -> result);
    }
}
