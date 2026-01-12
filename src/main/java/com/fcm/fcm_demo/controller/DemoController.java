package com.fcm.fcm_demo.controller;

import com.fcm.fcm_demo.dto.UserDTO;
import com.fcm.fcm_demo.entity.Logs;
import com.fcm.fcm_demo.entity.User;
import com.fcm.fcm_demo.entity.UserFcmToken;
import com.fcm.fcm_demo.fcm.FcmRequest;
import com.fcm.fcm_demo.kafka.NotificationProducer;
import com.fcm.fcm_demo.service.LogService;
import com.fcm.fcm_demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class DemoController {

    private final NotificationProducer producer;
    private final UserService userService;
    private final LogService logService;


    @PostMapping("/api/demo/publish")
    public ResponseEntity<?> publishAllUsers() {

        String trackingId = UUID.randomUUID().toString();

        List<User> users = userService.validateActiveUsers();
        int sentCount = 0;

        for (User user : users) {
            FcmRequest request = getFcmRequest(user, trackingId);

            if (request != null) {
                producer.publish(request);
                sentCount++;
            }
        }
        return ResponseEntity.ok(
                Map.of(
                        "trackingId", trackingId,
                        "publishedUsers", sentCount,
                        "totalUsers", users.size()
                )
        );
    }

    @GetMapping("/api/demo/status/{trackingId}")
    public ResponseEntity<?> getStatus(@PathVariable String trackingId) {
        List<Logs> log = logService.findAll(trackingId);
        return ResponseEntity.ok(log);
    }

    private static FcmRequest getFcmRequest(User user, String rand) {
        List<String> tokens = user.getTokens().stream()
                .map(UserFcmToken::getFcmToken)
                .collect(Collectors.toList());

        if (tokens.isEmpty()) return null;

        Map<String, String> data = new HashMap<>();
        data.put("type", "demo");
        data.put("priority", "high");
        data.put("userId", String.valueOf(user.getId()));
        data.put("rand", rand);

        return new FcmRequest(
                tokens,
                "Hello " + user.getUsername() + "!",
                "Your unique code for test: " + rand,
                data,
                user.getUsername()
        );
    }

    @PostMapping("/api/demo/save")
    public ResponseEntity<?> saveUser(@RequestBody UserDTO userDTO) {
        userService.save(userDTO);
        return ResponseEntity.ok(
              userDTO
        );
    }



}
