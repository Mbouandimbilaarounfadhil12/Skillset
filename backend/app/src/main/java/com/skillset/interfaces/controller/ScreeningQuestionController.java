package com.skillset.interfaces.controller;

import com.skillset.application.dto.ScreeningQuestionDTO;
import com.skillset.application.service.ScreeningQuestionService;
import com.skillset.domain.entity.ScreeningQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screening-questions")
@RequiredArgsConstructor
public class ScreeningQuestionController {
    private final ScreeningQuestionService screeningQuestionService;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ScreeningQuestion> createQuestion(@AuthenticationPrincipal String userId,
                                                            @RequestBody ScreeningQuestion question) {
        ScreeningQuestion createdQuestion = screeningQuestionService.createQuestion(userId, question);
        return new ResponseEntity<>(createdQuestion, HttpStatus.CREATED);
    }

    @GetMapping("/job/{jobListingId}")
    public ResponseEntity<List<ScreeningQuestionDTO>> getJobQuestions(@PathVariable String jobListingId) {
        List<ScreeningQuestionDTO> questions = screeningQuestionService.getJobScreeningQuestions(jobListingId);
        return ResponseEntity.ok(questions);
    }

    @PutMapping("/{questionId}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ScreeningQuestion> updateQuestion(@AuthenticationPrincipal String userId,
                                                            @PathVariable String questionId,
                                                            @RequestBody ScreeningQuestion questionDetails) {
        ScreeningQuestion updatedQuestion = screeningQuestionService.updateQuestion(userId, questionId, questionDetails);
        if (updatedQuestion != null) {
            return ResponseEntity.ok(updatedQuestion);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{questionId}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<Void> deleteQuestion(@AuthenticationPrincipal String userId,
                                               @PathVariable String questionId) {
        screeningQuestionService.deleteQuestion(userId, questionId);
        return ResponseEntity.noContent().build();
    }
}
