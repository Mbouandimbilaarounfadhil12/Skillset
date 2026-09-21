package com.skillset.infrastructure.persistence.specification;

import com.skillset.domain.entity.CandidateProfile;
import com.skillset.domain.entity.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CandidateSpecification {

    public static Specification<CandidateProfile> withKeywords(String keywords) {
        return (root, query, cb) -> {
            if (keywords == null || keywords.trim().isEmpty()) {
                return cb.conjunction();
            }

            String pattern = "%" + keywords.toLowerCase() + "%";
            Join<CandidateProfile, User> userJoin = root.join("user", jakarta.persistence.criteria.JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("jobDomain")), pattern),
                    cb.like(cb.lower(root.get("desiredRole")), pattern),
                    cb.like(cb.lower(userJoin.get("firstName")), pattern),
                    cb.like(cb.lower(userJoin.get("lastName")), pattern)
            );
        };
    }

    public static Specification<CandidateProfile> withSkills(List<String> skills) {
        return (root, query, cb) -> {
            if (skills == null || skills.isEmpty()) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();
            for (String skill : skills) {
                if (skill != null && !skill.trim().isEmpty()) {
                    predicates.add(cb.like(cb.lower(root.get("skills")), "%" + skill.toLowerCase() + "%"));
                }
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<CandidateProfile> withExperienceYears(Integer minYears) {
        return (root, query, cb) -> {
            if (minYears == null || minYears < 0) {
                return cb.conjunction();
            }

            return cb.like(cb.lower(root.get("experienceLevel")), "%" + minYears + "%");
        };
    }

    public static Specification<CandidateProfile> withAvailability(LocalDate availableFrom) {
        return (root, query, cb) -> {
            if (availableFrom == null) {
                return cb.conjunction();
            }

            return cb.greaterThanOrEqualTo(
                    cb.function("DATE", LocalDate.class, root.get("availability")),
                    availableFrom
            );
        };
    }

    public static Specification<CandidateProfile> withLocation(String location) {
        return (root, query, cb) -> {
            if (location == null || location.trim().isEmpty()) {
                return cb.conjunction();
            }

            String pattern = "%" + location.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("city")), pattern),
                    cb.like(cb.lower(root.get("country")), pattern),
                    cb.like(cb.lower(root.get("location")), pattern)
            );
        };
    }

    public static Specification<CandidateProfile> combine(List<Specification<CandidateProfile>> specs) {
        return (root, query, cb) -> {
            if (specs == null || specs.isEmpty()) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();
            for (Specification<CandidateProfile> spec : specs) {
                if (spec != null) {
                    Predicate predicate = spec.toPredicate(root, query, cb);
                    if (predicate != null && !isConjunction(predicate)) {
                        predicates.add(predicate);
                    }
                }
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean isConjunction(Predicate predicate) {
        return predicate != null && predicate.getExpressions().isEmpty();
    }
}
