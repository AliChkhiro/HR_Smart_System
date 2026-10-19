package com.apprh.backend;

import com.apprh.backend.auth.api.LoginRequest;
import com.apprh.backend.rgpd.api.ErasureRequestCreate;
import com.apprh.backend.users.api.UserCreateRequest;
import com.apprh.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditRgpdIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Apprh@2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void exportRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/rgpd/export"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportReturnsCurrentUserPersonalData() throws Exception {
        String token = login("employe@apprh.local");

        mockMvc.perform(get("/rgpd/export").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.account.email").value("employe@apprh.local"))
                .andExpect(jsonPath("$.account.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.skills").isArray())
                .andExpect(jsonPath("$.leaves").isArray())
                .andExpect(jsonPath("$.chatMessages").isArray());
    }

    @Test
    void employeeCannotReadAuditLog() throws Exception {
        String token = login("employe@apprh.local");

        mockMvc.perform(get("/audit").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportIsRecordedInAuditLog() throws Exception {
        String employeeToken = login("employe@apprh.local");
        long employeeId = loginUserId("employe@apprh.local");

        mockMvc.perform(get("/rgpd/export").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        String adminToken = login("admin@apprh.local");
        mockMvc.perform(get("/audit")
                        .param("entityType", "USER")
                        .param("entityId", String.valueOf(employeeId))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].action", org.hamcrest.Matchers.hasItem("RGPD_EXPORTED")))
                .andExpect(jsonPath("$.content[*].actorEmail", org.hamcrest.Matchers.hasItem("employe@apprh.local")));
    }

    @Test
    void employeeCannotListErasureRequests() throws Exception {
        String token = login("employe@apprh.local");

        mockMvc.perform(get("/rgpd/erasure-requests").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void erasureRequestIsValidatedByAdminAndSoftDeletesAccount() throws Exception {
        String adminToken = login("admin@apprh.local");
        String targetEmail = "rgpd.effacement@apprh.local";

        MvcResult created = mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UserCreateRequest(targetEmail, "Rgpd", "Effacement", PASSWORD, UserRole.EMPLOYEE))))
                .andExpect(status().isCreated())
                .andReturn();
        long targetId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        String targetToken = login(targetEmail);
        MvcResult requested = mockMvc.perform(post("/rgpd/erasure-request")
                        .header("Authorization", "Bearer " + targetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ErasureRequestCreate("Je ne souhaite plus utiliser le service"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        long requestId = objectMapper.readTree(requested.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/rgpd/my-erasure-requests").header("Authorization", "Bearer " + targetToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        mockMvc.perform(get("/rgpd/erasure-requests")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", org.hamcrest.Matchers.hasItem((int) requestId)));

        mockMvc.perform(post("/rgpd/erasure-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Le compte supprime (suppression douce) ne peut plus s'authentifier.
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(targetEmail, PASSWORD))))
                .andExpect(status().isUnauthorized());

        // La decision est tracee dans le journal d'audit.
        mockMvc.perform(get("/audit")
                        .param("entityType", "ERASURE_REQUEST")
                        .param("entityId", String.valueOf(requestId))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].action", org.hamcrest.Matchers.hasItem("RGPD_ERASURE_APPROVED")));

        org.assertj.core.api.Assertions.assertThat(targetId).isPositive();
    }

    @Test
    void erasureRequestCannotBeDuplicated() throws Exception {
        String adminToken = login("admin@apprh.local");
        String targetEmail = "rgpd.double@apprh.local";

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UserCreateRequest(targetEmail, "Rgpd", "Double", PASSWORD, UserRole.EMPLOYEE))))
                .andExpect(status().isCreated());

        String token = login(targetEmail);
        mockMvc.perform(post("/rgpd/erasure-request")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ErasureRequestCreate("Premiere demande"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/rgpd/erasure-request")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ErasureRequestCreate("Deuxieme demande"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ERASURE_REQUEST_PENDING"));
    }

    private String login(String email) throws Exception {
        return objectMapper.readTree(loginRaw(email)).get("accessToken").asText();
    }

    private long loginUserId(String email) throws Exception {
        return objectMapper.readTree(loginRaw(email)).get("user").get("id").asLong();
    }

    private String loginRaw(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
