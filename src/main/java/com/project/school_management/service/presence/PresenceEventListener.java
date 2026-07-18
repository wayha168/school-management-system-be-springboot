package com.project.school_management.service.presence;

import java.security.Principal;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class PresenceEventListener {

    private final PresenceTracker presenceTracker;

    public PresenceEventListener(PresenceTracker presenceTracker) {
        this.presenceTracker = presenceTracker;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (user != null && sessionId != null) {
            presenceTracker.userConnected(user.getName(), sessionId);
        }
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (user != null && sessionId != null) {
            presenceTracker.userDisconnected(user.getName(), sessionId);
        }
    }
}
