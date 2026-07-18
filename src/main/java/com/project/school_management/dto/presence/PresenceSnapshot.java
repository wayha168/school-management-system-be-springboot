package com.project.school_management.dto.presence;

import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceSnapshot {

    private int onlineCount;
    private Set<String> onlineEmails;
    private List<PresenceUser> users;
    private PresenceUser changed;
}
