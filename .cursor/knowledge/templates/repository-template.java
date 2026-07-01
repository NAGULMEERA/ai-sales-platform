package com.company.platform.template.domain.repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain Repository Contract.
 * The domain depends on this interface only.
 */
public interface LeadRepository {

    LeadAggregate save(LeadAggregate aggregate);

    Optional<LeadAggregate> findById(UUID leadId);

    boolean existsById(UUID leadId);

    void delete(LeadAggregate aggregate);
}

// ---------------------------------------------------------------------
// Infrastructure Adapter (Spring Data JPA)
// ---------------------------------------------------------------------

package com.company.platform.template.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

@Repository
public class JpaLeadRepositoryAdapter implements LeadRepository {

    private final SpringDataLeadRepository repository;
    private final LeadMapper mapper;

    public JpaLeadRepositoryAdapter(SpringDataLeadRepository repository,
                                    LeadMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public LeadAggregate save(LeadAggregate aggregate) {
        LeadEntity entity = mapper.toEntity(aggregate);
        return mapper.toAggregate(repository.save(entity));
    }

    @Override
    public Optional<LeadAggregate> findById(UUID leadId) {
        return repository.findById(leadId)
                .map(mapper::toAggregate);
    }

    @Override
    public boolean existsById(UUID leadId) {
        return repository.existsById(leadId);
    }

    @Override
    public void delete(LeadAggregate aggregate) {
        repository.deleteById(aggregate.getId());
    }
}

/**
 * Spring Data repository.
 */
interface SpringDataLeadRepository
        extends org.springframework.data.jpa.repository.JpaRepository<LeadEntity, UUID> {

    Optional<LeadEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}

/**
 * Mapper contract.
 */
interface LeadMapper {

    LeadEntity toEntity(LeadAggregate aggregate);

    LeadAggregate toAggregate(LeadEntity entity);
}

// Placeholder types
class LeadAggregate {
    UUID getId() { return UUID.randomUUID(); }
}
class LeadEntity {}
