package com.elemental.tech.workitems.api;

import com.elemental.tech.workitems.api.dto.CreateWorkItemRequest;
import com.elemental.tech.workitems.api.dto.UpdateStatusRequest;
import com.elemental.tech.workitems.domain.WorkItem.Status;
import com.elemental.tech.workitems.persistence.WorkItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkItemControllerIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    WorkItemRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void createNewWorkItem() throws Exception {
        var req = new CreateWorkItemRequest("Hello", "World", CreateWorkItemRequest.Priority.MEDIUM);

        String response = mvc.perform(
                        post("/api/work-items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var json = objectMapper.readTree(response);

        assertTrue(json.get("id").asLong() > 0);
        assertEquals("Hello", json.get("title").asText());
        assertEquals("World", json.get("description").asText());
        assertEquals("MEDIUM", json.get("priority").asText());
        assertEquals("OPEN", json.get("status").asText());
    }

    @Test
    void reqShouldReturnOK() throws Exception {
        mvc.perform(get("/api/work-items"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectBlankTitle() throws Exception {
        var req = new CreateWorkItemRequest(
                "",
                "Description",
                CreateWorkItemRequest.Priority.HIGH
        );

        mvc.perform(
                        post("/api/work-items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andExpect(status().isBadRequest());

        assertEquals(0, repository.count());
    }

    @Test
    void shouldRejectNullPriority() throws Exception {
        var req = new CreateWorkItemRequest(
                "Hello",
                "Description",
                null
        );

        mvc.perform(
                        post("/api/work-items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldTransitionOpenToInProgressToDone() throws Exception {
        JsonNode created = createWorkItem("Flow", "Happy path", CreateWorkItemRequest.Priority.MEDIUM);
        long id = created.get("id").asLong();
        long version = created.get("version").asLong();

        String inProgressResponse = mvc.perform(
                        patch("/api/work-items/{id}/status", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new UpdateStatusRequest(Status.IN_PROGRESS, version)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long nextVersion = objectMapper.readTree(inProgressResponse).get("version").asLong();

        mvc.perform(
                        patch("/api/work-items/{id}/status", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new UpdateStatusRequest(Status.DONE, nextVersion)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void shouldRejectIllegalStatusTransition() throws Exception {
        JsonNode created = createWorkItem("Illegal", "Cannot skip", CreateWorkItemRequest.Priority.LOW);
        long id = created.get("id").asLong();
        long version = created.get("version").asLong();

        mvc.perform(
                        patch("/api/work-items/{id}/status", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new UpdateStatusRequest(Status.DONE, version)))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldRejectStaleVersion() throws Exception {
        JsonNode created = createWorkItem("Stale", "Concurrency", CreateWorkItemRequest.Priority.HIGH);
        long id = created.get("id").asLong();
        long version = created.get("version").asLong();

        mvc.perform(
                        patch("/api/work-items/{id}/status", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new UpdateStatusRequest(Status.IN_PROGRESS, version)))
                )
                .andExpect(status().isOk());

        mvc.perform(
                        patch("/api/work-items/{id}/status", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new UpdateStatusRequest(Status.DONE, version)))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldGetWorkItemById() throws Exception {
        JsonNode created = createWorkItem("Fetch me", "By id", CreateWorkItemRequest.Priority.MEDIUM);
        long id = created.get("id").asLong();

        mvc.perform(get("/api/work-items/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Fetch me"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void shouldReturnNotFoundForMissingId() throws Exception {
        mvc.perform(get("/api/work-items/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldFilterByStatus() throws Exception {
        createWorkItem("Open", "a", CreateWorkItemRequest.Priority.HIGH);
        JsonNode inProgress = createWorkItem("Started", "b", CreateWorkItemRequest.Priority.MEDIUM);
        patchStatus(inProgress, Status.IN_PROGRESS);

        mvc.perform(get("/api/work-items").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].title").value("Open"));
    }

    @Test
    void shouldFilterByPriority() throws Exception {
        createWorkItem("High", "a", CreateWorkItemRequest.Priority.HIGH);
        createWorkItem("Low", "b", CreateWorkItemRequest.Priority.LOW);

        mvc.perform(get("/api/work-items").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].title").value("High"));
    }

    @Test
    void shouldFilterByStatusAndPriority() throws Exception {
        createWorkItem("Open High", "a", CreateWorkItemRequest.Priority.HIGH);
        createWorkItem("Open Low", "b", CreateWorkItemRequest.Priority.LOW);

        mvc.perform(get("/api/work-items")
                        .param("status", "OPEN")
                        .param("priority", "LOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Open Low"));
    }

    @Test
    void shouldPaginateListResults() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createWorkItem("Item " + i, "pageable", CreateWorkItemRequest.Priority.MEDIUM);
        }

        String page0 = mvc.perform(
                        get("/api/work-items")
                                .param("page", "0")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode firstPage = objectMapper.readTree(page0);
        assertEquals(2, firstPage.size());

        String page1 = mvc.perform(
                        get("/api/work-items")
                                .param("page", "1")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode secondPage = objectMapper.readTree(page1);
        assertEquals(2, secondPage.size());

        String page2 = mvc.perform(
                        get("/api/work-items")
                                .param("page", "2")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode thirdPage = objectMapper.readTree(page2);
        assertEquals(1, thirdPage.size());
    }

    private JsonNode createWorkItem(
            String title,
            String description,
            CreateWorkItemRequest.Priority priority) throws Exception {
        var req = new CreateWorkItemRequest(title, description, priority);

        String response = mvc.perform(
                        post("/api/work-items")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private void patchStatus(JsonNode workItem, Status status) throws Exception {
        mvc.perform(
                        patch("/api/work-items/{id}/status", workItem.get("id").asLong())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new UpdateStatusRequest(status, workItem.get("version").asLong())))
                )
                .andExpect(status().isOk());
    }
}
