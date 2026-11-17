package com.mdb.backend.service;

import com.mdb.backend.dto.request.ProblemRequest;
import com.mdb.backend.dto.response.ProblemDetailResponse;
import com.mdb.backend.dto.response.ProblemSummaryResponse;
import com.mdb.backend.entity.Area;
import com.mdb.backend.entity.Problem;
import com.mdb.backend.entity.Tag;
import com.mdb.backend.entity.User;
import com.mdb.backend.exception.ResourceNotFoundException;
import com.mdb.backend.mapper.ProblemMapper;
import com.mdb.backend.repository.AreaRepository;
import com.mdb.backend.repository.ProblemRepository;
import com.mdb.backend.repository.TagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final AreaRepository areaRepository;
    private final TagRepository tagRepository;
    private final AreaService areaService;
    private final TagService tagService;
    private final ProblemMapper problemMapper;

    public ProblemService(
            ProblemRepository problemRepository,
            AreaRepository areaRepository,
            TagRepository tagRepository,
            AreaService areaService,
            TagService tagService,
            ProblemMapper problemMapper
    ) {
        this.problemRepository = problemRepository;
        this.areaRepository = areaRepository;
        this.tagRepository = tagRepository;
        this.areaService = areaService;
        this.tagService = tagService;
        this.problemMapper = problemMapper;
    }

    public Page<ProblemSummaryResponse> search(
            String text, String areaName, String tagName, Integer minDifficulty, Integer maxDifficulty, Pageable pageable
    ) {
        Long areaId = resolveOptionalAreaId(areaName);
        Long tagId = resolveOptionalTagId(tagName);
        if ((areaName != null && !areaName.isBlank() && areaId == null)
                || (tagName != null && !tagName.isBlank() && tagId == null)) {
            return Page.empty(pageable);
        }

        double min = minDifficulty == null ? 1.0 : minDifficulty;
        double max = maxDifficulty == null ? 5.0 : maxDifficulty;
        // "" em vez de null: o Postgres não infere o tipo de null dentro de LOWER()
        String normalizedText = (text == null || text.isBlank()) ? "" : text.trim();

        return problemRepository.search(normalizedText, areaId, tagId, min, max, pageable)
                .map(problemMapper::toSummary);
    }

    public ProblemDetailResponse findById(Long id, Long currentUserId) {
        Problem problem = getOrThrow(id);
        return problemMapper.toDetail(problem, currentUserId);
    }

    Problem getOrThrow(Long id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Problema com id " + id + " não encontrado."));
    }

    @Transactional
    public ProblemDetailResponse create(ProblemRequest request, User createdBy) {
        Area area = areaService.findByNameOrThrow(request.area());
        Set<Tag> tags = resolveTags(request.tags());

        Problem problem = Problem.builder()
                .title(request.title().trim())
                .statement(request.statement())
                .hint(request.hint())
                .solution(request.solution())
                .source(request.source())
                .initialDifficulty(request.difficulty())
                .area(area)
                .tags(tags)
                .createdBy(createdBy)
                .build();

        Problem saved = problemRepository.save(problem);
        return problemMapper.toDetail(saved, createdBy.getId());
    }

    @Transactional
    public ProblemDetailResponse update(Long id, ProblemRequest request, Long currentUserId) {
        Problem problem = getOrThrow(id);
        Area area = areaService.findByNameOrThrow(request.area());
        Set<Tag> tags = resolveTags(request.tags());

        problem.setTitle(request.title().trim());
        problem.setStatement(request.statement());
        problem.setHint(request.hint());
        problem.setSolution(request.solution());
        problem.setSource(request.source());
        problem.setInitialDifficulty(request.difficulty());
        problem.setArea(area);
        problem.setTags(tags);

        Problem saved = problemRepository.save(problem);
        return problemMapper.toDetail(saved, currentUserId);
    }

    @Transactional
    public void delete(Long id) {
        Problem problem = getOrThrow(id);
        problemRepository.delete(problem);
    }

    private Set<Tag> resolveTags(List<String> tagNames) {
        Set<Tag> tags = new HashSet<>();
        if (tagNames != null) {
            for (String name : tagNames) {
                if (name != null && !name.isBlank()) {
                    tags.add(tagService.findOrCreateByName(name));
                }
            }
        }
        return tags;
    }

    private Long resolveOptionalAreaId(String areaName) {
        if (areaName == null || areaName.isBlank()) {
            return null;
        }
        return areaRepository.findByNameIgnoreCase(areaName.trim()).map(Area::getId).orElse(null);
    }

    private Long resolveOptionalTagId(String tagName) {
        if (tagName == null || tagName.isBlank()) {
            return null;
        }
        return tagRepository.findByNameIgnoreCase(tagName.trim()).map(Tag::getId).orElse(null);
    }
}
