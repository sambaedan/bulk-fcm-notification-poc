package com.fcm.fcm_demo.repository;

import com.fcm.fcm_demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
}
