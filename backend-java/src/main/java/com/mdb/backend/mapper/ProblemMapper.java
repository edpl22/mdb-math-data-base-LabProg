package com.mdb.backend.mapper;

import com.mdb.backend.dto.response.ProblemDetailResponse;
import com.mdb.backend.dto.response.ProblemSummaryResponse;
import com.mdb.backend.entity.Problem;
import com.mdb.backend.entity.StatusType;
import com.mdb.backend.entity.Tag;
import com.mdb.backend.repository.DifficultyRatingRepository;
import com.mdb.backend.repository.UserProblemStatusRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ProblemMapper {

    private final DifficultyRatingRepository difficultyRatingRepository;
    private final UserProblemStatusRepository userProblemStatusRepository;

    public ProblemMapper(
            DifficultyRatingRepository difficultyRatingRepository,
            UserProblemStatusRepository userProblemStatusRepository
    ) {
        this.difficultyRatingRepository = difficultyRatingRepository;
        this.userProblemStatusRepository = userProblemStatusRepository;
    }

    private List<String> tagNames(Problem problem) {
        return problem.getTags().stream().map(Tag::getName).sorted().toList();
    }

    private Double communityDifficulty(Problem problem) {
        Double avg = difficultyRatingRepository.averageDifficultyForProblem(problem.getId());
        if (avg == null) {
            return null;
        }
        return Math.round(avg * 10.0) / 10.0;
    }

    public ProblemSummaryResponse toSummary(Problem problem) {
        long count = difficultyRatingRepository.countForProblem(problem.getId());
        return new ProblemSummaryResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getArea().getName(),
                tagNames(problem),
                problem.getInitialDifficulty(),
                communityDifficulty(problem),
                count,
                problem.getUpdatedAt()
        );
    }

    public ProblemDetailResponse toDetail(Problem problem, Long currentUserId) {
        long count = difficultyRatingRepository.countForProblem(problem.getId());
        Integer userDifficulty = null;
        if (currentUserId != null) {
            userDifficulty = difficultyRatingRepository.findByUserIdAndProblemId(currentUserId, problem.getId())
                    .map(r -> r.getDifficulty())
                    .orElse(null);
        }
        List<String> userStatuses = List.of();
        if (currentUserId != null) {
            userStatuses = userProblemStatusRepository.findByUserIdAndProblemId(currentUserId, problem.getId())
                    .stream()
                    .map(s -> s.getStatus().name())
                    .sorted(Comparator.comparing(StatusType::valueOf))
                    .toList();
        }

        return new ProblemDetailResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getStatement(),
                problem.getHint(),
                problem.getSolution(),
                problem.getSource(),
                problem.getArea().getName(),
                tagNames(problem),
                problem.getInitialDifficulty(),
                communityDifficulty(problem),
                count,
                userDifficulty,
                userStatuses,
                problem.getCreatedBy().getName(),
                problem.getCreatedAt(),
                problem.getUpdatedAt()
        );
    }
}
