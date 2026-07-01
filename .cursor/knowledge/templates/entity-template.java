package com.company.platform.template.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Base Entity Template
 * Equality is based on identity.
 */
public abstract class BaseEntity<ID> {

    protected final ID id;
    protected Instant createdAt;
    protected Instant updatedAt;

    protected BaseEntity(ID id) {
        this.id = Objects.requireNonNull(id);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public ID getId() {
        return id;
    }

    protected void touch() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity<?> other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

/**
 * Example child entity owned by LeadAggregate.
 */
class LeadContact extends BaseEntity<UUID> {

    private String name;
    private String phone;
    private boolean primary;

    LeadContact(UUID id, String name, String phone, boolean primary) {
        super(id);
        this.name = name;
        this.phone = phone;
        this.primary = primary;
    }

    public void changePhone(String newPhone) {
        if (newPhone == null || newPhone.isBlank()) {
            throw new IllegalArgumentException("Phone cannot be blank");
        }
        this.phone = newPhone;
        touch();
    }

    public void markPrimary() {
        this.primary = true;
        touch();
    }

    public String getName() { return name; }
    public String getPhone() { return phone; }
    public boolean isPrimary() { return primary; }
}
