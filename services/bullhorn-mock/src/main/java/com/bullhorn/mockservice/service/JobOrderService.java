package com.bullhorn.mockservice.service;

import com.bullhorn.mockservice.dto.request.CreateJobOrderRequest;
import com.bullhorn.mockservice.dto.request.UpdateJobOrderRequest;
import com.bullhorn.mockservice.dto.response.JobOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for JobOrder operations
 */
public interface JobOrderService {

    JobOrderResponse createJobOrder(CreateJobOrderRequest request);

    JobOrderResponse updateJobOrder(Long id, UpdateJobOrderRequest request);

    JobOrderResponse getJobOrderById(Long id);

    JobOrderResponse getJobOrderByBullhornId(String bullhornId);

    Page<JobOrderResponse> getAllJobOrders(Pageable pageable);

    Page<JobOrderResponse> getOpenJobOrders(Pageable pageable);

    Page<JobOrderResponse> getJobOrdersByStatus(String status, Pageable pageable);

    Page<JobOrderResponse> searchJobOrders(String query, Pageable pageable);

    void deleteJobOrder(Long id);

    void hardDeleteJobOrder(Long id);
}
