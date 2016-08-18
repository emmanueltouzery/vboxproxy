package com.github.emmanueltouzery.vboxproxycommon;

import org.junit.*;
import javaslang.*;

public class ByteArrayTest {

    @Test
    public void simpleDrop() {
        Assert.assertArrayEquals(new byte[]{4,5}, new ByteArray(new byte[]{1,2,3,4,5}).drop(3).bytes);
    }

    @Test
    public void simpleSplit() {
        Tuple2<ByteArray, ByteArray> split = new ByteArray(new byte[]{1,2,3,4,5}).split(2);
        Assert.assertArrayEquals(new byte[]{1,2}, split._1.bytes);
        Assert.assertArrayEquals(new byte[]{3,4,5}, split._2.bytes);
    }

    @Test
    public void splitAtEnd() {
        Tuple2<ByteArray, ByteArray> split = new ByteArray(new byte[]{1,2,3,4,5}).split(5);
        Assert.assertArrayEquals(new byte[]{1,2,3,4,5}, split._1.bytes);
        Assert.assertArrayEquals(new byte[0], split._2.bytes);
    }

    @Test
    public void append() {
        Assert.assertArrayEquals(
            new byte[]{1,2,3,4,5},
            new ByteArray(new byte[]{1,2,3}).append(new ByteArray(new byte[]{4,5})).bytes);
    }
}
