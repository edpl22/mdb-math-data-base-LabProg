package com.mdb.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ProblemDetailResponse(
        Long id,
        String title,
        String statement,
        String hint,
        String solution,
        String source,
        String area,
        List<String> tags,
        Integer initialDifficulty,
        Double communityDifficulty,
        long ratingsCount,
        Integer userDifficulty,
        List<String> userStatuses,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
