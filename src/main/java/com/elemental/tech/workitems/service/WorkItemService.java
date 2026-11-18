package com.elemental.tech.workitems.service;

import com.elemental.tech.workitems.api.dto.CreateWorkItemRequest;
import com.elemental.tech.workitems.api.dto.UpdateStatusRequest;
import com.elemental.tech.workitems.api.dto.WorkItemResponse;
import com.elemental.tech.workitems.domain.WorkItem;
import com.elemental.tech.workitems.domain.WorkItem.Status;
import com.elemental.tech.workitems.domain.WorkItem.Priority;
import com.elemental.tech.workitems.persistence.WorkItemRepository;
import jakarta.validation.Valid;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class WorkItemService {

    private final WorkItemRepository repository;

    public WorkItemService(WorkItemRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WorkItemResponse create(CreateWorkItemRequest request) {
        WorkItem workItem = new WorkItem(
                request.title(),
                request.description(),
                WorkItem.Priority.valueOf(request.priority().name())
        );

        WorkItem saved = repository.save(workItem);
        return WorkItemMapper.toResponse(saved);
    }


    @Transactional(readOnly = true)
    public List<WorkItemResponse> list(
            Status status,
            Priority priority,
            Pageable pageable) {

        Page<WorkItem> result;

        if (status != null && priority != null) {

            result = repository.findByStatusAndPriority(
                    status, priority, pageable
            );

        } else if (status != null) {

            result = repository.findByStatus(
                    status, pageable
            );

        } else if (priority != null) {

            result = repository.findByPriority(
                    priority, pageable
            );

        } else {

            result = repository.findAll(pageable);
        }

        return result.getContent().stream().map(WorkItemMapper::toResponse).toList();
    }


    @Transactional(readOnly = true)
    public WorkItemResponse get(long id) {
        WorkItem workItem = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Work item not found: " + id));

        return WorkItemMapper.toResponse(workItem);
    }

    @Transactional
    public WorkItemResponse updateStatus(long id, @Valid UpdateStatusRequest request) {
        WorkItem workItem = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Work item not found: " + id));

        if (!Objects.equals(workItem.getVersion(), request.version())) {
            throw new OptimisticLockingFailureException("Work item has been modified");
        }

        workItem.transitionTo(request.status());
        repository.flush();
        return WorkItemMapper.toResponse(workItem);
    }
}
