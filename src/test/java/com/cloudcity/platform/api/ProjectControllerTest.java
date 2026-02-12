package com.cloudcity.platform.api;

import com.cloudcity.platform.api.dto.ProjectCreateRequest;
import com.cloudcity.platform.api.dto.ProjectUpdateRequest;
import com.cloudcity.platform.domain.Org;
import com.cloudcity.platform.repository.OrgRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrgRepository orgRepository;

    @Test
    void createGetUpdateProject() throws Exception {
        Org org = new Org();
        org.setName("Cloud City");
        Org savedOrg = orgRepository.save(org);

        ProjectCreateRequest createRequest = new ProjectCreateRequest();
        createRequest.setName("MVP");
        createRequest.setDescription("Initial project");
        createRequest.setMonthlyBudget(new BigDecimal("500.00"));
        createRequest.setBudgetWarningThreshold(new BigDecimal("85.00"));

        String response = mockMvc.perform(post("/api/v1/orgs/{orgId}/projects", savedOrg.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("MVP"))
                .andExpect(jsonPath("$.monthlyBudget").value(500.0))
                .andExpect(jsonPath("$.budgetWarningThreshold").value(85.0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String projectId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/projects/{projectId}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Initial project"));

        ProjectUpdateRequest updateRequest = new ProjectUpdateRequest();
        updateRequest.setName("MVP v1");
        updateRequest.setBudgetWarningThreshold(new BigDecimal("90.00"));

        mockMvc.perform(patch("/api/v1/projects/{projectId}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("MVP v1"))
                .andExpect(jsonPath("$.budgetWarningThreshold").value(90.0));
    }
}
