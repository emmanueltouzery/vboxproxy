package com.github.emmanueltouzery.vboxproxyhost;

import org.junit.*;
import javaslang.*;
import javaslang.collection.*;
import javaslang.control.*;

import com.github.emmanueltouzery.vboxproxycommon.*;

public class GuestMessagesProcessorTest {

    @Test
    public void mergePacketsEmpty() {
        List<ByteArray> packets = List.empty();
        Assert.assertEquals(
            Option.none(),
            GuestMessagesProcessor.mergePacketsToGetFirstPacketLength(packets, 10));
    }

    @Test
    public void mergePacketsNotEnough() {
        List<ByteArray> packets = List.of(new ByteArray(new byte[]{1,2,3,4,5,6,7,8,9}));
        Assert.assertEquals(
            Option.none(),
            GuestMessagesProcessor.mergePacketsToGetFirstPacketLength(packets, 10));
    }

    @Test
    public void mergePacketsNotEnough2() {
        List<ByteArray> packets = List.of(new ByteArray(new byte[]{1,2,3,4,5}), new ByteArray(new byte[]{6,7,8,9}));
        Assert.assertEquals(
            Option.none(),
            GuestMessagesProcessor.mergePacketsToGetFirstPacketLength(packets, 10));
    }

    @Test
    public void mergePacketsSimple() {
        List<ByteArray> packets = List.of(
            new ByteArray(new byte[]{1,2,3,4,5}),
            new ByteArray(new byte[]{6,7,8,9}),
            new ByteArray(new byte[]{10,11,12}));
        Assert.assertEquals(
            Option.of(List.of(
                          new ByteArray(new byte[]{1,2,3,4,5,6,7,8,9}),
                          new ByteArray(new byte[]{10,11,12}))),
            GuestMessagesProcessor.mergePacketsToGetFirstPacketLength(packets, 7));
    }

    @Test
    public void simpleExtractFirstMessage() {
        GuestResponseHeaders.HeadersData headersData =
            new GuestResponseHeaders.HeadersData(5, 1);
        ByteArray data = new ByteArray(headersData.toBytesWithData(new byte[]{1,2,3,4,5}));
        Tuple2<GuestMessagesProcessor.GuestMessage, List<ByteArray>> actual =
            GuestMessagesProcessor.extractFirstMessage(
                headersData,
                List.of(data.append(new ByteArray(new byte[]{6,7,8,9})),
                        new ByteArray(new byte[]{10,11,12}),
                        new ByteArray(new byte[]{13,14})));
        Assert.assertArrayEquals(new byte[]{1,2,3,4,5}, actual._1.msg.bytes);
        Assert.assertEquals(3, actual._2.length());
        Assert.assertArrayEquals(new byte[]{6,7,8,9}, actual._2.get(0).bytes);
        Assert.assertArrayEquals(new byte[]{10,11,12}, actual._2.get(1).bytes);
        Assert.assertArrayEquals(new byte[]{13,14}, actual._2.get(2).bytes);
    }
}
