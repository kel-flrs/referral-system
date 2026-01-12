package com.bullhorn.mockservice.service.impl;

import com.bullhorn.mockservice.dto.request.CreateCandidateRequest;
import com.bullhorn.mockservice.dto.request.UpdateCandidateRequest;
import com.bullhorn.mockservice.dto.response.CandidateResponse;
import com.bullhorn.mockservice.entity.Candidate;
import com.bullhorn.mockservice.entity.Education;
import com.bullhorn.mockservice.entity.Experience;
import com.bullhorn.mockservice.exception.DuplicateResourceException;
import com.bullhorn.mockservice.exception.ResourceNotFoundException;
import com.bullhorn.mockservice.mapper.CandidateDtoAssembler;
import com.bullhorn.mockservice.mapper.CandidateMapper;
import com.bullhorn.mockservice.repository.CandidateRepository;
import com.bullhorn.mockservice.repository.EducationRepository;
import com.bullhorn.mockservice.repository.ExperienceRepository;
import com.bullhorn.mockservice.repository.SkillsRepository;
import com.bullhorn.mockservice.service.CandidateService;
import com.bullhorn.mockservice.service.webhook.WebhookNotificationService;
import com.bullhorn.mockservice.util.Constants;
import com.bullhorn.mockservice.webhook.WebhookEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of CandidateService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final CandidateMapper candidateMapper;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final SkillsRepository skillsRepository;
    private final CandidateDtoAssembler assembler;
    private final WebhookNotificationService webhookNotificationService;

    @Override
    @Transactional
    public CandidateResponse createCandidate(CreateCandidateRequest request) {
        log.info("Creating new candidate with email: {}", request.getEmail());

        // Check for duplicate email
        candidateRepository.findByEmail(request.getEmail()).ifPresent(c -> {
            throw new DuplicateResourceException("Candidate", "email", request.getEmail());
        });

        Candidate candidate = candidateMapper.toEntity(request);
        candidate.setBullhornId(UUID.randomUUID().toString());
        candidate.setStatus(request.getStatus() != null ? request.getStatus() : Constants.STATUS_ACTIVE);

        Candidate savedCandidate = candidateRepository.save(candidate);
        log.info("Created candidate with ID: {}", savedCandidate.getId());

        // Notify webhook subscribers
        CandidateResponse response = candidateMapper.toResponse(savedCandidate);
        webhookNotificationService.notifySubscribers(WebhookEventType.CANDIDATE_CREATED, response);

        return response;
    }

    @Override
    @Transactional
    public CandidateResponse updateCandidate(Long id, UpdateCandidateRequest request) {
        log.info("Updating candidate with ID: {}", id);

        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", id));

        // Check for duplicate email if email is being updated
        if (request.getEmail() != null && !request.getEmail().equals(candidate.getEmail())) {
            if (candidateRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
                throw new DuplicateResourceException("Candidate", "email", request.getEmail());
            }
        }

        candidateMapper.updateEntityFromRequest(request, candidate);
        Candidate updatedCandidate = candidateRepository.save(candidate);

        log.info("Updated candidate with ID: {}", updatedCandidate.getId());

        // Notify webhook subscribers
        CandidateResponse response = candidateMapper.toResponse(updatedCandidate);
        webhookNotificationService.notifySubscribers(WebhookEventType.CANDIDATE_UPDATED, response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateResponse getCandidateById(Long id) {
        log.info("Fetching candidate with ID: {}", id);

        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", id));

        return candidateMapper.toResponse(candidate);
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateResponse getCandidateByBullhornId(String bullhornId) {
        log.info("Fetching candidate with Bullhorn ID: {}", bullhornId);

        Candidate candidate = candidateRepository.findByBullhornId(bullhornId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "bullhornId", bullhornId));

        return candidateMapper.toResponse(candidate);
    }

// Not optimized version (deprecated)
//    @Override
//    @Transactional(readOnly = true)
//    public Page<CandidateResponse> getAllCandidates(Pageable pageable) {
//        log.info("Fetching all active candidates");
//
//        Page<Candidate> candidates = candidateRepository.findAllActive(pageable);
//        return candidates.map(candidateMapper::toResponse);
//    }

    @Override
    @Transactional(readOnly = true)
    public Page<CandidateResponse> getAllCandidates(Pageable pageable) {
        log.info("Fetching all active candidates (optimized for large datasets)");

        // Step 1 — Fetch candidates for the page
        Page<Candidate> candidatesPage = candidateRepository.findAllActiveCandidates(pageable);
        List<Candidate> candidates = candidatesPage.getContent();

        if (candidates.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> candidateIds = candidates.stream()
                .map(Candidate::getId)
                .toList();

        // Step 2 — Initialize DTOs
        Map<Long, CandidateResponse> dtoMap = assembler.initDtos(candidates);

        // Step 3 — Fetch child entities in bulk
        List<Experience> experiences = experienceRepository.findByCandidateIds(candidateIds);
        List<Education> educations = educationRepository.findByCandidateIds(candidateIds);
        List<Object[]> skills = skillsRepository.findSkillsByCandidateIds(candidateIds);

        // Step 4 — Map children to DTOs
        assembler.addExperience(dtoMap, experiences);
        assembler.addEducation(dtoMap, educations);
        assembler.addSkills(dtoMap, skills);

        // Step 5 — Preserve order of candidates
        List<CandidateResponse> resultList = candidates.stream()
                .map(c -> dtoMap.get(c.getId()))
                .toList();

        return new PageImpl<>(resultList, pageable, candidatesPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CandidateResponse> getCandidatesByStatus(String status, Pageable pageable) {
        log.info("Fetching candidates with status: {}", status);

        Page<Candidate> candidates = candidateRepository.findByStatus(status, pageable);
        return candidates.map(candidateMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CandidateResponse> searchCandidates(String query, Pageable pageable) {
        log.info("Searching candidates with query: {}", query);

        Page<Candidate> candidates = candidateRepository.searchCandidates(query, pageable);
        return candidates.map(candidateMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CandidateResponse> getCandidatesModifiedSince(LocalDateTime modifiedSince) {
        log.info("Fetching candidates modified since: {}", modifiedSince);

        List<Candidate> candidates = candidateRepository.findByModifiedSince(modifiedSince);
        return candidateMapper.toResponseList(candidates);
    }

    @Override
    @Transactional
    public void deleteCandidate(Long id) {
        log.info("Soft deleting candidate with ID: {}", id);

        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate", "id", id));

        candidate.setIsDeleted(true);
        candidateRepository.save(candidate);

        log.info("Soft deleted candidate with ID: {}", id);

        // Notify webhook subscribers
        CandidateResponse response = candidateMapper.toResponse(candidate);
        webhookNotificationService.notifySubscribers(WebhookEventType.CANDIDATE_DELETED, response);
    }

    @Override
    @Transactional
    public void hardDeleteCandidate(Long id) {
        log.info("Hard deleting candidate with ID: {}", id);

        if (!candidateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Candidate", "id", id);
        }

        candidateRepository.deleteById(id);
        log.info("Hard deleted candidate with ID: {}", id);
    }
}
