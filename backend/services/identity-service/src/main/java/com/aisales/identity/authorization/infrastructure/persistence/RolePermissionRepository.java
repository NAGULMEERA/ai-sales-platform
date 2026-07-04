package com.aisales.identity.authorization.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.aisales.identity.authorization.domain.entity.RolePermission;



public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {
}
