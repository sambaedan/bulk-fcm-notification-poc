package com.fcm.fcm_demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_fcm_tokens")
@Data
public class UserFcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String fcmToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

}
