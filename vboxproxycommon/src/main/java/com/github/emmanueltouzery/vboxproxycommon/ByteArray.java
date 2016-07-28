package com.github.emmanueltouzery.vboxproxycommon;

import javaslang.*;
import javaslang.collection.*;
import javaslang.control.*;

public class ByteArray {

    public final byte[] bytes;

    public ByteArray(byte[] data) {
        bytes = data;
    }

    public ByteArray(Seq<Byte> data) {
        bytes = new byte[data.length()];
        int i = 0;
        for (byte b: data) {
            bytes[i++] = b;
        }
    }

    public int length() {
        return bytes.length;
    }

    public boolean isEmpty() {
        return bytes.length == 0;
    }

    public ByteArray append(ByteArray other) {
        byte[] allBytes = new byte[bytes.length+other.bytes.length];
        int i = 0;
        for (byte b : bytes) {
            allBytes[i++] = b;
        }
        for (byte b : other.bytes) {
            allBytes[i++] = b;
        }
        return new ByteArray(allBytes);
    }

    public ByteArray drop(int byteCount) {
        if (byteCount > bytes.length) {
            throw new IllegalArgumentException(String.format("ByteArray of length %d can't drop %d bytes", bytes.length, byteCount));
        }
        byte[] newData = new byte[bytes.length - byteCount];
        int i = byteCount;
        for (byte b : bytes) {
            newData[i++] = b;
        }
        return new ByteArray(newData);
    }

    // TODO needs a test, or a couple of them.
    public Tuple2<ByteArray,ByteArray> split(int byteIdx) {
        if (byteIdx > bytes.length) {
            throw new IllegalArgumentException(String.format("ByteArray of length %d can't split at %d bytes", bytes.length, byteIdx));
        }
        byte[] part1 = new byte[byteIdx];
        for(int i=0;i < part1.length;i++) {
            part1[i] = bytes[i];
        }
        byte[] part2 = new byte[bytes.length - byteIdx];
        for(int i=0;i < part2.length;i++) {
            part2[i] = bytes[byteIdx+i];
        }
        return Tuple.of(new ByteArray(part1), new ByteArray(part2));
    }

    // for logging
    @Override
    public String toString() {
        return Try.of(() -> new String(bytes, "UTF-8")
                      .replaceAll("\\p{C}", "?")) // http://stackoverflow.com/a/6199346/516188
            .getOrElse("** not utf8 **");
    }
}
