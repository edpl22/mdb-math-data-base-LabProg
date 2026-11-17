package com.mdb.backend.controller;

import com.mdb.backend.dto.request.DifficultyRatingRequest;
import com.mdb.backend.dto.request.ProblemRequest;
import com.mdb.backend.dto.request.StatusRequest;
import com.mdb.backend.dto.response.*;
import com.mdb.backend.entity.User;
import com.mdb.backend.security.AuthenticatedUser;
import com.mdb.backend.service.DifficultyRatingService;
import com.mdb.backend.service.ProblemService;
import com.mdb.backend.service.UserProblemStatusService;
import com.mdb.backend.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/problems")
@Tag(name = "Problems")
public class ProblemController {

    private final ProblemService problemService;
    private final DifficultyRatingService difficultyRatingService;
    private final UserProblemStatusService userProblemStatusService;
    private final UserService userService;

    public ProblemController(
            ProblemService problemService,
            DifficultyRatingService difficultyRatingService,
            UserProblemStatusService userProblemStatusService,
            UserService userService
    ) {
        this.problemService = problemService;
        this.difficultyRatingService = difficultyRatingService;
        this.userProblemStatusService = userProblemStatusService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProblemSummaryResponse>> search(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Integer minDifficulty,
            @RequestParam(required = false) Integer maxDifficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("id").descending());
        return ResponseEntity.ok(PageResponse.from(
                problemService.search(text, area, tag, minDifficulty, maxDifficulty, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProblemDetailResponse> findById(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        Long userId = principal == null ? null : principal.getId();
        return ResponseEntity.ok(problemService.findById(id, userId));
    }

    @PostMapping
    public ResponseEntity<ProblemDetailResponse> create(
            @Valid @RequestBody ProblemRequest request, @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        User createdBy = userService.getOrThrow(principal.getId());
        ProblemDetailResponse created = problemService.create(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProblemDetailResponse> update(
            @PathVariable Long id, @Valid @RequestBody ProblemRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ResponseEntity.ok(problemService.update(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        problemService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/difficulty")
    public ResponseEntity<DifficultyRatingResponse> getDifficulty(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        Long userId = principal == null ? null : principal.getId();
        return ResponseEntity.ok(difficultyRatingService.get(id, userId));
    }

    @PostMapping("/{id}/difficulty")
    public ResponseEntity<DifficultyRatingResponse> createDifficulty(
            @PathVariable Long id, @Valid @RequestBody DifficultyRatingRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        User user = userService.getOrThrow(principal.getId());
        DifficultyRatingResponse response = difficultyRatingService.create(id, user, request.difficulty());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/difficulty")
    public ResponseEntity<DifficultyRatingResponse> updateDifficulty(
            @PathVariable Long id, @Valid @RequestBody DifficultyRatingRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        User user = userService.getOrThrow(principal.getId());
        return ResponseEntity.ok(difficultyRatingService.update(id, user, request.difficulty()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<UserStatusResponse> setStatus(
            @PathVariable Long id, @Valid @RequestBody StatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        User user = userService.getOrThrow(principal.getId());
        return ResponseEntity.ok(userProblemStatusService.setStatus(id, user, request.status()));
    }

    @DeleteMapping("/{id}/status")
    public ResponseEntity<UserStatusResponse> removeStatus(
            @PathVariable Long id, @RequestParam com.mdb.backend.entity.StatusType status,
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        User user = userService.getOrThrow(principal.getId());
        return ResponseEntity.ok(userProblemStatusService.removeStatus(id, user, status));
    }
}
