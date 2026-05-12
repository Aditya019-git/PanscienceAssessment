package com.panscience.assessment.dto;

import com.panscience.assessment.service.RetrievedChunk;

public record AnswerSourceResponse(
    Integer sourceNumber,
    Long chunkId,
    String excerpt,
    Integer pageNumber,
    Double startTime,
    Double endTime,
    Double relevanceScore
) {

    public static AnswerSourceResponse from(int sourceNumber, RetrievedChunk retrievedChunk) {
        return new AnswerSourceResponse(
            sourceNumber,
            retrievedChunk.chunk().getId(),
            excerpt(retrievedChunk.chunk().getChunkText()),
            retrievedChunk.chunk().getPageNumber(),
            retrievedChunk.chunk().getStartTime(),
            retrievedChunk.chunk().getEndTime(),
            roundScore(retrievedChunk.score())
        );
    }

    private static String excerpt(String text) {
        if (text == null) {
            return "";
        }

        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 240) {
            return normalized;
        }

        return normalized.substring(0, 237) + "...";
    }

    private static Double roundScore(double score) {
        return Math.round(score * 1000.0) / 1000.0;
    }
}
