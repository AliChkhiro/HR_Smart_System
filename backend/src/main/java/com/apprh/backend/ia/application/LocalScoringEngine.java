package com.apprh.backend.ia.application;

import com.apprh.backend.ia.api.RecommendationDto;
import com.apprh.backend.ia.api.TaskRecommendRequest;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class LocalScoringEngine {

    private static final double SKILLS_WEIGHT = 0.40;
    private static final double WORKLOAD_WEIGHT = 0.25;
    private static final double AVAILABILITY_WEIGHT = 0.20;
    private static final double RELIABILITY_WEIGHT = 0.15;

    private static final Map<String, Double> WEIGHTS = Map.of(
            "skills", SKILLS_WEIGHT,
            "workload", WORKLOAD_WEIGHT,
            "availability", AVAILABILITY_WEIGHT,
            "reliability", RELIABILITY_WEIGHT);

    public List<RecommendationDto> recommend(TaskRecommendRequest request,
                                             Map<Long, EmployeeProfileData> profiles) {
        List<RecommendationDto> results = profiles.entrySet().stream()
                .map(entry -> score(request, entry.getKey(), entry.getValue()))
                .toList();
        return results.stream()
                .sorted(Comparator.comparingDouble(RecommendationDto::totalScore).reversed())
                .toList();
    }

    private RecommendationDto score(TaskRecommendRequest request, Long employeeId,
                                    EmployeeProfileData profile) {
        Criterion skills = skillsScore(request.skillIds(), profile.skillIds());
        Criterion workload = workloadScore(profile.ongoingTaskCount());
        Criterion availability = availabilityScore(request.startDate(), request.dueDate(), profile.leavePeriods());
        Criterion reliability = new Criterion(profile.reliability(),
                (int) Math.round(profile.reliability() * 100) + " % de complétion");

        List<com.apprh.backend.ia.api.CriterionScoreDto> criteria = List.of(
                toDto("skills", skills),
                toDto("workload", workload),
                toDto("availability", availability),
                toDto("reliability", reliability));

        boolean skillsUsable = request.skillIds() != null && !request.skillIds().isEmpty();
        List<Criterion> usable = List.of(skills, workload, availability, reliability);
        double totalWeight = WEIGHTS.get("skills") * (skillsUsable ? 1 : 0)
                + WORKLOAD_WEIGHT + AVAILABILITY_WEIGHT + RELIABILITY_WEIGHT;
        double total = 0;
        total += skillsUsable ? WEIGHTS.get("skills") * skills.score() : 0;
        total += WORKLOAD_WEIGHT * workload.score();
        total += AVAILABILITY_WEIGHT * availability.score();
        total += RELIABILITY_WEIGHT * reliability.score();
        double finalScore = total / totalWeight;

        String explanation = criteria.stream()
                .map(c -> c.explanation() + " (score " + c.score() + ")")
                .reduce((a, b) -> a + " ; " + b)
                .orElse("");

        return new RecommendationDto(employeeId, profile.employeeName(), round3(finalScore), criteria, explanation);
    }

    private Criterion skillsScore(List<Long> required, List<Long> owned) {
        if (required == null || required.isEmpty()) {
            return new Criterion(1.0, "aucune compétence requise");
        }
        long covered = required.stream().filter(owned::contains).count();
        return new Criterion((double) covered / required.size(),
                covered + "/" + required.size() + " compétences requises couvertes");
    }

    private Criterion workloadScore(long count) {
        if (count == 0) {
            return new Criterion(1.0, "aucune tâche en cours");
        }
        if (count <= 3) {
            return new Criterion(0.8, "charge légère (" + count + " tâches en cours)");
        }
        if (count <= 6) {
            return new Criterion(0.5, "charge modérée (" + count + " tâches en cours)");
        }
        return new Criterion(0.2, "charge élevée (" + count + " tâches en cours)");
    }

    private Criterion availabilityScore(java.time.LocalDate start, java.time.LocalDate due,
                                       List<com.apprh.backend.leaves.domain.LeaveRequest> leaves) {
        if (leaves.isEmpty() || start == null || due == null) {
            return new Criterion(1.0, "disponible sur la période");
        }
        boolean overlap = leaves.stream().anyMatch(leave ->
                !start.isAfter(leave.getEndDate()) && !due.isBefore(leave.getStartDate()));
        return overlap
                ? new Criterion(0.0, "congé validé sur la période de la tâche")
                : new Criterion(1.0, "disponible sur la période");
    }

    private com.apprh.backend.ia.api.CriterionScoreDto toDto(String name, Criterion criterion) {
        return new com.apprh.backend.ia.api.CriterionScoreDto(name, round3(criterion.score()), criterion.explanation());
    }

    private double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public record EmployeeProfileData(String employeeName, List<Long> skillIds, long ongoingTaskCount,
                                      List<com.apprh.backend.leaves.domain.LeaveRequest> leavePeriods,
                                      double reliability) {
    }

    private record Criterion(double score, String explanation) {
    }
}
