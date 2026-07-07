package com.ntnn.stdf;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public class StdfParser implements AutoCloseable {
    private final Arena arena;
    private final MemorySegment segment;
    private final long fileSize;
    private long fileOffset = 0;
    
    // We start with a default Little Endian layout to read the first record's header.
    // Once FAR is read, we update this with the correct byte order.
    private ByteOrderHelper orderHelper = new ByteOrderHelper(ByteOrder.LITTLE_ENDIAN);

    public StdfParser(String filePath) throws IOException {
        this.arena = Arena.ofShared();
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r");
             FileChannel channel = raf.getChannel()) {
            this.fileSize = channel.size();
            this.segment = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize, arena);
        } catch (Exception e) {
            arena.close();
            throw e;
        }
        
        // Determine Endianness from FAR record (CPU_TYPE field at offset 4)
        if (fileSize >= 6) {
            int cpuType = segment.get(ValueLayout.JAVA_BYTE, 4) & 0xFF;
            this.orderHelper = ByteOrderHelper.forCpuType(cpuType);
        }
    }

    public boolean hasNext() {
        return fileOffset < fileSize;
    }

    public RawRecord nextRecord() {
        if (!hasNext()) {
            return null;
        }

        // Read header: 2 bytes length (U2), 1 byte type (U1), 1 byte subtype (U1)
        int length = segment.get(orderHelper.u2, fileOffset) & 0xFFFF;
        int type = segment.get(ValueLayout.JAVA_BYTE, fileOffset + 2) & 0xFF;
        int subtype = segment.get(ValueLayout.JAVA_BYTE, fileOffset + 3) & 0xFF;

        MemorySegment bodySlice = segment.asSlice(fileOffset + 4, length);
        
        fileOffset += 4 + length;

        return new RawRecord(type, subtype, bodySlice);
    }

    public ByteOrderHelper getOrderHelper() {
        return orderHelper;
    }

    @Override
    public void close() {
        arena.close();
    }

    public record RawRecord(int type, int subtype, MemorySegment body) {}

    public static class SliceReader {
        private final MemorySegment segment;
        private final ByteOrderHelper orderHelper;
        private long offset = 0;

        public SliceReader(MemorySegment segment, ByteOrderHelper orderHelper) {
            this.segment = segment;
            this.orderHelper = orderHelper;
        }

        public int readU1() {
            byte b = segment.get(ValueLayout.JAVA_BYTE, offset);
            offset += 1;
            return b & 0xFF;
        }

        public int readI1() {
            byte b = segment.get(ValueLayout.JAVA_BYTE, offset);
            offset += 1;
            return b;
        }

        public int readU2() {
            short s = segment.get(orderHelper.u2, offset);
            offset += 2;
            return s & 0xFFFF;
        }

        public int readI2() {
            short s = segment.get(orderHelper.u2, offset);
            offset += 2;
            return s;
        }

        public long readU4() {
            int i = segment.get(orderHelper.u4, offset);
            offset += 4;
            return i & 0xFFFFFFFFL;
        }

        public int readI4() {
            int i = segment.get(orderHelper.u4, offset);
            offset += 4;
            return i;
        }

        public float readR4() {
            float f = segment.get(orderHelper.r4, offset);
            offset += 4;
            return f;
        }

        public String readC1() {
            byte b = segment.get(ValueLayout.JAVA_BYTE, offset);
            offset += 1;
            return b == 0 ? "" : String.valueOf((char) b);
        }

        public String readCn() {
            int len = readU1();
            if (len == 0) {
                return "";
            }
            byte[] bytes = new byte[len];
            MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, offset, bytes, 0, len);
            offset += len;
            return new String(bytes, StandardCharsets.US_ASCII);
        }
    }

    // Parsers for specific records
    public static StdfRecord.FarRecord parseFar(MemorySegment body, ByteOrderHelper orderHelper) {
        SliceReader reader = new SliceReader(body, orderHelper);
        int cpuType = reader.readU1();
        int stdfVer = reader.readU1();
        return new StdfRecord.FarRecord(cpuType, stdfVer);
    }

    public static StdfRecord.MirRecord parseMir(MemorySegment body, ByteOrderHelper orderHelper) {
        SliceReader reader = new SliceReader(body, orderHelper);
        long setupTime = reader.readU4();
        long startTime = reader.readU4();
        int stationNum = reader.readU1();
        String modeCode = reader.readC1();
        String retestCode = reader.readC1();
        String protectionCode = reader.readC1();
        int burnTime = reader.readU2();
        String commandModeCode = reader.readC1();
        String lotId = reader.readCn();
        String partType = reader.readCn();
        String jobName = reader.readCn();
        String jobRevision = reader.readCn();
        String subLotId = reader.readCn();
        String operatorName = reader.readCn();
        String execType = reader.readCn();
        String testCode = reader.readCn();
        String testTemp = reader.readCn();
        String userText = reader.readCn();
        String auxFile = reader.readCn();
        String packageType = reader.readCn();
        String familyId = reader.readCn();
        String dateCode = reader.readCn();
        String facilityId = reader.readCn();
        String floorId = reader.readCn();
        return new StdfRecord.MirRecord(
            setupTime, startTime, stationNum, modeCode, retestCode, protectionCode,
            burnTime, commandModeCode, lotId, partType, jobName, jobRevision,
            subLotLotId(subLotId), operatorName, execType, testCode, testTemp, userText,
            auxFile, packageType, familyId, dateCode, facilityId, floorId
        );
    }

    private static String subLotLotId(String val) {
        return val == null ? "" : val;
    }

    public static StdfRecord.PtrRecord parsePtr(MemorySegment body, ByteOrderHelper orderHelper) {
        SliceReader reader = new SliceReader(body, orderHelper);
        long testNum = reader.readU4();
        int headNum = reader.readU1();
        int siteNum = reader.readU1();
        int testFlag = reader.readU1();
        int paramFlag = reader.readU1();
        float result = reader.readR4();
        String testTxt = reader.readCn();
        String alarmId = reader.readCn();
        int optFlg = reader.readU1();
        
        // Limits are present unless specific bits in optFlg are set. 
        // We will read them assuming they are present (standard layout with optFlg = 0).
        int resScal = reader.readI1();
        int llmScal = reader.readI1();
        int hlmScal = reader.readI1();
        float loLimit = reader.readR4();
        float hiLimit = reader.readR4();

        return new StdfRecord.PtrRecord(
            testNum, headNum, siteNum, testFlag, paramFlag, result,
            testTxt, alarmId, loLimit, hiLimit
        );
    }

    public static StdfRecord.MrrRecord parseMrr(MemorySegment body, ByteOrderHelper orderHelper) {
        SliceReader reader = new SliceReader(body, orderHelper);
        long finishTime = reader.readU4();
        String dispositionCode = reader.readC1();
        String usrDesc = reader.readCn();
        String excDesc = reader.readCn();
        return new StdfRecord.MrrRecord(finishTime, dispositionCode, usrDesc, excDesc);
    }
}
