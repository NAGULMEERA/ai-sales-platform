package com.aisales.tenant.domain.entity;

import com.aisales.common.core.persistence.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role extends TenantAwareEntity {

    @Column(nullable = false)
    private String name;

    private String description;
}
