package com.elemental.tech.workitems.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "work_item")
public class WorkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Priority priority;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;


    public WorkItem(String title, String description, Priority priority) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = Status.OPEN;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void setTitle(String title) {
        this.title = title;
        touch();
    }

    public void setDescription(String description) {
        this.description = description;
        touch();
    }

    public void transitionTo(Status newStatus) {
        Objects.requireNonNull(newStatus, "Status cannot be null");

        if (this.status == newStatus) {
            return;
        }

        boolean allowed = switch (this.status) {
            case OPEN -> newStatus == Status.IN_PROGRESS ||
                    newStatus == Status.CANCELLED;

            case IN_PROGRESS -> newStatus == Status.DONE ||
                    newStatus == Status.CANCELLED;

            case DONE, CANCELLED -> false;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    "Invalid transition from " +
                            this.status + " to " + newStatus
            );
        }

        this.status = newStatus;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public enum Status {
        OPEN,
        IN_PROGRESS,
        DONE,
        CANCELLED
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH
    }
}
