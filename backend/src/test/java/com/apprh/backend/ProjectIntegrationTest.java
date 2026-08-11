package com.apprh.backend;

import com.apprh.backend.auth.api.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Apprh@2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void chefDeProjetCanCreateProjectAndManageMembers() throws Exception {
        String chefToken = login("chefprojet@apprh.local");
        long employeeId = firstEmployeeId(chefToken);

        MvcResult projectResult = mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + chefToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Projet Innovation " + System.nanoTime() + "\","
                                + "\"description\":\"Projet de test\",\"startDate\":\"2026-09-01\","
                                + "\"endDate\":\"2026-12-31\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andReturn();
        long projectId = id(projectResult);

        mockMvc.perform(post("/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + chefToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":" + employeeId + ",\"role\":\"MANAGER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.members[*].role").value(org.hamcrest.Matchers.hasItem("MANAGER")));

        mockMvc.perform(post("/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + chefToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":" + employeeId + ",\"role\":\"MEMBER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_MEMBER_EXISTS"));

        mockMvc.perform(patch("/projects/" + projectId + "/members/" + employeeId)
                        .header("Authorization", "Bearer " + chefToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":" + employeeId + ",\"role\":\"MEMBER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[*].role").value(org.hamcrest.Matchers.hasItem("MEMBER")));

        mockMvc.perform(get("/projects/" + projectId)
                        .header("Authorization", "Bearer " + chefToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(1));

        mockMvc.perform(delete("/projects/" + projectId + "/members/" + employeeId)
                        .header("Authorization", "Bearer " + chefToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(0));

        mockMvc.perform(patch("/projects/" + projectId)
                        .header("Authorization", "Bearer " + chefToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(delete("/projects/" + projectId)
                        .header("Authorization", "Bearer " + chefToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void employeeCannotCreateProject() throws Exception {
        String employeeToken = login("employe@apprh.local");

        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Projet Interdit " + System.nanoTime() + "\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/projects")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());
    }

    @Test
    void invalidDatesAndDuplicateNameAreRejected() throws Exception {
        String rhToken = login("rh@apprh.local");

        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + rhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Projet Dates " + System.nanoTime() + "\","
                                + "\"startDate\":\"2026-12-31\",\"endDate\":\"2026-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROJECT_INVALID_DATES"));

        String body = "{\"name\":\"Projet Doublon " + System.nanoTime() + "\"}";
        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + rhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + rhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_NAME_EXISTS"));
    }

    @Test
    void projectsAreFilterableByStatusAndPriority() throws Exception {
        String adminToken = login("admin@apprh.local");

        mockMvc.perform(get("/projects?status=PLANNED&priority=MEDIUM")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("PLANNED"))));

        mockMvc.perform(get("/projects?search=inexistant-xyz")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private long firstEmployeeId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("content").get(0).get("id").asLong();
    }

    private long id(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("accessToken").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
