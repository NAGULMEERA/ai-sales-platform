package com.aisales.identity.infrastructure.persistence;

import com.aisales.identity.domain.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {
}
