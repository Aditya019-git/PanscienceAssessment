package com.panscience.assessment.dto;

import com.panscience.assessment.entity.ContentChunk;

public record ContentChunkResponse(
    Long id,
    String chunkText,
    Integer pageNumber,
    Double startTime,
    Double endTime
) {

    public static ContentChunkResponse from(ContentChunk contentChunk) {
        return new ContentChunkResponse(
            contentChunk.getId(),
            contentChunk.getChunkText(),
            contentChunk.getPageNumber(),
            contentChunk.getStartTime(),
            contentChunk.getEndTime()
        );
    }
}

