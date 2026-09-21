package com.skillset.infrastructure.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CVParserUtil {

    private static final Set<String> STOP_WORDS = Set.of(
        "le", "la", "les", "de", "du", "des", "un", "une", "et", "en",
        "à", "au", "aux", "sur", "dans", "pour", "par", "avec", "qui",
        "que", "est", "sont", "the", "a", "an", "of", "in", "to", "for",
        "and", "or", "is", "are", "was", "were", "be", "been", "has", "have"
    );

    public String extractTextFromPdf(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) return "";
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            log.warn("Impossible de lire le PDF : {}", e.getMessage());
            return "";
        }
    }

    public String extractKeywords(String cvContent) {
        if (cvContent == null || cvContent.isBlank()) return "";
        log.info("Extracting keywords from CV ({} chars)", cvContent.length());
        return Arrays.stream(cvContent.toLowerCase().split("[^a-zA-ZÀ-ÿ]+"))
            .filter(w -> w.length() > 2 && !STOP_WORDS.contains(w))
            .distinct()
            .collect(Collectors.joining(","));
    }

    public Double calculateMatchScore(String cvContent, String jobRequirements) {
        if (cvContent == null || jobRequirements == null
                || cvContent.isBlank() || jobRequirements.isBlank()) return 0.0;
        log.info("Calculating match score between CV and job requirements");

        Set<String> cvWords = tokenize(cvContent);
        Set<String> jobWords = tokenize(jobRequirements);

        if (jobWords.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(cvWords);
        intersection.retainAll(jobWords);

        // Percentage of job keywords found in CV (recall-style score)
        double score = (double) intersection.size() / jobWords.size() * 100.0;
        return Math.round(score * 100.0) / 100.0;
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("[^a-zA-ZÀ-ÿ]+"))
            .filter(w -> w.length() > 2 && !STOP_WORDS.contains(w))
            .collect(Collectors.toCollection(HashSet::new));
    }
}
