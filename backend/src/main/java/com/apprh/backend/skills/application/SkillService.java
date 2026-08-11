package com.apprh.backend.skills.application;

import com.apprh.backend.common.exception.ApiException;
import com.apprh.backend.skills.api.SkillRequest;
import com.apprh.backend.skills.api.SkillResponse;
import com.apprh.backend.skills.domain.Skill;
import com.apprh.backend.skills.infrastructure.EmployeeSkillRepository;
import com.apprh.backend.skills.infrastructure.SkillMapper;
import com.apprh.backend.skills.infrastructure.SkillRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final SkillMapper skillMapper;
    private final EmployeeSkillRepository employeeSkillRepository;

    @Transactional(readOnly = true)
    public Page<SkillResponse> list(String search, String category, Pageable pageable) {
        return skillRepository.findAll(filter(search, category), pageable)
                .map(this::withEmployeeCount);
    }

    @Transactional
    public SkillResponse create(SkillRequest request) {
        String name = request.name().trim();
        checkNameAvailable(name);
        Skill skill = Skill.builder()
                .name(name)
                .category(request.category().trim().toUpperCase(Locale.ROOT))
                .build();
        return withEmployeeCount(skillRepository.save(skill));
    }

    @Transactional
    public SkillResponse update(Long id, SkillRequest request) {
        Skill skill = findActive(id);
        if (request.name() != null && !request.name().isBlank() && !request.name().trim().equalsIgnoreCase(skill.getName())) {
            checkNameAvailable(request.name().trim());
            skill.setName(request.name().trim());
        }
        if (request.category() != null && !request.category().isBlank()) {
            skill.setCategory(request.category().trim().toUpperCase(Locale.ROOT));
        }
        return withEmployeeCount(skillRepository.save(skill));
    }

    @Transactional
    public void delete(Long id) {
        Skill skill = findActive(id);
        long linked = employeeSkillRepository.countBySkillId(id);
        if (linked > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SKILL_IN_USE",
                    "Cette compÃ©tence est encore associÃ©e Ã  " + linked + " employÃ©(s)");
        }
        skill.setDeletedAt(Instant.now());
        skillRepository.save(skill);
    }

    private Skill findActive(Long id) {
        return skillRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SKILL_NOT_FOUND", "CompÃ©tence introuvable"));
    }

    private void checkNameAvailable(String name) {
        if (skillRepository.findByNameIgnoreCaseAndDeletedAtIsNull(name).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "SKILL_NAME_EXISTS", "Une compÃ©tence porte dÃ©jÃ  ce nom");
        }
    }

    private SkillResponse withEmployeeCount(Skill skill) {
        SkillResponse response = skillMapper.toResponse(skill);
        return new SkillResponse(response.id(), response.name(), response.category(),
                employeeSkillRepository.countBySkillId(skill.getId()), response.createdAt());
    }

    private Specification<Skill> filter(String search, String category) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase(Locale.ROOT)));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("category")), pattern)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}

