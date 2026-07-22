package com.project.school_management.controller.api;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.project.school_management.dto.classroom.CallSignalMessage;

@Controller
public class VideoCallWsController {

    private final SimpMessagingTemplate messagingTemplate;

    public VideoCallWsController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/call/signal")
    public void signal(@Payload CallSignalMessage message) {
        if (message == null || message.getRoomCode() == null || message.getRoomCode().isBlank()) {
            return;
        }
        String room = message.getRoomCode().trim().toUpperCase();
        message.setRoomCode(room);
        messagingTemplate.convertAndSend("/topic/call/" + room, message);
    }
}
