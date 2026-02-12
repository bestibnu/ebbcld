package com.cloudcity.platform.api;

import com.cloudcity.platform.api.dto.ResourceNodeRequest;
import com.cloudcity.platform.domain.CloudProvider;
import com.cloudcity.platform.domain.Org;
import com.cloudcity.platform.domain.Project;
import com.cloudcity.platform.domain.ResourceSource;
import com.cloudcity.platform.domain.ResourceType;
import com.cloudcity.platform.repository.OrgRepository;
import com.cloudcity.platform.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
class CostControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void getCostComputesSnapshot() throws Exception {
        Org org = new Org();
        org.setName("Cloud City");
        Org savedOrg = orgRepository.save(org);

        Project project = new Project();
        project.setOrg(savedOrg);
        project.setName("Costs");
        Project savedProject = projectRepository.save(project);

        ResourceNodeRequest first = new ResourceNodeRequest();
        first.setProvider(CloudProvider.AWS);
        first.setType(ResourceType.EC2);
        first.setName("app-1");
        first.setSource(ResourceSource.PLANNED);
        first.setCostEstimate(new BigDecimal("12.50"));

        ResourceNodeRequest second = new ResourceNodeRequest();
        second.setProvider(CloudProvider.AWS);
        second.setType(ResourceType.RDS);
        second.setName("db-1");
        second.setSource(ResourceSource.PLANNED);
        second.setCostEstimate(new BigDecimal("20.00"));

        mockMvc.perform(post("/api/v1/projects/{projectId}/nodes", savedProject.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/projects/{projectId}/nodes", savedProject.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/projects/{projectId}/cost", savedProject.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(32.5))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void getDeltaReturnsDifference() throws Exception {
        Org org = new Org();
        org.setName("Cloud City");
        Org savedOrg = orgRepository.save(org);

        Project project = new Project();
        project.setOrg(savedOrg);
        project.setName("Costs");
        Project savedProject = projectRepository.save(project);

        ResourceNodeRequest first = new ResourceNodeRequest();
        first.setProvider(CloudProvider.AWS);
        first.setType(ResourceType.EC2);
        first.setName("app-1");
        first.setSource(ResourceSource.PLANNED);
        first.setCostEstimate(new BigDecimal("5.00"));

        mockMvc.perform(post("/api/v1/projects/{projectId}/nodes", savedProject.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/projects/{projectId}/cost/delta", savedProject.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentTotal").value(5.0))
                .andExpect(jsonPath("$.delta").value(5.0));
    }
}
