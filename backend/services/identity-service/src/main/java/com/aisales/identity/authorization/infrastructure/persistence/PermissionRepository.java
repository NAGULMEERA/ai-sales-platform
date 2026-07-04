package com.aisales.identity.authorization.infrastructure.persistence;

import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.aisales.identity.authorization.domain.entity.Permission;
import com.aisales.identity.authorization.domain.entity.RolePermission;



public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    @Query("SELECT p.code FROM RolePermission rp JOIN rp.permission p WHERE rp.roleName IN :roleNames")
    Set<String> findPermissionCodesByRoleNames(@Param("roleNames") Set<String> roleNames);
}
