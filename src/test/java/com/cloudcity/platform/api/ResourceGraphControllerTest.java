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
import java.math.BigDecimal;
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

    @Test
    void getGraphSummaryAggregatesCountsAndCosts() throws Exception {
        Org org = new Org();
        org.setName("Cloud City");
        Org savedOrg = orgRepository.save(org);

        Project project = new Project();
        project.setOrg(savedOrg);
        project.setName("Graph Summary");
        Project savedProject = projectRepository.save(project);

        UUID projectId = savedProject.getId();

        createNode(projectId, ResourceType.VPC, "vpc-main", "us-east-1", new BigDecimal("100.00"));
        createNode(projectId, ResourceType.EC2, "app-1", "us-east-1", new BigDecimal("50.00"));
        createNode(projectId, ResourceType.RDS, "db-main", "us-west-2", new BigDecimal("200.00"));
        createNode(projectId, ResourceType.S3, "logs", null, new BigDecimal("25.00"));

        mockMvc.perform(get("/api/v1/projects/{projectId}/graph/summary", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNodes").value(4))
                .andExpect(jsonPath("$.totalEdges").value(0))
                .andExpect(jsonPath("$.totalEstimatedCost").value(375))
                .andExpect(jsonPath("$.nodeCountByType.VPC").value(1))
                .andExpect(jsonPath("$.nodeCountByType.EC2").value(1))
                .andExpect(jsonPath("$.nodeCountByType.RDS").value(1))
                .andExpect(jsonPath("$.nodeCountByType.S3").value(1))
                .andExpect(jsonPath("$.nodeCountByRegion['us-east-1']").value(2))
                .andExpect(jsonPath("$.nodeCountByRegion['us-west-2']").value(1))
                .andExpect(jsonPath("$.nodeCountByRegion.unknown").value(1))
                .andExpect(jsonPath("$.estimatedCostByType.RDS").value(200))
                .andExpect(jsonPath("$.estimatedCostByRegion['us-east-1']").value(150))
                .andExpect(jsonPath("$.topCostTypes[0].key").value("RDS"))
                .andExpect(jsonPath("$.topCostTypes[0].estimatedCost").value(200))
                .andExpect(jsonPath("$.topCostRegions[0].key").value("us-west-2"))
                .andExpect(jsonPath("$.topCostRegions[0].estimatedCost").value(200));
    }

    @Test
    void getGraphHealthDetectsOrphansAndMisconfiguredNodes() throws Exception {
        Org org = new Org();
        org.setName("Cloud City");
        Org savedOrg = orgRepository.save(org);

        Project project = new Project();
        project.setOrg(savedOrg);
        project.setName("Graph Health");
        Project savedProject = projectRepository.save(project);

        UUID projectId = savedProject.getId();

        String vpcId = createNodeAndReturnId(projectId, ResourceType.VPC, "vpc-main", "us-east-1", null);
        String subnetId = createNodeAndReturnId(projectId, ResourceType.SUBNET, "subnet-main", "us-east-1", null);
        String ec2Id = createNodeAndReturnId(projectId, ResourceType.EC2, "app-1", "us-east-1", null);
        createEdge(projectId, vpcId, subnetId);
        createEdge(projectId, subnetId, ec2Id);

        createNode(projectId, ResourceType.EC2, "orphan-ec2", "us-east-1", null);
        createNode(projectId, ResourceType.SUBNET, "bad-subnet", null, null);

        mockMvc.perform(get("/api/v1/projects/{projectId}/graph/health", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNodes").value(5))
                .andExpect(jsonPath("$.totalEdges").value(2))
                .andExpect(jsonPath("$.orphanNodeCount").value(2))
                .andExpect(jsonPath("$.misconfiguredNodeCount").value(1));
    }

    private void createNode(UUID projectId,
                            ResourceType type,
                            String name,
                            String region,
                            BigDecimal cost) throws Exception {
        createNodeAndReturnId(projectId, type, name, region, cost);
    }

    private String createNodeAndReturnId(UUID projectId,
                                         ResourceType type,
                                         String name,
                                         String region,
                                         BigDecimal cost) throws Exception {
        ResourceNodeRequest request = new ResourceNodeRequest();
        request.setProvider(CloudProvider.AWS);
        request.setType(type);
        request.setName(name);
        request.setRegion(region);
        request.setSource(ResourceSource.PLANNED);
        request.setCostEstimate(cost);

        return objectMapper.readTree(mockMvc.perform(post("/api/v1/projects/{projectId}/nodes", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asText();
    }

    private void createEdge(UUID projectId, String fromNodeId, String toNodeId) throws Exception {
        ResourceEdgeRequest edgeRequest = new ResourceEdgeRequest();
        edgeRequest.setFromNodeId(UUID.fromString(fromNodeId));
        edgeRequest.setToNodeId(UUID.fromString(toNodeId));
        edgeRequest.setRelationType(RelationType.CONTAINS);

        mockMvc.perform(post("/api/v1/projects/{projectId}/edges", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(edgeRequest)))
                .andExpect(status().isCreated());
    }
}
