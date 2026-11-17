package com.mdb.backend.service;

import com.mdb.backend.dto.request.ProblemRequest;
import com.mdb.backend.entity.*;
import com.mdb.backend.exception.ResourceNotFoundException;
import com.mdb.backend.mapper.ProblemMapper;
import com.mdb.backend.repository.AreaRepository;
import com.mdb.backend.repository.ProblemRepository;
import com.mdb.backend.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @Mock private ProblemRepository problemRepository;
    @Mock private AreaRepository areaRepository;
    @Mock private TagRepository tagRepository;
    @Mock private AreaService areaService;
    @Mock private TagService tagService;
    @Mock private ProblemMapper problemMapper;

    @InjectMocks
    private ProblemService problemService;

    @Test
    void create_shouldRejectUnknownArea() {
        ProblemRequest request = new ProblemRequest(
                "Title", "Statement", "Nonexistent Area", "Hint", "Solution", 3, List.of("tag1"), null);
        when(areaService.findByNameOrThrow("Nonexistent Area"))
                .thenThrow(new ResourceNotFoundException("Area 'Nonexistent Area' not found."));

        User admin = User.builder().id(1L).name("Admin").role(Role.ADMIN).build();

        assertThatThrownBy(() -> problemService.create(request, admin))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(problemRepository, never()).save(any());
    }

    @Test
    void getOrThrow_shouldThrowWhenProblemDoesNotExist() {
        when(problemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> problemService.getOrThrow(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_shouldRemoveExistingProblem() {
        Problem problem = Problem.builder().id(5L).build();
        when(problemRepository.findById(5L)).thenReturn(Optional.of(problem));

        problemService.delete(5L);

        verify(problemRepository).delete(problem);
    }

    @Test
    void delete_shouldThrowWhenProblemDoesNotExist() {
        when(problemRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> problemService.delete(5L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(problemRepository, never()).delete(any());
    }
}
