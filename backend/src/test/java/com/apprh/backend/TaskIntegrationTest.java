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
class TaskIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Apprh@2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void chefDeProjetCanCreateTasksAndManageAssignees() throws Exception {
        String chefToken = login("chefprojet@apprh.local");
        long projectId = createProject(chefToken);
        long employeeId = firstEmployeeId(chefToken);
        long skillId = firstSkillId(chefToken);

        MvcResult taskResult = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + chefToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Développer le module RH " + System.nanoTime() + "\","
                                + "\"description\":\"Implémentation CRUD\",\"projectId\":" + projectId + ","
                                + "\"assigneeId\":" + employeeId + ",\"skillIds\":[" + skillId + "],"
                                + "\"priority\":\"HIGH\","
                                + "\"estimatedHours\":8,\"startDate\":\"2026-09-01\",\"dueDate\":\"2026-09-15\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.skillIds[0]").value(Math.toIntExact(skillId)))
                .andExpect(jsonPath("$.projectName").value(org.hamcrest.Matchers.containsString("Projet Kanban")))
                .andReturn();
        long taskId = id(taskResult);

        mockMvc.perform(patch("/tasks/" + taskId)
                        .header("Authorization", "Bearer " + chefToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estimatedHours\":12,\"priority\":\"URGENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedHours").value(12))
                .andExpect(jsonPath("$.priority").value("URGENT"));

        mockMvc.perform(delete("/tasks/" + taskId)
                        .header("Authorization", "Bearer " + chefToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void assigneeCanMoveOwnTaskInKanban() throws Exception {
        String chefToken = login("chefprojet@apprh.local");
        String employeeToken = login("employe@apprh.local");
        long projectId = createProject(chefToken);
        long employeeId = employeeIdOf(chefToken, "employe@apprh.local");

        long taskId = createTask(chefToken, projectId, employeeId, "Tâche Kanban " + System.nanoTime());

        mockMvc.perform(patch("/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(patch("/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void employeeCannotMoveTaskAssignedToSomeoneElse() throws Exception {
        String chefToken = login("chefprojet@apprh.local");
        String rhToken = login("rh@apprh.local");
        String employeeToken = login("employe@apprh.local");
        long projectId = createProject(chefToken);
        long rhEmployeeId = employeeIdOf(rhToken, "rh@apprh.local");

        long taskId = createTask(chefToken, projectId, rhEmployeeId, "Tâche d'un autre " + System.nanoTime());

        mockMvc.perform(patch("/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TASK_NOT_YOURS"));

        mockMvc.perform(patch("/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + rhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void employeeCannotCreateOrEditTasks() throws Exception {
        String employeeToken = login("employe@apprh.local");

        mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Interdit\",\"projectId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tasksAreFilterableAndInvalidDatesRejected() throws Exception {
        String adminToken = login("admin@apprh.local");
        long projectId = createProject(adminToken);

        mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dates KO\",\"projectId\":" + projectId + ","
                                + "\"startDate\":\"2026-12-31\",\"dueDate\":\"2026-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TASK_INVALID_DATES"));

        mockMvc.perform(get("/tasks?status=DONE&dueTo=2030-01-01")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("DONE"))));

        mockMvc.perform(get("/tasks?search=inexistant-xyz")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private long createProject(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Projet Kanban " + System.nanoTime() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result);
    }

    private long createTask(String token, long projectId, long assigneeId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"projectId\":" + projectId
                                + ",\"assigneeId\":" + assigneeId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result);
    }

    private long employeeIdOf(String token, String email) throws Exception {
        MvcResult result = mockMvc.perform(get("/employees?search=" + email)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("content").get(0).get("id").asLong();
    }

    private long firstEmployeeId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("content").get(0).get("id").asLong();
    }

    private long firstSkillId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/skills")
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
