package com.elemental.tech.workitems.api.dto;

import com.elemental.tech.workitems.domain.WorkItem.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateStatusRequest(@NotNull Status status, @NotNull @PositiveOrZero Long version) {
}
