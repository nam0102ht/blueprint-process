package com.ntnn.database;

import com.ntnn.database.entity.StdfLotEntity;
import com.ntnn.database.entity.TestResultEntity;
import com.ntnn.database.repository.StdfLotRepository;
import com.ntnn.database.repository.TestResultRepository;
import com.ntnn.stdf.StdfRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DatabaseClient {
    private final StdfLotRepository lotRepository;
    private final TestResultRepository testResultRepository;

    @Autowired
    public DatabaseClient(StdfLotRepository lotRepository, TestResultRepository testResultRepository) {
        this.lotRepository = lotRepository;
        this.testResultRepository = testResultRepository;
    }

    public void initializeSchema() {
        // Schema is initialized automatically by Spring Boot (spring.jpa.hibernate.ddl-auto=update)
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void insertLot(StdfRecord.MirRecord mir) {
        StdfLotEntity lot = new StdfLotEntity(
            mir.lotId(),
            mir.partType(),
            mir.jobName(),
            mir.operatorName(),
            Instant.ofEpochSecond(mir.startTime()),
            Instant.ofEpochSecond(mir.setupTime())
        );
        lotRepository.save(lot);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void insertTestResult(String lotId, StdfRecord.PtrRecord ptr) {
        TestResultEntity result = new TestResultEntity(
            lotId,
            ptr.testNum(),
            ptr.headNum(),
            ptr.siteNum(),
            (double) ptr.result(),
            ptr.testTxt(),
            (double) ptr.loLimit(),
            (double) ptr.hiLimit(),
            ptr.testFlag()
        );
        testResultRepository.save(result);
    }
}
