package com.apprh.backend;

import com.apprh.backend.auth.api.LoginRequest;
import com.apprh.backend.auth.api.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Valide le workflow « option C » : une inscription publique crée un compte PENDING
 * qui ne peut pas se connecter, jusqu'à validation (ou refus) par un membre RH.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RegistrationApprovalIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Apprh@2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registrationCreatesPendingAccountThatCannotLoginYet() throws Exception {
        String email = newEmail();

        MvcResult registration = register(email)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(registration.getResponse().getContentAsString())
                .contains("PENDING");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ACCOUNT_PENDING"));
    }

    @Test
    void rhCanApproveRegistrationThenUserCanLogin() throws Exception {
        String email = newEmail();
        long id = registerAndGetId(email);
        String rhToken = login("rh@apprh.local");

        mockMvc.perform(get("/users").param("status", "PENDING")
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].email", hasItem(email)));

        mockMvc.perform(post("/users/" + id + "/approve")
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void rhCanRejectRegistrationThenUserIsDisabled() throws Exception {
        String email = newEmail();
        long id = registerAndGetId(email);
        String rhToken = login("rh@apprh.local");

        mockMvc.perform(post("/users/" + id + "/reject")
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ACCOUNT_DISABLED"));
    }

    @Test
    void employeeCannotApproveOrListPendingRegistrations() throws Exception {
        String email = newEmail();
        long id = registerAndGetId(email);
        String employeeToken = login("employe@apprh.local");

        mockMvc.perform(get("/users").param("status", "PENDING")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/users/" + id + "/approve")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void pendingRegistrationIsHiddenFromDefaultUserList() throws Exception {
        String email = newEmail();
        registerAndGetId(email);
        String adminToken = login("admin@apprh.local");

        mockMvc.perform(get("/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].email", not(hasItem(email))));
    }

    @Test
    void approvingAnAlreadyActiveAccountIsRejected() throws Exception {
        String email = newEmail();
        long id = registerAndGetId(email);
        String rhToken = login("rh@apprh.local");

        mockMvc.perform(post("/users/" + id + "/approve")
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/users/" + id + "/approve")
                        .header("Authorization", "Bearer " + rhToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_NOT_PENDING"));
    }

    private org.springframework.test.web.servlet.ResultActions register(String email) throws Exception {
        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new RegisterRequest(email, "Candide", "Martin", PASSWORD))));
    }

    private long registerAndGetId(String email) throws Exception {
        MvcResult result = register(email).andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String newEmail() {
        return "candidat-" + UUID.randomUUID() + "@example.com";
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
