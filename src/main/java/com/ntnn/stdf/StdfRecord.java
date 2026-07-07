package com.ntnn.stdf;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface StdfRecord {

    record FarRecord(
        @JsonProperty("cpuType") int cpuType,
        @JsonProperty("stdfVer") int stdfVer
    ) implements StdfRecord {}

    record MirRecord(
        @JsonProperty("setupTime") long setupTime,
        @JsonProperty("startTime") long startTime,
        @JsonProperty("stationNum") int stationNum,
        @JsonProperty("modeCode") String modeCode,
        @JsonProperty("retestCode") String retestCode,
        @JsonProperty("protectionCode") String protectionCode,
        @JsonProperty("burnTime") int burnTime,
        @JsonProperty("commandModeCode") String commandModeCode,
        @JsonProperty("lotId") String lotId,
        @JsonProperty("partType") String partType,
        @JsonProperty("jobName") String jobName,
        @JsonProperty("jobRevision") String jobRevision,
        @JsonProperty("subLotId") String subLotId,
        @JsonProperty("operatorName") String operatorName,
        @JsonProperty("execType") String execType,
        @JsonProperty("testCode") String testCode,
        @JsonProperty("testTemp") String testTemp,
        @JsonProperty("userText") String userText,
        @JsonProperty("auxFile") String auxFile,
        @JsonProperty("packageType") String packageType,
        @JsonProperty("familyId") String familyId,
        @JsonProperty("dateCode") String dateCode,
        @JsonProperty("facilityId") String facilityId,
        @JsonProperty("floorId") String floorId
    ) implements StdfRecord {}

    record PtrRecord(
        @JsonProperty("testNum") long testNum,
        @JsonProperty("headNum") int headNum,
        @JsonProperty("siteNum") int siteNum,
        @JsonProperty("testFlag") int testFlag,
        @JsonProperty("paramFlag") int paramFlag,
        @JsonProperty("result") float result,
        @JsonProperty("testTxt") String testTxt,
        @JsonProperty("alarmId") String alarmId,
        @JsonProperty("loLimit") float loLimit,
        @JsonProperty("hiLimit") float hiLimit
    ) implements StdfRecord {}

    record MrrRecord(
        @JsonProperty("finishTime") long finishTime,
        @JsonProperty("dispositionCode") String dispositionCode,
        @JsonProperty("usrDesc") String usrDesc,
        @JsonProperty("excDesc") String excDesc
    ) implements StdfRecord {}
}
