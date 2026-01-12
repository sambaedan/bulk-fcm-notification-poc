package com.fcm.fcm_demo.repository;

import com.fcm.fcm_demo.entity.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {

}
