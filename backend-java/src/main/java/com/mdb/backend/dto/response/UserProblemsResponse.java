package com.mdb.backend.dto.response;

import java.util.List;

public record UserProblemsResponse(
        List<ProblemSummaryResponse> favorites,
        List<ProblemSummaryResponse> wantToDo,
        List<ProblemSummaryResponse> done
) {
}
