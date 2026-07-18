package com.project.school_management.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.school_management.entities.RolePermission;
import com.project.school_management.enums.RoleName;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    List<RolePermission> findByRoleUuid(UUID roleUuid);

    List<RolePermission> findByRole_Name(RoleName roleName);

    boolean existsByRoleUuidAndPermission(UUID roleUuid, String permission);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RolePermission rp WHERE rp.role.uuid = :roleUuid")
    void deleteByRoleUuid(@Param("roleUuid") UUID roleUuid);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RolePermission rp WHERE rp.role.uuid = :roleUuid AND rp.permission = :permission")
    void deleteByRoleUuidAndPermission(@Param("roleUuid") UUID roleUuid, @Param("permission") String permission);
}
