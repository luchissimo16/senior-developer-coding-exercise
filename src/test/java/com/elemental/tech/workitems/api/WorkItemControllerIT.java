package com.elemental.tech.workitems.api;

// TODO: 2. Left imports as a hint.
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.elemental.tech.workitems.api.dto.CreateWorkItemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class WorkItemControllerIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void createNewWorkItem() throws Exception {
        var req = new CreateWorkItemRequest("Hello", "World", CreateWorkItemRequest.Priority.MEDIUM);

        // TODO: 1. Please identify an appropriate testing strat
        String response = "";

        // TODO: 3. enhance and add further tests
    }

    @Test
    void reqShouldReturnOK() throws Exception {
        mvc.perform(get("/api/work-items"))
                .andExpect(status().isOk());
    }
}
