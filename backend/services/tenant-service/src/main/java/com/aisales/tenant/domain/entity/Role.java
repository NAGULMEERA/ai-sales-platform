package com.aisales.tenant.domain.entity;

import com.aisales.common.core.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role extends BaseEntity {

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    private String description;
}
