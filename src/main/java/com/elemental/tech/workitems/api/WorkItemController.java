package com.elemental.tech.workitems.api;

import com.elemental.tech.workitems.api.dto.CreateWorkItemRequest;
import com.elemental.tech.workitems.api.dto.UpdateStatusRequest;
import com.elemental.tech.workitems.api.dto.WorkItemResponse;
import com.elemental.tech.workitems.domain.WorkItem.Status;
import com.elemental.tech.workitems.domain.WorkItem.Priority;
import com.elemental.tech.workitems.service.WorkItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/work-items")
public class WorkItemController {

    private final WorkItemService service;

    public WorkItemController(WorkItemService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkItemResponse create(@Valid @RequestBody CreateWorkItemRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<WorkItemResponse> list(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Priority priority,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(status, priority, pageable);
    }

    @GetMapping("/{id}")
    public WorkItemResponse get(@PathVariable @Positive long id) {
        return service.get(id);
    }

    @PatchMapping("/{id}/status")
    public WorkItemResponse updateStatus(
            @PathVariable @Positive long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return service.updateStatus(id, request);
    }
}
