package com.ntnn.database.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "TEST_RESULTS")
public class TestResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TEST_ID")
    private Long testId;

    @Column(name = "LOT_ID", length = 100)
    private String lotId;

    @Column(name = "TEST_NUM")
    private Long testNum;

    @Column(name = "HEAD_NUM")
    private Integer headNum;

    @Column(name = "SITE_NUM")
    private Integer siteNum;

    @Column(name = "RESULT")
    private Double result;

    @Column(name = "TEST_TXT", length = 255)
    private String testTxt;

    @Column(name = "LO_LIMIT")
    private Double loLimit;

    @Column(name = "HI_LIMIT")
    private Double hiLimit;

    @Column(name = "TEST_FLAG")
    private Integer testFlag;

    public TestResultEntity() {
    }

    public TestResultEntity(String lotId, Long testNum, Integer headNum, Integer siteNum, Double result, String testTxt, Double loLimit, Double hiLimit, Integer testFlag) {
        this.lotId = lotId;
        this.testNum = testNum;
        this.headNum = headNum;
        this.siteNum = siteNum;
        this.result = result;
        this.testTxt = testTxt;
        this.loLimit = loLimit;
        this.hiLimit = hiLimit;
        this.testFlag = testFlag;
    }

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public String getLotId() {
        return lotId;
    }

    public void setLotId(String lotId) {
        this.lotId = lotId;
    }

    public Long getTestNum() {
        return testNum;
    }

    public void setTestNum(Long testNum) {
        this.testNum = testNum;
    }

    public Integer getHeadNum() {
        return headNum;
    }

    public void setHeadNum(Integer headNum) {
        this.headNum = headNum;
    }

    public Integer getSiteNum() {
        return siteNum;
    }

    public void setSiteNum(Integer siteNum) {
        this.siteNum = siteNum;
    }

    public Double getResult() {
        return result;
    }

    public void setResult(Double result) {
        this.result = result;
    }

    public String getTestTxt() {
        return testTxt;
    }

    public void setTestTxt(String testTxt) {
        this.testTxt = testTxt;
    }

    public Double getLoLimit() {
        return loLimit;
    }

    public void setLoLimit(Double loLimit) {
        this.loLimit = loLimit;
    }

    public Double getHiLimit() {
        return hiLimit;
    }

    public void setHiLimit(Double hiLimit) {
        this.hiLimit = hiLimit;
    }

    public Integer getTestFlag() {
        return testFlag;
    }

    public void setTestFlag(Integer testFlag) {
        this.testFlag = testFlag;
    }
}
