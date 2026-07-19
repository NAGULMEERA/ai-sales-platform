package com.aisales.integration.infrastructure.persistence;

import com.aisales.integration.domain.entity.MetaPageBinding;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetaPageBindingRepository extends JpaRepository<MetaPageBinding, UUID> {

    Optional<MetaPageBinding> findByPageIdAndActiveTrue(String pageId);
}
