package com.shu.backend.domain.push.controller;

import com.shu.backend.domain.push.dto.PushTestRequest;
import com.shu.backend.domain.push.service.PushService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dev/push")
public class PushTestController {

    private final PushService pushService;

    @PostMapping("/test")
    public void testPush(@RequestBody PushTestRequest req) {
        pushService.sendToUser(
                req.getUserId(),
                req.getTitle(),
                req.getBody(),
                Map.of("type", "SYSTEM")
        );
    }
}