package com.ntnn.database.repository;

import com.ntnn.database.entity.TestResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestResultRepository extends JpaRepository<TestResultEntity, Long> {
}
