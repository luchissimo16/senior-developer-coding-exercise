package com.elemental.tech.workitems.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/*
    TODO: please enhance with validation
 */
public record CreateWorkItemRequest(
        String title,
        String description,
        Priority priority
) {

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH
    }
}
