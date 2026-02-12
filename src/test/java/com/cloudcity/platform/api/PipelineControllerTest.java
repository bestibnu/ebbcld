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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PipelineControllerTest {
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
    void checkPassesWithWarningInNonStrictMode() throws Exception {
        Project project = createProjectWithBudget("Warn Non Strict", "100.00", "80.00");
        createVpc(project, "85.00");

        mockMvc.perform(post("/api/v1/projects/{projectId}/pipeline/check", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"strictMode\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pass").value(true))
                .andExpect(jsonPath("$.budgetStatus").value("WARNING"))
                .andExpect(jsonPath("$.requiredApproval").value(true))
                .andExpect(jsonPath("$.terraformPlanEligible").value(true));
    }

    @Test
    void checkFailsWithWarningInStrictMode() throws Exception {
        Project project = createProjectWithBudget("Warn Strict", "100.00", "80.00");
        createVpc(project, "85.00");

        mockMvc.perform(post("/api/v1/projects/{projectId}/pipeline/check", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"strictMode\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pass").value(false))
                .andExpect(jsonPath("$.budgetStatus").value("WARNING"))
                .andExpect(jsonPath("$.requiredApproval").value(true))
                .andExpect(jsonPath("$.reason").value("Strict mode blocks WARNING budget status"));
    }

    @Test
    void checkFailsWhenTerraformPlanIsIneligibleAndWritesAuditEvent() throws Exception {
        Project project = createProjectWithBudget("No Vpc", "1000.00", "80.00");

        mockMvc.perform(post("/api/v1/projects/{projectId}/pipeline/check", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pass").value(false))
                .andExpect(jsonPath("$.terraformPlanEligible").value(false))
                .andExpect(jsonPath("$.reason").value("Terraform plan eligibility check failed"));

        Assertions.assertTrue(
                auditEventRepository.findAll().stream()
                        .anyMatch(event -> "PIPELINE_CHECK_EXECUTED".equals(event.getAction()))
        );
    }

    private Project createProjectWithBudget(String name, String monthlyBudget, String warningThreshold) {
        Org org = new Org();
        org.setName("Cloud City");
        Org savedOrg = orgRepository.save(org);

        Project project = new Project();
        project.setOrg(savedOrg);
        project.setName(name);
        project.setMonthlyBudget(new BigDecimal(monthlyBudget));
        project.setBudgetWarningThreshold(new BigDecimal(warningThreshold));
        return projectRepository.save(project);
    }

    private void createVpc(Project project, String cost) throws Exception {
        ResourceNodeRequest vpc = new ResourceNodeRequest();
        vpc.setProvider(CloudProvider.AWS);
        vpc.setType(ResourceType.VPC);
        vpc.setName("vpc-main");
        vpc.setSource(ResourceSource.PLANNED);
        vpc.setCostEstimate(new BigDecimal(cost));

        mockMvc.perform(post("/api/v1/projects/{projectId}/nodes", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vpc)))
                .andExpect(status().isCreated());
    }
}
