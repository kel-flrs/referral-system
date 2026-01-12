package com.bullhorn.mockservice.service;

import com.bullhorn.mockservice.dto.request.CreateCandidateRequest;
import com.bullhorn.mockservice.dto.request.UpdateCandidateRequest;
import com.bullhorn.mockservice.dto.response.CandidateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for Candidate operations
 */
public interface CandidateService {

    CandidateResponse createCandidate(CreateCandidateRequest request);

    CandidateResponse updateCandidate(Long id, UpdateCandidateRequest request);

    CandidateResponse getCandidateById(Long id);

    CandidateResponse getCandidateByBullhornId(String bullhornId);

    Page<CandidateResponse> getAllCandidates(Pageable pageable);

    Page<CandidateResponse> getCandidatesByStatus(String status, Pageable pageable);

    Page<CandidateResponse> searchCandidates(String query, Pageable pageable);

    List<CandidateResponse> getCandidatesModifiedSince(LocalDateTime modifiedSince);

    void deleteCandidate(Long id);

    void hardDeleteCandidate(Long id);
}
