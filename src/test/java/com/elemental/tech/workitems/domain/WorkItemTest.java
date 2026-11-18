package com.elemental.tech.workitems.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkItemTest {

    private WorkItem newWorkItem() {
        return new WorkItem(
                "Fix login",
                "Login is broken",
                WorkItem.Priority.HIGH
        );
    }

    @Test
    void shouldStartWithOpenStatus() {
        WorkItem item = newWorkItem();

        assertEquals(WorkItem.Status.OPEN, item.getStatus());
        assertNotNull(item.getCreatedAt());
        assertNotNull(item.getUpdatedAt());
    }

    @Test
    void shouldTransitionFromOpenToInProgress() {
        WorkItem item = newWorkItem();

        item.transitionTo(WorkItem.Status.IN_PROGRESS);

        assertEquals(
                WorkItem.Status.IN_PROGRESS,
                item.getStatus()
        );
    }

    @Test
    void shouldTransitionFromInProgressToDone() {
        WorkItem item = newWorkItem();

        item.transitionTo(WorkItem.Status.IN_PROGRESS);
        item.transitionTo(WorkItem.Status.DONE);

        assertEquals(WorkItem.Status.DONE, item.getStatus());
    }

    @Test
    void shouldRejectOpenToDone() {
        WorkItem item = newWorkItem();

        assertThrows(
                IllegalStateException.class,
                () -> item.transitionTo(WorkItem.Status.DONE)
        );

        assertEquals(WorkItem.Status.OPEN, item.getStatus());
    }

    @Test
    void shouldRejectChangesAfterDone() {
        WorkItem item = newWorkItem();

        item.transitionTo(WorkItem.Status.IN_PROGRESS);
        item.transitionTo(WorkItem.Status.DONE);

        assertThrows(
                IllegalStateException.class,
                () -> item.transitionTo(WorkItem.Status.OPEN)
        );
    }

    @Test
    void shouldRejectNullStatus() {
        WorkItem item = newWorkItem();

        assertThrows(
                NullPointerException.class,
                () -> item.transitionTo(null)
        );

        assertEquals(WorkItem.Status.OPEN, item.getStatus());
    }

    @Test
    void shouldNotModifyTimestampForSameStatus() {
        WorkItem item = newWorkItem();

        var originalUpdatedAt = item.getUpdatedAt();

        item.transitionTo(WorkItem.Status.OPEN);

        assertEquals(
                originalUpdatedAt,
                item.getUpdatedAt()
        );
    }
}
