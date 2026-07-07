package com.ntnn.database.repository;

import com.ntnn.database.entity.StdfLotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StdfLotRepository extends JpaRepository<StdfLotEntity, String> {
}
