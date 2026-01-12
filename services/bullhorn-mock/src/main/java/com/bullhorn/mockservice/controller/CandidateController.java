package com.bullhorn.mockservice.controller;

import com.bullhorn.mockservice.dto.request.CreateCandidateRequest;
import com.bullhorn.mockservice.dto.request.UpdateCandidateRequest;
import com.bullhorn.mockservice.dto.response.ApiResponse;
import com.bullhorn.mockservice.dto.response.CandidateResponse;
import com.bullhorn.mockservice.service.CandidateService;
import com.bullhorn.mockservice.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST Controller for Candidate operations
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_VERSION + "/candidates")
@RequiredArgsConstructor
@Tag(name = "Candidates", description = "Candidate management APIs")
public class CandidateController {

    private final CandidateService candidateService;

    @PostMapping
    @Operation(summary = "Create a new candidate")
    public ResponseEntity<ApiResponse<CandidateResponse>> createCandidate(
            @Valid @RequestBody CreateCandidateRequest request
    ) {
        CandidateResponse candidate = candidateService.createCandidate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(candidate, "Candidate created successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a candidate")
    public ResponseEntity<ApiResponse<CandidateResponse>> updateCandidate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCandidateRequest request
    ) {
        CandidateResponse candidate = candidateService.updateCandidate(id, request);
        return ResponseEntity.ok(ApiResponse.success(candidate, "Candidate updated successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get candidate by ID")
    public ResponseEntity<ApiResponse<CandidateResponse>> getCandidateById(@PathVariable Long id) {
        CandidateResponse candidate = candidateService.getCandidateById(id);
        return ResponseEntity.ok(ApiResponse.success(candidate));
    }

    @GetMapping("/bullhorn/{bullhornId}")
    @Operation(summary = "Get candidate by Bullhorn ID")
    public ResponseEntity<ApiResponse<CandidateResponse>> getCandidateByBullhornId(@PathVariable String bullhornId) {
        CandidateResponse candidate = candidateService.getCandidateByBullhornId(bullhornId);
        return ResponseEntity.ok(ApiResponse.success(candidate));
    }

    @GetMapping
    @Operation(summary = "Get all candidates with pagination")
    public ResponseEntity<ApiResponse<Page<CandidateResponse>>> getAllCandidates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, Math.min(size, Constants.MAX_PAGE_SIZE), sort);

        Page<CandidateResponse> candidates = candidateService.getAllCandidates(pageable);
        return ResponseEntity.ok(ApiResponse.success(candidates));
    }

    @GetMapping("/search")
    @Operation(summary = "Search candidates")
    public ResponseEntity<ApiResponse<Page<CandidateResponse>>> searchCandidates(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, Constants.MAX_PAGE_SIZE));
        Page<CandidateResponse> candidates = candidateService.searchCandidates(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(candidates));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get candidates by status")
    public ResponseEntity<ApiResponse<Page<CandidateResponse>>> getCandidatesByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, Constants.MAX_PAGE_SIZE));
        Page<CandidateResponse> candidates = candidateService.getCandidatesByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(candidates));
    }

    @GetMapping("/modified-since")
    @Operation(summary = "Get candidates modified since a specific date")
    public ResponseEntity<ApiResponse<?>> getCandidatesModifiedSince(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime modifiedSince
    ) {
        var candidates = candidateService.getCandidatesModifiedSince(modifiedSince);
        return ResponseEntity.ok(ApiResponse.success(candidates));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a candidate")
    public ResponseEntity<ApiResponse<Void>> deleteCandidate(@PathVariable Long id) {
        candidateService.deleteCandidate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Candidate deleted successfully"));
    }

    @DeleteMapping("/{id}/hard")
    @Operation(summary = "Hard delete a candidate")
    public ResponseEntity<ApiResponse<Void>> hardDeleteCandidate(@PathVariable Long id) {
        candidateService.hardDeleteCandidate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Candidate permanently deleted"));
    }
}
