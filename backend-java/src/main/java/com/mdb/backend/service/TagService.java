package com.mdb.backend.service;

import com.mdb.backend.dto.response.TagResponse;
import com.mdb.backend.entity.Tag;
import com.mdb.backend.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<TagResponse> findAll() {
        return tagRepository.findAll().stream()
                .map(t -> new TagResponse(t.getId(), t.getName()))
                .toList();
    }

    @Transactional
    public Tag findOrCreateByName(String name) {
        String normalized = name.trim().toLowerCase();
        return tagRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> tagRepository.save(Tag.builder().name(normalized).build()));
    }
}
