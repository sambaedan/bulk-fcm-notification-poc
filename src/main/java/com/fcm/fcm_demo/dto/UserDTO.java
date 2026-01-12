package com.fcm.fcm_demo.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserDTO {
    private String username;
    private List<String> tokens;
    private boolean active;
    private String mobileNumber;

}
