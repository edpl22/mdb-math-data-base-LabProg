package com.mdb.backend.dto.response;

public record DifficultyRatingResponse(
        Long problemId,
        Double communityDifficulty,
        long ratingsCount,
        Integer userDifficulty
) {
}
