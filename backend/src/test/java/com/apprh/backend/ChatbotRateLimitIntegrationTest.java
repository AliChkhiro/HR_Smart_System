package com.apprh.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.chatbot.base-url=http://127.0.0.1:1",
        "app.chatbot.rate-limit-max=2",
        "app.chatbot.rate-limit-window-seconds=60"
})
@AutoConfigureMockMvc
class ChatbotRateLimitIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void rateLimitsExcessiveRequests() throws Exception {
        String token = login();
        String body = objectMapper.writeValueAsString(
                Map.of("message", "Combien de congés approuvés ai-je ?"));

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/chatbot/chat")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/chatbot/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("CHATBOT_RATE_LIMITED"));
    }

    @Test
    void historyReadsAreNotRateLimited() throws Exception {
        String token = login();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/chatbot/messages")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    private String login() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "employe@apprh.local",
                "password", "Apprh@2026!"));
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
