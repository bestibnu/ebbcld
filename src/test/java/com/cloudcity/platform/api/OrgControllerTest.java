package com.cloudcity.platform.api;

import com.cloudcity.platform.api.dto.OrgCreateRequest;
import com.cloudcity.platform.api.dto.OrgMemberCreateRequest;
import com.cloudcity.platform.domain.Org;
import com.cloudcity.platform.domain.OrgRole;
import com.cloudcity.platform.domain.User;
import com.cloudcity.platform.repository.OrgRepository;
import com.cloudcity.platform.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class OrgControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrgRepository orgRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createOrg() throws Exception {
        OrgCreateRequest request = new OrgCreateRequest();
        request.setName("Cloud City");

        mockMvc.perform(post("/api/v1/orgs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cloud City"));
    }

    @Test
    void addMember() throws Exception {
        Org org = new Org();
        org.setName("Cloud City");
        Org savedOrg = orgRepository.save(org);

        User user = new User();
        user.setEmail("member@example.com");
        user.setName("Member");
        userRepository.save(user);

        OrgMemberCreateRequest request = new OrgMemberCreateRequest();
        request.setEmail("member@example.com");
        request.setRole(OrgRole.ADMIN);

        mockMvc.perform(post("/api/v1/orgs/{orgId}/members", savedOrg.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orgId").value(savedOrg.getId().toString()))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
