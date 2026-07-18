package com.project.school_management.controller.api;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.project.school_management.service.presence.PresenceTracker;

/**
 * Client pings after STOMP connect so presence is registered without a channel interceptor
 * (avoids a WebSocket config ↔ PresenceTracker bean cycle).
 */
@Controller
public class PresenceWsController {

    private final PresenceTracker presenceTracker;

    public PresenceWsController(PresenceTracker presenceTracker) {
        this.presenceTracker = presenceTracker;
    }

    @MessageMapping("/presence/ping")
    public void ping(Principal principal, SimpMessageHeaderAccessor accessor) {
        if (principal == null || accessor.getSessionId() == null) {
            return;
        }
        presenceTracker.userConnected(principal.getName(), accessor.getSessionId());
    }
}
