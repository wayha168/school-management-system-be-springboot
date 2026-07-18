package com.project.school_management.service.presence;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.project.school_management.dto.presence.PresenceSnapshot;
import com.project.school_management.dto.presence.PresenceUser;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.exception.UserNotFound;
import com.project.school_management.service.user.UserService;

@Service
public class PresenceTracker {

    private final Map<String, Set<String>> sessionsByEmail = new ConcurrentHashMap<>();
    private final Map<String, PresenceUser> profiles = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    public PresenceTracker(@Lazy SimpMessagingTemplate messagingTemplate, UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
    }

    public synchronized void userConnected(String email, String sessionId) {
        if (email == null || email.isBlank() || sessionId == null) {
            return;
        }
        PresenceUser profile = profiles.computeIfAbsent(email, this::loadProfile);
        sessionsByEmail.computeIfAbsent(email, key -> new CopyOnWriteArraySet<>()).add(sessionId);
        broadcast(profile.toBuilder().online(true).build());
    }

    public synchronized void userDisconnected(String email, String sessionId) {
        if (email == null || sessionId == null) {
            return;
        }
        Set<String> sessions = sessionsByEmail.get(email);
        if (sessions == null) {
            return;
        }
        sessions.remove(sessionId);
        if (sessions.isEmpty()) {
            sessionsByEmail.remove(email);
            PresenceUser profile = profiles.getOrDefault(email, PresenceUser.builder().email(email).build());
            broadcast(PresenceUser.builder()
                    .email(profile.getEmail())
                    .name(profile.getName())
                    .role(profile.getRole())
                    .online(false)
                    .build());
        }
    }

    public PresenceSnapshot snapshot() {
        Set<String> onlineEmails = Set.copyOf(sessionsByEmail.keySet());
        List<PresenceUser> users = onlineEmails.stream()
                .map(email -> {
                    PresenceUser profile = profiles.computeIfAbsent(email, this::loadProfile);
                    return PresenceUser.builder()
                            .email(profile.getEmail())
                            .name(profile.getName())
                            .role(profile.getRole())
                            .online(true)
                            .build();
                })
                .sorted((a, b) -> a.getEmail().compareToIgnoreCase(b.getEmail()))
                .toList();
        return PresenceSnapshot.builder()
                .onlineCount(onlineEmails.size())
                .onlineEmails(onlineEmails)
                .users(users)
                .build();
    }

    public boolean isOnline(String email) {
        return email != null && sessionsByEmail.containsKey(email);
    }

    private void broadcast(PresenceUser changed) {
        PresenceSnapshot snapshot = snapshot();
        PresenceSnapshot payload = PresenceSnapshot.builder()
                .onlineCount(snapshot.getOnlineCount())
                .onlineEmails(snapshot.getOnlineEmails())
                .users(snapshot.getUsers())
                .changed(changed)
                .build();
        messagingTemplate.convertAndSend("/topic/presence", payload);
    }

    private PresenceUser loadProfile(String email) {
        try {
            DataUser user = userService.getAccountByEmail(email);
            return PresenceUser.builder()
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRoleLabel())
                    .online(false)
                    .build();
        } catch (UserNotFound ex) {
            return PresenceUser.builder().email(email).name(email).role("UNKNOWN").online(false).build();
        }
    }
}
