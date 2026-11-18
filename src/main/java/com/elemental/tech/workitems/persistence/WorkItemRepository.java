package com.elemental.tech.workitems.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.elemental.tech.workitems.domain.WorkItem;
import com.elemental.tech.workitems.domain.WorkItem.Status;
import com.elemental.tech.workitems.domain.WorkItem.Priority;

public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {
    Page<WorkItem> findByStatus(
            Status status,
            Pageable pageable
    );

    Page<WorkItem> findByPriority(
            Priority priority,
            Pageable pageable
    );

    Page<WorkItem> findByStatusAndPriority(
            Status status,
            Priority priority,
            Pageable pageable
    );
}
