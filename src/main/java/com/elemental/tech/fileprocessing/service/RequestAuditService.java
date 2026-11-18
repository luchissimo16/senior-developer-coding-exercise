package com.elemental.tech.fileprocessing.service;

import com.elemental.tech.fileprocessing.domain.RequestAudit;
import com.elemental.tech.fileprocessing.persistence.RequestAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestAuditService {

    private final RequestAuditRepository repository;

    public RequestAuditService(RequestAuditRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(RequestAudit audit) {
        repository.save(audit);
    }
}
