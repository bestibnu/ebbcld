package com.cloudcity.platform.api;

import com.cloudcity.platform.api.dto.ResourceNodeRequest;
import com.cloudcity.platform.domain.CloudProvider;
import com.cloudcity.platform.domain.Org;
import com.cloudcity.platform.domain.Project;
import com.cloudcity.platform.domain.ResourceSource;
import com.cloudcity.platform.domain.ResourceType;
import com.cloudcity.platform.repository.AuditEventRepository;
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
class TerraformExportControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void createAndFetchExport() throws Exception {
        Org org = new Org();
        org.setName("Cloud City");
        Org savedOrg = orgRepository.save(org);

        Project project = new Project();
        project.setOrg(savedOrg);
        project.setName("Terraform");
        Project savedProject = projectRepository.save(project);

        ResourceNodeRequest vpc = new ResourceNodeRequest();
        vpc.setProvider(CloudProvider.AWS);
        vpc.setType(ResourceType.VPC);
        vpc.setName("vpc-main");
        vpc.setSource(ResourceSource.PLANNED);

        mockMvc.perform(post("/api/v1/projects/{projectId}/nodes", savedProject.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vpc)))
                .andExpect(status().isCreated());

        String response = mockMvc.perform(post("/api/v1/projects/{projectId}/terraform/export", savedProject.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.artifactPath").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String exportId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/projects/{projectId}/terraform/export/{exportId}",
                        savedProject.getId(), exportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void exportRequiresVpc() throws Exception {
        Org org = new Org();
        org.setName("Cloud City");
        Org savedOrg = orgRepository.save(org);

        Project project = new Project();
        project.setOrg(savedOrg);
        project.setName("Terraform");
        Project savedProject = projectRepository.save(project);

        mockMvc.perform(post("/api/v1/projects/{projectId}/terraform/export", savedProject.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void planApproveApplyFlow() throws Exception {
        Org org = new Org();
        org.setName("Cloud City");
        Org savedOrg = orgRepository.save(org);

        Project project = new Project();
        project.setOrg(savedOrg);
        project.setName("Terraform Plan");
        project.setMonthlyBudget(new BigDecimal("1000.00"));
        project.setBudgetWarningThreshold(new BigDecimal("80.00"));
        Project savedProject = projectRepository.save(project);

        ResourceNodeRequest vpc = new ResourceNodeRequest();
        vpc.setProvider(CloudProvider.AWS);
        vpc.setType(ResourceType.VPC);
        vpc.setName("vpc-main");
        vpc.setSource(ResourceSource.PLANNED);
        vpc.setCostEstimate(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/v1/projects/{projectId}/nodes", savedProject.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vpc)))
                .andExpect(status().isCreated());

        String planResponse = mockMvc.perform(post("/api/v1/projects/{projectId}/terraform/plan", savedProject.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.summaryJson").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String exportId = objectMapper.readTree(planResponse).get("id").asText();

        mockMvc.perform(post("/api/v1/projects/{projectId}/terraform/{exportId}/approve", savedProject.getId(), exportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true,\"reason\":\"reviewed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(post("/api/v1/projects/{projectId}/terraform/{exportId}/apply", savedProject.getId(), exportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void applyRequiresApproval() throws Exception {
        Org org = new Org();
        org.setName("Cloud City");
        Org savedOrg = orgRepository.save(org);

        Project project = new Project();
        project.setOrg(savedOrg);
        project.setName("Terraform Plan");
        Project savedProject = projectRepository.save(project);

        ResourceNodeRequest vpc = new ResourceNodeRequest();
        vpc.setProvider(CloudProvider.AWS);
        vpc.setType(ResourceType.VPC);
        vpc.setName("vpc-main");
        vpc.setSource(ResourceSource.PLANNED);

        mockMvc.perform(post("/api/v1/projects/{projectId}/nodes", savedProject.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vpc)))
                .andExpect(status().isCreated());

        String planResponse = mockMvc.perform(post("/api/v1/projects/{projectId}/terraform/plan", savedProject.getId()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String exportId = objectMapper.readTree(planResponse).get("id").asText();

        mockMvc.perform(post("/api/v1/projects/{projectId}/terraform/{exportId}/apply", savedProject.getId(), exportId))
                .andExpect(status().isConflict());
    }
}
