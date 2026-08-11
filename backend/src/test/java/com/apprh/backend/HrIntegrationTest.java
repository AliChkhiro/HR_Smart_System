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
class HrIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Apprh@2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void rhCanManageDepartmentsButEmployeeCannot() throws Exception {
        String rhToken = login("rh@apprh.local");
        String employeeToken = login("employe@apprh.local");

        String deptJson = "{\"name\":\"Département Test RH " + System.nanoTime() + "\",\"description\":\"Unité de test\"}";

        MvcResult created = mockMvc.perform(post("/departments")
                        .header("Authorization", "Bearer " + rhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deptJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(org.hamcrest.Matchers.containsString("Département Test RH")))
                .andReturn();

        long deptId = id(created);

        mockMvc.perform(post("/departments")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deptJson))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/departments/" + deptId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deptId));
    }

    @Test
    void duplicateDepartmentNameIsRejected() throws Exception {
        String rhToken = login("rh@apprh.local");
        String body = "{\"name\":\"Département Unique " + System.nanoTime() + "\"}";

        mockMvc.perform(post("/departments")
                        .header("Authorization", "Bearer " + rhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/departments")
                        .header("Authorization", "Bearer " + rhToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_NAME_EXISTS"));
    }

    @Test
    void departmentWithEmployeesCannotBeDeleted() throws Exception {
        String adminToken = login("admin@apprh.local");

        MvcResult deptResult = mockMvc.perform(post("/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Département Occupé " + System.nanoTime() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long deptId = id(deptResult);

        long userId = createUser(adminToken, "occupe" + System.nanoTime() + "@test.local");
        long employeeId = createEmployee(adminToken, userId, deptId, "Technicien Test");

        mockMvc.perform(delete("/departments/" + deptId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_HAS_EMPLOYEES"));

        mockMvc.perform(delete("/employees/" + employeeId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/departments/" + deptId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void employeeCrudWithDepartmentAndSearch() throws Exception {
        String adminToken = login("admin@apprh.local");
        long userId = createUser(adminToken, "nouvel" + System.nanoTime() + "@test.local");

        long employeeId = createEmployee(adminToken, userId, null, "Analyste Test");

        mockMvc.perform(get("/employees?search=Analyste")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].jobTitle").value(
                        org.hamcrest.Matchers.hasItem("Analyste Test")));

        mockMvc.perform(patch("/employees/" + employeeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobTitle\":\"Analyste Senior\",\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle").value("Analyste Senior"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(post("/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId + ",\"jobTitle\":\"Doublon\",\"hireDate\":\"2026-01-01\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMPLOYEE_USER_EXISTS"));

        mockMvc.perform(delete("/employees/" + employeeId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId + ",\"jobTitle\":\"Recyclé\",\"hireDate\":\"2026-01-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobTitle").value("Recyclé"));
    }

    @Test
    void skillReferentialAndEmployeeSkillAssignment() throws Exception {
        String adminToken = login("admin@apprh.local");

        String skillBody = "{\"name\":\"Kotlin " + System.nanoTime() + "\",\"category\":\"DEVELOPPEMENT\"}";
        MvcResult skillResult = mockMvc.perform(post("/skills")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skillBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("DEVELOPPEMENT"))
                .andReturn();
        long skillId = id(skillResult);

        mockMvc.perform(post("/skills")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(skillBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SKILL_NAME_EXISTS"));

        long employeeId = firstEmployeeId(adminToken);

        mockMvc.perform(post("/employees/" + employeeId + "/skills")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":" + skillId + ",\"level\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.skills[*].skillId").value(
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.is((int) skillId))));

        mockMvc.perform(post("/employees/" + employeeId + "/skills")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":" + skillId + ",\"level\":4}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMPLOYEE_SKILL_EXISTS"));

        mockMvc.perform(patch("/employees/" + employeeId + "/skills/" + skillId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":" + skillId + ",\"level\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills[*].level").value(org.hamcrest.Matchers.hasItem(5)));

        mockMvc.perform(delete("/skills/" + skillId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SKILL_IN_USE"));

        mockMvc.perform(delete("/employees/" + employeeId + "/skills/" + skillId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/skills/" + skillId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void employeeCannotListEmployeeSkillsMutation() throws Exception {
        String employeeToken = login("employe@apprh.local");
        long employeeId = firstEmployeeId(employeeToken);

        mockMvc.perform(post("/employees/" + employeeId + "/skills")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillId\":1,\"level\":2}"))
                .andExpect(status().isForbidden());
    }

    private long createUser(String token, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"firstName\":\"Test\",\"lastName\":\"Un\","
                                + "\"password\":\"Test@1234\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result);
    }

    private long createEmployee(String token, long userId, Long departmentId, String jobTitle) throws Exception {
        String department = departmentId != null ? String.valueOf(departmentId) : "null";
        MvcResult result = mockMvc.perform(post("/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId + ",\"departmentId\":" + department
                                + ",\"jobTitle\":\"" + jobTitle + "\",\"hireDate\":\"2026-01-01\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return id(result);
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
