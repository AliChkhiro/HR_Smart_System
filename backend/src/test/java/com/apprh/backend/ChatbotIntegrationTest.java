package com.apprh.backend;

import com.apprh.backend.employees.infrastructure.EmployeeRepository;
import com.apprh.backend.leaves.domain.LeaveStatus;
import com.apprh.backend.leaves.infrastructure.LeaveRequestRepository;
import com.apprh.backend.users.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.chatbot.base-url=http://127.0.0.1:1")
@AutoConfigureMockMvc
class ChatbotIntegrationTest extends AbstractIntegrationTest {

    private static final String EMPLOYEE_EMAIL = "employe@apprh.local";
    private static final String PASSWORD = "Apprh@2026!";
    private static final MediaType NDJSON = MediaType.parseMediaType("application/x-ndjson");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Test
    void chatRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/chatbot/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Bonjour\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsEmptyMessage() throws Exception {
        String token = login();
        mockMvc.perform(post("/chatbot/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidHistoryRole() throws Exception {
        String token = login();
        String body = objectMapper.writeValueAsString(Map.of(
                "message", "Bonjour",
                "history", List.of(Map.of("role", "system", "content", "x"))));

        mockMvc.perform(post("/chatbot/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptsHistoryPayload() throws Exception {
        String token = login();
        String body = objectMapper.writeValueAsString(Map.of(
                "message", "Et pour mes vacances ?",
                "history", List.of(
                        Map.of("role", "user", "content", "Bonjour"),
                        Map.of("role", "assistant", "content", "Bonjour, comment puis-je aider ?"))));

        mockMvc.perform(post("/chatbot/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").exists());
    }

    @Test
    void deterministicLeaveCountMatchesDatabase() throws Exception {
        String token = login();
        long approved = approvedLeaveCount();

        mockMvc.perform(post("/chatbot/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("message", "Combien de congés approuvés ai-je ?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiAvailable").value(true))
                .andExpect(jsonPath("$.reply", containsString(String.valueOf(approved))));
    }

    @Test
    void chatFallsBackWhenIaServiceUnavailable() throws Exception {
        String token = login();

        mockMvc.perform(post("/chatbot/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("message", "Peux-tu me parler de la politique de télétravail ?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiAvailable").value(false))
                .andExpect(jsonPath("$.reply", containsString("indisponible")));
    }

    @Test
    void streamFallsBackWhenIaServiceUnavailable() throws Exception {
        String token = login();

        mockMvc.perform(post("/chatbot/chat/stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("message", "Peux-tu me parler de la politique de télétravail ?"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/x-ndjson")))
                .andExpect(content().string(containsString("\"type\":\"fallback\"")));
    }

    @Test
    void streamReturnsDeterministicAnswerForCountQuestion() throws Exception {
        String token = login();
        long approved = approvedLeaveCount();

        mockMvc.perform(post("/chatbot/chat/stream")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("message", "Combien de congés approuvés ai-je ?"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"type\":\"token\"")))
                .andExpect(content().string(containsString(String.valueOf(approved))))
                .andExpect(content().string(containsString("\"type\":\"done\"")));
    }

    private long approvedLeaveCount() {
        Long userId = userRepository.findByEmailAndDeletedAtIsNull(EMPLOYEE_EMAIL).orElseThrow().getId();
        Long employeeId = employeeRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow().getId();
        return leaveRequestRepository.findByEmployeeIdAndDeletedAtIsNull(employeeId).stream()
                .filter(leave -> leave.getStatus() == LeaveStatus.APPROVED)
                .count();
    }

    private String login() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", EMPLOYEE_EMAIL,
                "password", PASSWORD));
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
