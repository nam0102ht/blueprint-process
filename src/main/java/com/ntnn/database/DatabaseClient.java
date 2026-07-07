package com.ntnn.database;

import com.ntnn.database.entity.StdfLotEntity;
import com.ntnn.database.entity.TestResultEntity;
import com.ntnn.stdf.StdfRecord;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class DatabaseClient implements AutoCloseable {
    private final EntityManagerFactory emf;

    public DatabaseClient(String jdbcUrl, String username, String password) {
        Map<String, String> properties = new HashMap<>();
        if (jdbcUrl != null) {
            properties.put("jakarta.persistence.jdbc.url", jdbcUrl);
        }
        if (username != null) {
            properties.put("jakarta.persistence.jdbc.user", username);
        }
        if (password != null) {
            properties.put("jakarta.persistence.jdbc.password", password);
        }
        this.emf = Persistence.createEntityManagerFactory("stdf-pu", properties);
    }

    public void initializeSchema() {
        // Schema is generated automatically on creation of EMF because of persistence.xml config.
        // We open and close a dummy EntityManager to trigger it.
        try (EntityManager em = emf.createEntityManager()) {
            // Trigger connection initialization
        }
    }

    public void insertLot(StdfRecord.MirRecord mir) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                StdfLotEntity lot = new StdfLotEntity(
                    mir.lotId(),
                    mir.partType(),
                    mir.jobName(),
                    mir.operatorName(),
                    Instant.ofEpochSecond(mir.startTime()),
                    Instant.ofEpochSecond(mir.setupTime())
                );
                em.merge(lot);
                em.getTransaction().commit();
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
    }

    public void insertTestResult(String lotId, StdfRecord.PtrRecord ptr) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
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
                em.persist(result);
                em.getTransaction().commit();
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }

    @Override
    public void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
