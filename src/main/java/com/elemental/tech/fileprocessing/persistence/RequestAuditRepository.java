package com.elemental.tech.fileprocessing.persistence;


import com.elemental.tech.fileprocessing.domain.RequestAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RequestAuditRepository
        extends JpaRepository<RequestAudit, UUID> {
}
