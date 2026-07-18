package com.project.school_management.dto.presence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PresenceUser {

    private String email;
    private String name;
    private String role;
    private boolean online;
}
