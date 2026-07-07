package com.ntnn.database.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "STDF_LOTS")
public class StdfLotEntity {
    @Id
    @Column(name = "LOT_ID", length = 100)
    private String lotId;

    @Column(name = "PART_TYP", length = 100)
    private String partType;

    @Column(name = "JOB_NAM", length = 100)
    private String jobName;

    @Column(name = "OPER_NAM", length = 100)
    private String operatorName;

    @Column(name = "START_TIME")
    private Instant startTime;

    @Column(name = "SETUP_TIME")
    private Instant setupTime;

    public StdfLotEntity() {
    }

    public StdfLotEntity(String lotId, String partType, String jobName, String operatorName, Instant startTime, Instant setupTime) {
        this.lotId = lotId;
        this.partType = partType;
        this.jobName = jobName;
        this.operatorName = operatorName;
        this.startTime = startTime;
        this.setupTime = setupTime;
    }

    public String getLotId() {
        return lotId;
    }

    public void setLotId(String lotId) {
        this.lotId = lotId;
    }

    public String getPartType() {
        return partType;
    }

    public void setPartType(String partType) {
        this.partType = partType;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getSetupTime() {
        return setupTime;
    }

    public void setSetupTime(Instant setupTime) {
        this.setupTime = setupTime;
    }
}
