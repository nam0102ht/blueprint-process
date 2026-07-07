package com.ntnn.testutil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class StdfTestWriter implements AutoCloseable {
    private final FileOutputStream fos;
    private final ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

    public StdfTestWriter(String filePath) throws IOException {
        this.fos = new FileOutputStream(filePath);
    }

    private void writeRecord(int type, int subtype, byte[] body) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(4).order(byteOrder);
        header.putShort((short) body.length);
        header.put((byte) type);
        header.put((byte) subtype);
        fos.write(header.array());
        fos.write(body);
    }

    public void writeFar(int cpuType, int stdfVer) throws IOException {
        ByteBuffer body = ByteBuffer.allocate(2).order(byteOrder);
        body.put((byte) cpuType);
        body.put((byte) stdfVer);
        writeRecord(0, 10, body.array());
    }

    public void writeMir(String lotId, String partType, String jobName, String operatorName, long startTime, long setupTime) throws IOException {
        ByteBuffer temp = ByteBuffer.allocate(4096).order(byteOrder);
        temp.putInt((int) setupTime);
        temp.putInt((int) startTime);
        temp.put((byte) 1); // STAT_NUM
        temp.put((byte) 'P'); // MODE_COD
        temp.put((byte) ' '); // RTST_COD
        temp.put((byte) ' '); // PROT_COD
        temp.putShort((short) 0); // BURN_TIM
        temp.put((byte) ' '); // CMOD_COD
        
        writeCn(temp, lotId);
        writeCn(temp, partType);
        writeCn(temp, jobName);
        writeCn(temp, ""); // JOB_REV
        writeCn(temp, ""); // SBLOT_ID
        writeCn(temp, operatorName);
        writeCn(temp, ""); // EXEC_TYP
        writeCn(temp, ""); // TEST_COD
        writeCn(temp, ""); // TST_TEMP
        writeCn(temp, ""); // USER_TXT
        writeCn(temp, ""); // AUX_FILE
        writeCn(temp, ""); // PKG_TYP
        writeCn(temp, ""); // FAMLY_ID
        writeCn(temp, ""); // DATE_COD
        writeCn(temp, ""); // FACIL_ID
        writeCn(temp, ""); // FLOOR_ID

        byte[] body = new byte[temp.position()];
        System.arraycopy(temp.array(), 0, body, 0, body.length);
        writeRecord(1, 10, body);
    }

    public void writePtr(long testNum, int headNum, int siteNum, float result, String testTxt, float loLimit, float hiLimit) throws IOException {
        ByteBuffer temp = ByteBuffer.allocate(1024).order(byteOrder);
        temp.putInt((int) testNum);
        temp.put((byte) headNum);
        temp.put((byte) siteNum);
        temp.put((byte) 0); // TEST_FLG
        temp.put((byte) 0); // PARM_FLG
        temp.putFloat(result);
        writeCn(temp, testTxt);
        writeCn(temp, ""); // ALARM_ID
        temp.put((byte) 0); // OPT_FLG
        temp.put((byte) 0); // RES_SCAL
        temp.put((byte) 0); // LLM_SCAL
        temp.put((byte) 0); // HLM_SCAL
        temp.putFloat(loLimit);
        temp.putFloat(hiLimit);

        byte[] body = new byte[temp.position()];
        System.arraycopy(temp.array(), 0, body, 0, body.length);
        writeRecord(15, 10, body);
    }

    public void writeMrr(long finishTime, String dispositionCode, String usrDesc, String excDesc) throws IOException {
        ByteBuffer temp = ByteBuffer.allocate(1024).order(byteOrder);
        temp.putInt((int) finishTime);
        temp.put((byte) (dispositionCode.isEmpty() ? ' ' : dispositionCode.charAt(0)));
        writeCn(temp, usrDesc);
        writeCn(temp, excDesc);

        byte[] body = new byte[temp.position()];
        System.arraycopy(temp.array(), 0, body, 0, body.length);
        writeRecord(1, 20, body);
    }

    private void writeCn(ByteBuffer buf, String s) {
        if (s == null || s.isEmpty()) {
            buf.put((byte) 0);
        } else {
            byte[] bytes = s.getBytes(StandardCharsets.US_ASCII);
            buf.put((byte) bytes.length);
            buf.put(bytes);
        }
    }

    @Override
    public void close() throws IOException {
        fos.close();
    }
}
