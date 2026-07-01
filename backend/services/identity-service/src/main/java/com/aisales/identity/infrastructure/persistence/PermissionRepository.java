package com.aisales.identity.infrastructure.persistence;

import com.aisales.identity.domain.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    @Query("SELECT p.code FROM RolePermission rp JOIN rp.permission p WHERE rp.roleName IN :roleNames")
    Set<String> findPermissionCodesByRoleNames(@Param("roleNames") Set<String> roleNames);
}
