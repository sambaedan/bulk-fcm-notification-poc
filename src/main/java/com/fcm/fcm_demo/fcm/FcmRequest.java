package com.fcm.fcm_demo.fcm;

import java.util.List;
import java.util.Map;

public record FcmRequest(List<String> tokens, String title, String body, Map<String, String> data, String username) {

}
