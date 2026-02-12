package com.cloudcity.platform.api;

import com.cloudcity.platform.api.dto.ResourceEdgeRequest;
import com.cloudcity.platform.api.dto.ResourceNodeRequest;
import com.cloudcity.platform.domain.CloudProvider;
import com.cloudcity.platform.domain.Org;
import com.cloudcity.platform.domain.Project;
import com.cloudcity.platform.domain.RelationType;
import com.cloudcity.platform.domain.ResourceSource;
import com.cloudcity.platform.domain.ResourceType;
import com.cloudcity.platform.repository.OrgRepository;
import com.cloudcity.platform.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
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
class ResourceGraphControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void createNodesEdgesAndFetchGraph() throws Exception {
        Org org = new Org();
        org.setName("Cloud City");
        Org savedOrg = orgRepository.save(org);

        Project project = new Project();
        project.setOrg(savedOrg);
        project.setName("Graph");
        Project savedProject = projectRepository.save(project);

        UUID projectId = savedProject.getId();

        ResourceNodeRequest regionRequest = new ResourceNodeRequest();
        regionRequest.setProvider(CloudProvider.AWS);
        regionRequest.setType(ResourceType.REGION);
        regionRequest.setName("us-east-1");
        regionRequest.setSource(ResourceSource.PLANNED);

        String regionResponse = mockMvc.perform(post("/api/v1/projects/{projectId}/nodes", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("REGION"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String regionId = objectMapper.readTree(regionResponse).get("id").asText();

        ResourceNodeRequest vpcRequest = new ResourceNodeRequest();
        vpcRequest.setProvider(CloudProvider.AWS);
        vpcRequest.setType(ResourceType.VPC);
        vpcRequest.setName("vpc-main");
        vpcRequest.setRegion("us-east-1");
        vpcRequest.setSource(ResourceSource.PLANNED);

        String vpcResponse = mockMvc.perform(post("/api/v1/projects/{projectId}/nodes", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vpcRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("VPC"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String vpcId = objectMapper.readTree(vpcResponse).get("id").asText();

        ResourceEdgeRequest edgeRequest = new ResourceEdgeRequest();
        edgeRequest.setFromNodeId(UUID.fromString(regionId));
        edgeRequest.setToNodeId(UUID.fromString(vpcId));
        edgeRequest.setRelationType(RelationType.CONTAINS);

        mockMvc.perform(post("/api/v1/projects/{projectId}/edges", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(edgeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relationType").value("CONTAINS"));

        mockMvc.perform(get("/api/v1/projects/{projectId}/graph", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes.length()").value(2))
                .andExpect(jsonPath("$.edges.length()").value(1));
    }
}
