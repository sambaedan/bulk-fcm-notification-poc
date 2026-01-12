package com.fcm.fcm_demo.service;

import com.fcm.fcm_demo.dto.UserDTO;
import com.fcm.fcm_demo.entity.User;
import com.fcm.fcm_demo.entity.UserFcmToken;
import com.fcm.fcm_demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> validateActiveUsers(){
        return userRepository.findAll().stream().filter(User::isActive).toList();
    }

    public void save(UserDTO userDTO){
        User user = new User();
        user.setActive(userDTO.isActive());
        user.setUsername(userDTO.getUsername());
        user.setTokens(mapToUserFcmTokens(user, userDTO.getTokens()));
        user.setMobileNumber(userDTO.getMobileNumber());
        userRepository.save(user);
    }

    private List<UserFcmToken> mapToUserFcmTokens(User user, List<String> tokens){
        List<UserFcmToken> userFcmTokens = new ArrayList<>();
        tokens.forEach(token -> {
            UserFcmToken userFcmToken = new UserFcmToken();
            userFcmToken.setUser(user);
            userFcmToken.setFcmToken(token);
            userFcmTokens.add(userFcmToken);
        });
        return userFcmTokens;
    }
}
