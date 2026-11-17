package com.mdb.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ProblemSummaryResponse(
        Long id,
        String title,
        String area,
        List<String> tags,
        Integer initialDifficulty,
        Double communityDifficulty,
        long ratingsCount,
        LocalDateTime updatedAt
) {
}
