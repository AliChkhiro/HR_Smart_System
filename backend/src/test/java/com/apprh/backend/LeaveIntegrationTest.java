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
class LeaveIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Apprh@2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void employeeCreatesOwnLeaveAndSeesOnlyOwn() throws Exception {
        String employeeToken = login("employe@apprh.local");
        String rhToken = login("rh@apprh.local");

        long myLeaveId = createLeave(employeeToken, "2027-02-01", "2027-02-05");
        long rhLeaveId = createLeave(rhToken, "2027-06-01", "2027-06-05");

        mockMvc.perform(get("/leaves")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.equalTo(Math.toIntExact(myLeaveId)))))
                .andExpect(jsonPath("$.content[*].id").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.equalTo(Math.toIntExact(rhLeaveId))))))
                .andExpect(jsonPath("$.content[*].employeeName").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("Yassine Idrissi"))));

        mockMvc.perform(get("/leaves")
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.equalTo(Math.toIntExact(rhLeaveId)))));
    }

    @Test
    void overlappingLeaveRejected() throws Exception {
        String employeeToken = login("employe@apprh.local");
        createLeave(employeeToken, "2027-04-01", "2027-04-05");

        mockMvc.perform(post("/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ANNUAL\",\"startDate\":\"2027-04-03\",\"endDate\":\"2027-04-10\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LEAVE_OVERLAP"));
    }

    @Test
    void invalidDatesRejected() throws Exception {
        String employeeToken = login("employe@apprh.local");

        mockMvc.perform(post("/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ANNUAL\",\"startDate\":\"2027-05-10\",\"endDate\":\"2027-05-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LEAVE_INVALID_DATES"));

        mockMvc.perform(post("/leaves")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ANNUAL\",\"startDate\":\"2027-05-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void employeeCannotReviewOrCancelOthersLeave() throws Exception {
        String rhToken = login("rh@apprh.local");
        String employeeToken = login("employe@apprh.local");
        long rhLeaveId = createLeave(rhToken, "2027-07-01", "2027-07-05");

        mockMvc.perform(patch("/leaves/" + rhLeaveId + "/review")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/leaves/" + rhLeaveId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void managerReviewsLeaveAndNotifiesEmployee() throws Exception {
        String employeeToken = login("employe@apprh.local");
        String rhToken = login("rh@apprh.local");
        long myLeaveId = createLeave(employeeToken, "2027-05-01", "2027-05-05");

        mockMvc.perform(patch("/leaves/" + myLeaveId + "/review")
                        .header("Authorization", "Bearer " + rhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"comment\":\"Accordé\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewerName").value("Karim Benali"))
                .andExpect(jsonPath("$.reviewComment").value("Accordé"));

        mockMvc.perform(patch("/leaves/" + myLeaveId + "/review")
                        .header("Authorization", "Bearer " + rhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LEAVE_NOT_PENDING"));

        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].type").value(
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.is("LEAVE_REVIEW"))));
    }

    @Test
    void leaveCreationNotifiesManagers() throws Exception {
        String employeeToken = login("employe@apprh.local");
        String adminToken = login("admin@apprh.local");
        createLeave(employeeToken, "2027-08-01", "2027-08-05");

        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].type").value(
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.is("LEAVE_REQUEST"))));
    }

    @Test
    void employeeCancelsOwnPendingLeave() throws Exception {
        String employeeToken = login("employe@apprh.local");
        long myLeaveId = createLeave(employeeToken, "2027-03-01", "2027-03-05");

        mockMvc.perform(delete("/leaves/" + myLeaveId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/leaves/" + myLeaveId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void taskAssignmentCreatesNotificationAndReadFlowWorks() throws Exception {
        String chefToken = login("chefprojet@apprh.local");
        String employeeToken = login("employe@apprh.local");

        long projectId = createProject(chefToken);
        long employeeId = employeeIdOf(chefToken, "employe@apprh.local");
        long taskId = createTask(chefToken, projectId, employeeId, "Tâche notifiée " + System.nanoTime());

        MvcResult notifResult = mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].type").value(
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.is("TASK_ASSIGNED"))))
                .andExpect(jsonPath("$.content[0].message").value(
                        org.hamcrest.Matchers.containsString("Tâche notifiée")))
                .andReturn();
        long notificationId = objectMapper.readTree(notifResult.getResponse().getContentAsString())
                .get("content").get(0).get("id").asLong();

        mockMvc.perform(patch("/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/notifications/unread-count")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0)));

        mockMvc.perform(patch("/notifications/read-all")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/notifications/unread-count")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }

    private long createLeave(String token, String start, String end) throws Exception {
        MvcResult result = mockMvc.perform(post("/leaves")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ANNUAL\",\"startDate\":\"" + start + "\",\"endDate\":\""
                                + end + "\",\"reason\":\"Test\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result);
    }

    private long createProject(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Projet Congés " + System.nanoTime() + "\"}"))
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
