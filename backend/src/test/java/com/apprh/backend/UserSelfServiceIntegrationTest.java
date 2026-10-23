package com.apprh.backend;

import com.apprh.backend.auth.api.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P4 : non-regression du libre-service P3 ({@code PATCH /users/me}).
 *
 * <p>Verifie qu'un employe peut modifier uniquement son prenom et son nom,
 * que les champs sensibles transmis sont ignores, et qu'aucun acces croise
 * n'est possible.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserSelfServiceIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@apprh.local";
    private static final String DEMO_PASSWORD = "Apprh@2026!";
    private static final String NEW_USER_PASSWORD = "Test@1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void employeeCanUpdateOwnNamesAndSensitiveFieldsAreIgnored() throws Exception {
        String adminToken = login(ADMIN_EMAIL, DEMO_PASSWORD);
        String email = uniqueEmail();
        long userId = createUser(adminToken, email);
        String employeeToken = login(email, NEW_USER_PASSWORD);

        mockMvc.perform(patch("/users/me")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Nouveau\",\"lastName\":\"Nom\","
                                + "\"role\":\"ADMIN\",\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.firstName").value("Nouveau"))
                .andExpect(jsonPath("$.lastName").value("Nom"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void employeeCannotUpdateAnotherUser() throws Exception {
        String adminToken = login(ADMIN_EMAIL, DEMO_PASSWORD);
        String email = uniqueEmail();
        createUser(adminToken, email);
        String employeeToken = login(email, NEW_USER_PASSWORD);

        mockMvc.perform(patch("/users/1")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Intrusion\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotUpdateOwnProfile() throws Exception {
        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Intrusion\"}"))
                .andExpect(status().isUnauthorized());
    }

    private long createUser(String adminToken, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"firstName\":\"Self\",\"lastName\":\"Service\","
                                + "\"password\":\"" + NEW_USER_PASSWORD + "\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String uniqueEmail() {
        return "selfservice-" + System.nanoTime() + "@apprh.local";
    }
}
