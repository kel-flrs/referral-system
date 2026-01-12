package com.bullhorn.mockservice.service.impl;

import com.bullhorn.mockservice.dto.request.CreateJobOrderRequest;
import com.bullhorn.mockservice.dto.request.UpdateJobOrderRequest;
import com.bullhorn.mockservice.dto.response.JobOrderResponse;
import com.bullhorn.mockservice.entity.JobOrder;
import com.bullhorn.mockservice.exception.ResourceNotFoundException;
import com.bullhorn.mockservice.mapper.JobOrderMapper;
import com.bullhorn.mockservice.repository.JobOrderRepository;
import com.bullhorn.mockservice.service.JobOrderService;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of JobOrderService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobOrderServiceImpl implements JobOrderService {

    private final JobOrderRepository jobOrderRepository;
    private final JobOrderMapper jobOrderMapper;
    private final WebhookNotificationService webhookNotificationService;

    @Override
    @Transactional
    public JobOrderResponse createJobOrder(CreateJobOrderRequest request) {
        log.info("Creating new job order with title: {}", request.getTitle());

        JobOrder jobOrder = jobOrderMapper.toEntity(request);
        jobOrder.setBullhornId(UUID.randomUUID().toString());
        jobOrder.setStatus(request.getStatus() != null ? request.getStatus() : Constants.STATUS_OPEN);

        JobOrder savedJobOrder = jobOrderRepository.save(jobOrder);
        log.info("Created job order with ID: {}", savedJobOrder.getId());

        // Notify webhook subscribers
        JobOrderResponse response = jobOrderMapper.toResponse(savedJobOrder);
        webhookNotificationService.notifySubscribers(WebhookEventType.JOB_ORDER_CREATED, response);

        return response;
    }

    @Override
    @Transactional
    public JobOrderResponse updateJobOrder(Long id, UpdateJobOrderRequest request) {
        log.info("Updating job order with ID: {}", id);

        JobOrder jobOrder = jobOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobOrder", "id", id));

        jobOrderMapper.updateEntityFromRequest(request, jobOrder);
        JobOrder updatedJobOrder = jobOrderRepository.save(jobOrder);

        log.info("Updated job order with ID: {}", updatedJobOrder.getId());

        // Notify webhook subscribers
        JobOrderResponse response = jobOrderMapper.toResponse(updatedJobOrder);
        webhookNotificationService.notifySubscribers(WebhookEventType.JOB_ORDER_UPDATED, response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public JobOrderResponse getJobOrderById(Long id) {
        log.info("Fetching job order with ID: {}", id);

        JobOrder jobOrder = jobOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobOrder", "id", id));

        return jobOrderMapper.toResponse(jobOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public JobOrderResponse getJobOrderByBullhornId(String bullhornId) {
        log.info("Fetching job order with Bullhorn ID: {}", bullhornId);

        JobOrder jobOrder = jobOrderRepository.findByBullhornId(bullhornId)
                .orElseThrow(() -> new ResourceNotFoundException("JobOrder", "bullhornId", bullhornId));

        return jobOrderMapper.toResponse(jobOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobOrderResponse> getAllJobOrders(Pageable pageable) {
        log.info("Fetching all active job orders (optimized for large datasets)");

        Page<JobOrder> jobOrdersPage = jobOrderRepository.findAllActive(pageable);
        List<JobOrder> jobOrders = jobOrdersPage.getContent();

        if (jobOrders.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, jobOrdersPage.getTotalElements());
        }

        List<Long> jobOrderIds = jobOrders.stream()
                .map(JobOrder::getId)
                .toList();

        Map<Long, JobOrderResponse> dtoMap = new HashMap<>();
        for (JobOrder jobOrder : jobOrders) {
            dtoMap.put(
                    jobOrder.getId(),
                    JobOrderResponse.builder()
                            .id(jobOrder.getId())
                            .bullhornId(jobOrder.getBullhornId())
                            .title(jobOrder.getTitle())
                            .description(jobOrder.getDescription())
                            .employmentType(jobOrder.getEmploymentType())
                            .requiredSkills(new HashSet<>())
                            .preferredSkills(new HashSet<>())
                            .experienceLevel(jobOrder.getExperienceLevel())
                            .location(jobOrder.getLocation())
                            .salary(jobOrder.getSalary())
                            .clientName(jobOrder.getClientName())
                            .clientBullhornId(jobOrder.getClientBullhornId())
                            .status(jobOrder.getStatus())
                            .openDate(jobOrder.getOpenDate())
                            .closeDate(jobOrder.getCloseDate())
                            .lastSyncedAt(jobOrder.getLastSyncedAt())
                            .createdAt(jobOrder.getCreatedAt())
                            .updatedAt(jobOrder.getUpdatedAt())
                            .build()
            );
        }

        List<Object[]> requiredSkillsRows = jobOrderRepository.findRequiredSkillsByJobOrderIds(jobOrderIds);
        for (Object[] row : requiredSkillsRows) {
            Long jobOrderId = ((Number) row[0]).longValue();
            String skill = (String) row[1];
            JobOrderResponse dto = dtoMap.get(jobOrderId);
            if (dto != null) {
                dto.getRequiredSkills().add(skill);
            }
        }

        List<Object[]> preferredSkillsRows = jobOrderRepository.findPreferredSkillsByJobOrderIds(jobOrderIds);
        for (Object[] row : preferredSkillsRows) {
            Long jobOrderId = ((Number) row[0]).longValue();
            String skill = (String) row[1];
            JobOrderResponse dto = dtoMap.get(jobOrderId);
            if (dto != null) {
                dto.getPreferredSkills().add(skill);
            }
        }

        List<JobOrderResponse> resultList = jobOrders.stream()
                .map(j -> dtoMap.get(j.getId()))
                .toList();

        return new PageImpl<>(resultList, pageable, jobOrdersPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobOrderResponse> getOpenJobOrders(Pageable pageable) {
        log.info("Fetching open job orders");

        Page<JobOrder> jobOrders = jobOrderRepository.findAllOpenJobs(pageable);
        return jobOrders.map(jobOrderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobOrderResponse> getJobOrdersByStatus(String status, Pageable pageable) {
        log.info("Fetching job orders with status: {}", status);

        Page<JobOrder> jobOrders = jobOrderRepository.findByStatus(status, pageable);
        return jobOrders.map(jobOrderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobOrderResponse> searchJobOrders(String query, Pageable pageable) {
        log.info("Searching job orders with query: {}", query);

        Page<JobOrder> jobOrders = jobOrderRepository.searchJobOrders(query, pageable);
        return jobOrders.map(jobOrderMapper::toResponse);
    }

    @Override
    @Transactional
    public void deleteJobOrder(Long id) {
        log.info("Soft deleting job order with ID: {}", id);

        JobOrder jobOrder = jobOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobOrder", "id", id));

        jobOrder.setIsDeleted(true);
        jobOrderRepository.save(jobOrder);

        log.info("Soft deleted job order with ID: {}", id);

        // Notify webhook subscribers
        JobOrderResponse response = jobOrderMapper.toResponse(jobOrder);
        webhookNotificationService.notifySubscribers(WebhookEventType.JOB_ORDER_DELETED, response);
    }

    @Override
    @Transactional
    public void hardDeleteJobOrder(Long id) {
        log.info("Hard deleting job order with ID: {}", id);

        if (!jobOrderRepository.existsById(id)) {
            throw new ResourceNotFoundException("JobOrder", "id", id);
        }

        jobOrderRepository.deleteById(id);
        log.info("Hard deleted job order with ID: {}", id);
    }
}
