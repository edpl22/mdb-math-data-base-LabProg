package com.mdb.backend.controller;

import com.mdb.backend.dto.response.UserProblemsResponse;
import com.mdb.backend.dto.response.UserResponse;
import com.mdb.backend.entity.StatusType;
import com.mdb.backend.security.AuthenticatedUser;
import com.mdb.backend.service.UserProblemStatusService;
import com.mdb.backend.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;
    private final UserProblemStatusService userProblemStatusService;

    public UserController(UserService userService, UserProblemStatusService userProblemStatusService) {
        this.userService = userService;
        this.userProblemStatusService = userProblemStatusService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(userService.findById(principal.getId()));
    }

    @GetMapping("/me/problems")
    public ResponseEntity<UserProblemsResponse> myProblems(@AuthenticationPrincipal AuthenticatedUser principal) {
        Long userId = principal.getId();
        UserProblemsResponse response = new UserProblemsResponse(
                userProblemStatusService.findUserProblemsByStatus(userId, StatusType.FAVORITE),
                userProblemStatusService.findUserProblemsByStatus(userId, StatusType.WANT_TO_DO),
                userProblemStatusService.findUserProblemsByStatus(userId, StatusType.DONE)
        );
        return ResponseEntity.ok(response);
    }
}
