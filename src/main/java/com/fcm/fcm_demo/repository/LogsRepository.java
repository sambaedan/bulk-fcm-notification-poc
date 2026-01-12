package com.fcm.fcm_demo.repository;

import com.fcm.fcm_demo.entity.Logs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LogsRepository extends JpaRepository<Logs, Integer> {

    Optional<Logs> findTopByTrackingIdAndUsernameOrderByCreatedAtDesc(String trackingId, String username);


    List<Logs> findAllByTrackingId(String trackingId);
}
