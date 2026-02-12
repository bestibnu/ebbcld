package com.cloudcity.platform.api;

import com.cloudcity.platform.api.dto.DiscoveryCreateRequest;
import com.cloudcity.platform.domain.CloudProvider;
import com.cloudcity.platform.domain.Org;
import com.cloudcity.platform.domain.Project;
import com.cloudcity.platform.repository.OrgRepository;
import com.cloudcity.platform.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class DiscoveryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void createAndFetchDiscovery() throws Exception {
        Org org = new Org();
        org.setName("Cloud City");
        Org savedOrg = orgRepository.save(org);

        Project project = new Project();
        project.setOrg(savedOrg);
        project.setName("Discovery");
        Project savedProject = projectRepository.save(project);

        DiscoveryCreateRequest request = new DiscoveryCreateRequest();
        request.setProvider(CloudProvider.AWS);
        request.setAccountId("123456789012");
        request.setRegions(List.of("us-east-1", "us-west-2"));
        request.setRoleArn("arn:aws:iam::123456789012:role/CloudCityReadOnly");
        request.setExternalId("external-id-123");

        String response = mockMvc.perform(post("/api/v1/projects/{projectId}/discoveries", savedProject.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.provider").value("AWS"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String discoveryId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/projects/{projectId}/discoveries/{discoveryId}",
                        savedProject.getId(), discoveryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("123456789012"));

        String executeResponse = mockMvc.perform(post("/api/v1/projects/{projectId}/discoveries/{discoveryId}/execute",
                        savedProject.getId(), discoveryId))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String executeStatus = objectMapper.readTree(executeResponse).get("status").asText();
        Assertions.assertTrue(
                executeStatus.equals("QUEUED") || executeStatus.equals("RUNNING") || executeStatus.equals("COMPLETED"),
                "Unexpected execute status: " + executeStatus
        );

        String statusResponse = mockMvc.perform(get("/api/v1/projects/{projectId}/discoveries/{discoveryId}/status",
                        savedProject.getId(), discoveryId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String statusValue = objectMapper.readTree(statusResponse).get("status").asText();
        Assertions.assertNotNull(statusValue);
    }
}
