package com.bullhorn.mockservice.controller;

import com.bullhorn.mockservice.dto.request.CreateConsultantRequest;
import com.bullhorn.mockservice.dto.request.UpdateConsultantRequest;
import com.bullhorn.mockservice.dto.response.ApiResponse;
import com.bullhorn.mockservice.dto.response.ConsultantResponse;
import com.bullhorn.mockservice.service.ConsultantService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Consultant operations
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_VERSION + "/consultants")
@RequiredArgsConstructor
@Tag(name = "Consultants", description = "Consultant management APIs")
public class ConsultantController {

    private final ConsultantService consultantService;

    @PostMapping
    @Operation(summary = "Create a new consultant")
    public ResponseEntity<ApiResponse<ConsultantResponse>> createConsultant(
            @Valid @RequestBody CreateConsultantRequest request
    ) {
        ConsultantResponse consultant = consultantService.createConsultant(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(consultant, "Consultant created successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a consultant")
    public ResponseEntity<ApiResponse<ConsultantResponse>> updateConsultant(
            @PathVariable Long id,
            @Valid @RequestBody UpdateConsultantRequest request
    ) {
        ConsultantResponse consultant = consultantService.updateConsultant(id, request);
        return ResponseEntity.ok(ApiResponse.success(consultant, "Consultant updated successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get consultant by ID")
    public ResponseEntity<ApiResponse<ConsultantResponse>> getConsultantById(@PathVariable Long id) {
        ConsultantResponse consultant = consultantService.getConsultantById(id);
        return ResponseEntity.ok(ApiResponse.success(consultant));
    }

    @GetMapping("/bullhorn/{bullhornId}")
    @Operation(summary = "Get consultant by Bullhorn ID")
    public ResponseEntity<ApiResponse<ConsultantResponse>> getConsultantByBullhornId(@PathVariable String bullhornId) {
        ConsultantResponse consultant = consultantService.getConsultantByBullhornId(bullhornId);
        return ResponseEntity.ok(ApiResponse.success(consultant));
    }

    @GetMapping
    @Operation(summary = "Get all consultants with pagination")
    public ResponseEntity<ApiResponse<Page<ConsultantResponse>>> getAllConsultants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, Math.min(size, Constants.MAX_PAGE_SIZE), sort);

        Page<ConsultantResponse> consultants = consultantService.getAllConsultants(pageable);
        return ResponseEntity.ok(ApiResponse.success(consultants));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active consultants")
    public ResponseEntity<ApiResponse<Page<ConsultantResponse>>> getActiveConsultants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, Constants.MAX_PAGE_SIZE));
        Page<ConsultantResponse> consultants = consultantService.getActiveConsultants(pageable);
        return ResponseEntity.ok(ApiResponse.success(consultants));
    }

    @GetMapping("/search")
    @Operation(summary = "Search consultants")
    public ResponseEntity<ApiResponse<Page<ConsultantResponse>>> searchConsultants(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, Constants.MAX_PAGE_SIZE));
        Page<ConsultantResponse> consultants = consultantService.searchConsultants(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(consultants));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a consultant")
    public ResponseEntity<ApiResponse<Void>> deleteConsultant(@PathVariable Long id) {
        consultantService.deleteConsultant(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Consultant deleted successfully"));
    }
}
