package com.ntnn.stdf;

import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

public class ByteOrderHelper {
    public final ByteOrder byteOrder;
    public final ValueLayout.OfShort u2;
    public final ValueLayout.OfInt u4;
    public final ValueLayout.OfFloat r4;

    public ByteOrderHelper(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
        this.u2 = ValueLayout.JAVA_SHORT.withOrder(byteOrder).withByteAlignment(1);
        this.u4 = ValueLayout.JAVA_INT.withOrder(byteOrder).withByteAlignment(1);
        this.r4 = ValueLayout.JAVA_FLOAT.withOrder(byteOrder).withByteAlignment(1);
    }

    public static ByteOrderHelper forCpuType(int cpuType) {
        // 1, 2, 5, 6 are Little Endian in STDF V4
        // 3, 4 are Big Endian
        if (cpuType == 1 || cpuType == 2 || cpuType == 5 || cpuType == 6) {
            return new ByteOrderHelper(ByteOrder.LITTLE_ENDIAN);
        } else {
            return new ByteOrderHelper(ByteOrder.BIG_ENDIAN);
        }
    }
}
