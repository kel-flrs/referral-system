package com.bullhorn.mockservice.controller;

import com.bullhorn.mockservice.dto.request.CreateJobOrderRequest;
import com.bullhorn.mockservice.dto.request.UpdateJobOrderRequest;
import com.bullhorn.mockservice.dto.response.ApiResponse;
import com.bullhorn.mockservice.dto.response.JobOrderResponse;
import com.bullhorn.mockservice.service.JobOrderService;
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
 * REST Controller for JobOrder operations
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_VERSION + "/job-orders")
@RequiredArgsConstructor
@Tag(name = "Job Orders", description = "Job order management APIs")
public class JobOrderController {

    private final JobOrderService jobOrderService;

    @PostMapping
    @Operation(summary = "Create a new job order")
    public ResponseEntity<ApiResponse<JobOrderResponse>> createJobOrder(
            @Valid @RequestBody CreateJobOrderRequest request
    ) {
        JobOrderResponse jobOrder = jobOrderService.createJobOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(jobOrder, "Job order created successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a job order")
    public ResponseEntity<ApiResponse<JobOrderResponse>> updateJobOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobOrderRequest request
    ) {
        JobOrderResponse jobOrder = jobOrderService.updateJobOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success(jobOrder, "Job order updated successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job order by ID")
    public ResponseEntity<ApiResponse<JobOrderResponse>> getJobOrderById(@PathVariable Long id) {
        JobOrderResponse jobOrder = jobOrderService.getJobOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(jobOrder));
    }

    @GetMapping("/bullhorn/{bullhornId}")
    @Operation(summary = "Get job order by Bullhorn ID")
    public ResponseEntity<ApiResponse<JobOrderResponse>> getJobOrderByBullhornId(@PathVariable String bullhornId) {
        JobOrderResponse jobOrder = jobOrderService.getJobOrderByBullhornId(bullhornId);
        return ResponseEntity.ok(ApiResponse.success(jobOrder));
    }

    @GetMapping
    @Operation(summary = "Get all job orders with pagination")
    public ResponseEntity<ApiResponse<Page<JobOrderResponse>>> getAllJobOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("DESC") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, Math.min(size, Constants.MAX_PAGE_SIZE), sort);

        Page<JobOrderResponse> jobOrders = jobOrderService.getAllJobOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(jobOrders));
    }

    @GetMapping("/open")
    @Operation(summary = "Get open job orders")
    public ResponseEntity<ApiResponse<Page<JobOrderResponse>>> getOpenJobOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, Constants.MAX_PAGE_SIZE));
        Page<JobOrderResponse> jobOrders = jobOrderService.getOpenJobOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(jobOrders));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get job orders by status")
    public ResponseEntity<ApiResponse<Page<JobOrderResponse>>> getJobOrdersByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, Constants.MAX_PAGE_SIZE));
        Page<JobOrderResponse> jobOrders = jobOrderService.getJobOrdersByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(jobOrders));
    }

    @GetMapping("/search")
    @Operation(summary = "Search job orders")
    public ResponseEntity<ApiResponse<Page<JobOrderResponse>>> searchJobOrders(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, Constants.MAX_PAGE_SIZE));
        Page<JobOrderResponse> jobOrders = jobOrderService.searchJobOrders(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(jobOrders));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a job order")
    public ResponseEntity<ApiResponse<Void>> deleteJobOrder(@PathVariable Long id) {
        jobOrderService.deleteJobOrder(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Job order deleted successfully"));
    }

    @DeleteMapping("/{id}/hard")
    @Operation(summary = "Hard delete a job order")
    public ResponseEntity<ApiResponse<Void>> hardDeleteJobOrder(@PathVariable Long id) {
        jobOrderService.hardDeleteJobOrder(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Job order permanently deleted"));
    }
}
