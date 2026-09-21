package com.skillset.infrastructure.persistence.specification;

import com.skillset.domain.entity.JobListing;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JobListingSpecification {

    public static Specification<JobListing> withKeywords(String keywords) {
        return (root, query, cb) -> {
            if (keywords == null || keywords.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + keywords + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), cb.lower(cb.literal(pattern))),
                    cb.like(cb.lower(root.get("description")), cb.lower(cb.literal(pattern)))
            );
        };
    }

    public static Specification<JobListing> withLocation(String location) {
        return (root, query, cb) -> {
            if (location == null || location.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%");
        };
    }

    public static Specification<JobListing> withContractType(String contractType) {
        return (root, query, cb) -> {
            if (contractType == null || contractType.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("jobType"), contractType);
        };
    }

    public static Specification<JobListing> withSalaryRange(Integer minSalary, Integer maxSalary) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (minSalary != null && minSalary > 0) {
                try {
                    predicates.add(cb.greaterThanOrEqualTo(
                            cb.function("CAST", Integer.class, root.get("salaryMin"), cb.literal("int")),
                            minSalary
                    ));
                } catch (Exception e) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("salaryMin"), minSalary.toString()));
                }
            }

            if (maxSalary != null && maxSalary > 0) {
                try {
                    predicates.add(cb.lessThanOrEqualTo(
                            cb.function("CAST", Integer.class, root.get("salaryMax"), cb.literal("int")),
                            maxSalary
                    ));
                } catch (Exception e) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("salaryMax"), maxSalary.toString()));
                }
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<JobListing> withSkills(List<String> skills) {
        return (root, query, cb) -> {
            if (skills == null || skills.isEmpty()) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();
            for (String skill : skills) {
                if (skill != null && !skill.trim().isEmpty()) {
                    predicates.add(cb.like(cb.lower(root.get("requiredSkills")), "%" + skill.toLowerCase() + "%"));
                }
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<JobListing> withPostedWithin(Integer days) {
        return (root, query, cb) -> {
            if (days == null || days <= 0) {
                return cb.conjunction();
            }
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
            return cb.greaterThanOrEqualTo(root.get("postedAt"), cutoffDate);
        };
    }

    public static Specification<JobListing> combine(List<Specification<JobListing>> specs) {
        return (root, query, cb) -> {
            if (specs == null || specs.isEmpty()) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();
            for (Specification<JobListing> spec : specs) {
                if (spec != null) {
                    Predicate predicate = spec.toPredicate(root, query, cb);
                    if (predicate != null && !isConjunction(predicate, cb)) {
                        predicates.add(predicate);
                    }
                }
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean isConjunction(Predicate predicate, CriteriaBuilder cb) {
        return predicate != null && predicate.getExpressions().isEmpty();
    }
}
