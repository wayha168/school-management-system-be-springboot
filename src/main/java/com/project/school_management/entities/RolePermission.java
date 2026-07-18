package com.project.school_management.entities;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "role_permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"role_uuid", "permission"}))
@Getter
@Setter
@NoArgsConstructor
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_uuid", nullable = false)
    private Role role;

    @Column(name = "permission", nullable = false, length = 100)
    private String permission;

    public RolePermission(Role role, String permission) {
        this.role = role;
        this.permission = permission;
    }
}
