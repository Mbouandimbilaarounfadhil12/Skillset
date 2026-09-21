package com.skillset.application.service;

import com.skillset.application.dto.ScreeningQuestionDTO;
import com.skillset.domain.entity.ScreeningQuestion;
import com.skillset.domain.port.ScreeningQuestionRepositoryPort;
import com.skillset.infrastructure.security.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScreeningQuestionService {
    private final ScreeningQuestionRepositoryPort screeningQuestionRepositoryPort;
    private final AuthorizationService authorizationService;

    public ScreeningQuestion createQuestion(String employerId, ScreeningQuestion question) {
        if (question.getJobListing() != null && question.getJobListing().getId() != null) {
            authorizationService.requireJobOwner(employerId, question.getJobListing().getId());
        }
        return screeningQuestionRepositoryPort.save(question);
    }

    public List<ScreeningQuestionDTO> getJobScreeningQuestions(String jobListingId) {
        return screeningQuestionRepositoryPort.findByJobListingId(jobListingId)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public ScreeningQuestion updateQuestion(String employerId, String questionId, ScreeningQuestion questionDetails) {
        return screeningQuestionRepositoryPort.findById(questionId)
            .map(question -> {
                authorizationService.requireJobOwner(employerId, question.getJobListing().getId());
                question.setQuestionText(questionDetails.getQuestionText());
                question.setQuestionType(questionDetails.getQuestionType());
                question.setOptions(questionDetails.getOptions());
                question.setIsRequired(questionDetails.getIsRequired());
                question.setOrderIndex(questionDetails.getOrderIndex());
                return screeningQuestionRepositoryPort.save(question);
            }).orElse(null);
    }

    public void deleteQuestion(String employerId, String questionId) {
        screeningQuestionRepositoryPort.findById(questionId).ifPresent(question -> {
            authorizationService.requireJobOwner(employerId, question.getJobListing().getId());
            screeningQuestionRepositoryPort.deleteById(questionId);
        });
    }

    private ScreeningQuestionDTO convertToDTO(ScreeningQuestion question) {
        return new ScreeningQuestionDTO(
            question.getId(),
            question.getJobListing().getId(),
            question.getQuestionText(),
            question.getQuestionType(),
            question.getOptions(),
            question.getIsRequired(),
            question.getOrderIndex()
        );
    }
}
