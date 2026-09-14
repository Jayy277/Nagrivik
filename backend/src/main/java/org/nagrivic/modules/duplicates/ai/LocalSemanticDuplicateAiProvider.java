package org.nagrivic.modules.duplicates.ai;

import org.nagrivic.modules.duplicates.model.DuplicateConfidence;

import java.util.*;

public class LocalSemanticDuplicateAiProvider implements DuplicateAiProvider {

    private final String modelName;
    private final String calculationVersion;
    private final double defaultDetectionRadius;

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for", "with",
            "of", "by", "from", "up", "about", "into", "through", "after", "is", "are",
            "was", "were", "be", "been", "being", "have", "has", "had", "do", "does",
            "did", "will", "would", "shall", "should", "may", "might", "must", "can",
            "could", "this", "that", "these", "those", "it", "its", "there", "here"
    );

    public LocalSemanticDuplicateAiProvider(String modelName, String calculationVersion, double defaultDetectionRadius) {
        this.modelName = modelName != null ? modelName : "local-semantic-v1";
        this.calculationVersion = calculationVersion != null ? calculationVersion : "duplicate-v1";
        this.defaultDetectionRadius = defaultDetectionRadius > 0 ? defaultDetectionRadius : 100.0;
    }

    @Override
    public String getProviderName() {
        return "LOCAL_SEMANTIC";
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public DuplicateAiAnalysisResult analyze(DuplicateAiRequest request) {
        if (request == null || request.target() == null || request.candidates() == null || request.candidates().isEmpty()) {
            return new DuplicateAiAnalysisResult(getProviderName(), getModelName(), calculationVersion, Collections.emptyList());
        }

        var target = request.target();
        List<DuplicateCandidateScore> candidateScores = new ArrayList<>(request.candidates().size());

        for (var candidate : request.candidates()) {
            // 1. Category equality score (0.0 to 1.0)
            boolean sameCategory = target.categoryId() != null && target.categoryId().equals(candidate.categoryId());
            double categoryScore = sameCategory ? 1.0 : 0.0;

            // 2. Spatial proximity score (0.0 to 1.0)
            double distance = Math.max(0.0, candidate.distanceMeters());
            double spatialScore = Math.max(0.0, 1.0 - (distance / defaultDetectionRadius));

            // 3. Text semantic similarity score (0.0 to 1.0)
            double titleSimilarity = computeTextSimilarity(target.title(), candidate.title());
            double descSimilarity = computeTextSimilarity(target.description(), candidate.description());

            double textScore;
            if (target.description() != null && !target.description().isBlank()
                    && candidate.description() != null && !candidate.description().isBlank()) {
                textScore = (titleSimilarity * 0.6) + (descSimilarity * 0.4);
            } else {
                textScore = titleSimilarity;
            }

            // 4. Weighted composite score
            // 45% text, 35% spatial, 20% category
            double composite = (textScore * 0.45) + (spatialScore * 0.35) + (categoryScore * 0.20);
            int rawScore = (int) Math.round(composite * 100.0);
            int finalScore = Math.min(100, Math.max(0, rawScore));

            // 5. Confidence determination
            DuplicateConfidence confidence;
            if (finalScore >= 85) {
                confidence = DuplicateConfidence.HIGH;
            } else if (finalScore >= 70) {
                confidence = DuplicateConfidence.LIKELY;
            } else if (finalScore >= 40) {
                confidence = DuplicateConfidence.POSSIBLE;
            } else {
                confidence = DuplicateConfidence.LOW;
            }

            // 6. Explainable structured signals
            List<String> signals = new ArrayList<>();
            if (sameCategory) {
                String catName = candidate.categoryName() != null ? candidate.categoryName() : "Same category";
                signals.add("Same category: " + catName);
            }
            if (distance <= 5.0) {
                signals.add("Immediate proximity (<5m)");
            } else {
                signals.add("Location " + Math.round(distance) + "m away");
            }

            if (textScore >= 0.75) {
                signals.add("High semantic text similarity");
            } else if (textScore >= 0.45) {
                signals.add("Similar problem description");
            } else if (titleSimilarity >= 0.50) {
                signals.add("Similar issue title");
            }

            candidateScores.add(new DuplicateCandidateScore(
                    candidate.issueId(),
                    finalScore,
                    confidence,
                    signals
            ));
        }

        return new DuplicateAiAnalysisResult(
                getProviderName(),
                getModelName(),
                calculationVersion,
                candidateScores
        );
    }

    public static double computeTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }

        Set<String> tokens1 = tokenize(text1);
        Set<String> tokens2 = tokenize(text2);

        if (tokens1.isEmpty() && tokens2.isEmpty()) {
            return 1.0;
        }
        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return 0.0;
        }

        // Token Jaccard similarity
        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);

        Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);

        double jaccard = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();

        // 3-gram character overlap for catching stem / minor spelling differences
        Set<String> grams1 = generateCharNGrams(text1, 3);
        Set<String> grams2 = generateCharNGrams(text2, 3);

        double nGramSim = 0.0;
        if (!grams1.isEmpty() && !grams2.isEmpty()) {
            Set<String> gramIntersection = new HashSet<>(grams1);
            gramIntersection.retainAll(grams2);
            Set<String> gramUnion = new HashSet<>(grams1);
            gramUnion.addAll(grams2);
            nGramSim = (double) gramIntersection.size() / gramUnion.size();
        }

        return (jaccard * 0.65) + (nGramSim * 0.35);
    }

    private static Set<String> tokenize(String input) {
        if (input == null) return Collections.emptySet();
        String cleaned = input.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] parts = cleaned.split("\\s+");
        Set<String> tokens = new HashSet<>();
        for (String p : parts) {
            if (p.length() > 1 && !STOP_WORDS.contains(p)) {
                tokens.add(p);
            }
        }
        return tokens;
    }

    private static Set<String> generateCharNGrams(String input, int n) {
        if (input == null) return Collections.emptySet();
        String cleaned = input.toLowerCase().replaceAll("\\s+", " ").trim();
        if (cleaned.length() < n) {
            return cleaned.isEmpty() ? Collections.emptySet() : Collections.singleton(cleaned);
        }
        Set<String> ngrams = new HashSet<>();
        for (int i = 0; i <= cleaned.length() - n; i++) {
            ngrams.add(cleaned.substring(i, i + n));
        }
        return ngrams;
    }
}
