package com.apprh.backend.ia.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class IaClient {

    private static final Logger log = LoggerFactory.getLogger(IaClient.class);

    private final RestClient restClient;

    public IaClient(@Value("${app.ia-service.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public List<RemoteRecommendation> recommend(RemoteRequest request) {
        RemoteResponse body = restClient.post()
                .uri("/api/v1/tasks/recommend")
                .body(request)
                .retrieve()
                .body(RemoteResponse.class);
        log.info("ia-service a classé {} employés", body == null ? 0 : body.recommendations().size());
        return body == null ? List.of() : body.recommendations();
    }

    public record RemoteRequest(TaskProfile task, List<EmployeeProfile> employees) {
    }

    public record TaskProfile(int id, List<Long> required_skill_ids, String start_date, String due_date, int priority) {
    }

    public record EmployeeProfile(long id, List<Long> skill_ids, int ongoing_task_count,
                                  List<LeavePeriod> leave_periods, double reliability) {
    }

    public record LeavePeriod(String start, String end) {
    }

    public record RemoteResponse(List<RemoteRecommendation> recommendations) {
    }

    public record RemoteRecommendation(long employee_id, double total_score,
                                       List<RemoteCriterion> criteria, String explanation) {
    }

    public record RemoteCriterion(String name, double score, String explanation) {
    }
}