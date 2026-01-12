package com.bullhorn.mockservice.service.impl;

import com.bullhorn.mockservice.dto.request.CreateConsultantRequest;
import com.bullhorn.mockservice.dto.request.UpdateConsultantRequest;
import com.bullhorn.mockservice.dto.response.ConsultantResponse;
import com.bullhorn.mockservice.entity.Consultant;
import com.bullhorn.mockservice.exception.DuplicateResourceException;
import com.bullhorn.mockservice.exception.ResourceNotFoundException;
import com.bullhorn.mockservice.mapper.ConsultantMapper;
import com.bullhorn.mockservice.repository.ConsultantRepository;
import com.bullhorn.mockservice.service.ConsultantService;
import com.bullhorn.mockservice.service.webhook.WebhookNotificationService;
import com.bullhorn.mockservice.webhook.WebhookEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of ConsultantService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultantServiceImpl implements ConsultantService {

    private final ConsultantRepository consultantRepository;
    private final ConsultantMapper consultantMapper;
    private final WebhookNotificationService webhookNotificationService;

    @Override
    @Transactional
    public ConsultantResponse createConsultant(CreateConsultantRequest request) {
        log.info("Creating new consultant with email: {}", request.getEmail());

        consultantRepository.findByEmail(request.getEmail()).ifPresent(c -> {
            throw new DuplicateResourceException("Consultant", "email", request.getEmail());
        });

        Consultant consultant = consultantMapper.toEntity(request);
        consultant.setBullhornId(UUID.randomUUID().toString());
        consultant.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        Consultant savedConsultant = consultantRepository.save(consultant);
        log.info("Created consultant with ID: {}", savedConsultant.getId());

        // Notify webhook subscribers
        ConsultantResponse response = consultantMapper.toResponse(savedConsultant);
        webhookNotificationService.notifySubscribers(WebhookEventType.CONSULTANT_CREATED, response);

        return response;
    }

    @Override
    @Transactional
    public ConsultantResponse updateConsultant(Long id, UpdateConsultantRequest request) {
        log.info("Updating consultant with ID: {}", id);

        Consultant consultant = consultantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultant", "id", id));

        if (request.getEmail() != null && !request.getEmail().equals(consultant.getEmail())) {
            if (consultantRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
                throw new DuplicateResourceException("Consultant", "email", request.getEmail());
            }
        }

        consultantMapper.updateEntityFromRequest(request, consultant);
        Consultant updatedConsultant = consultantRepository.save(consultant);

        log.info("Updated consultant with ID: {}", updatedConsultant.getId());

        // Notify webhook subscribers
        ConsultantResponse response = consultantMapper.toResponse(updatedConsultant);
        webhookNotificationService.notifySubscribers(WebhookEventType.CONSULTANT_UPDATED, response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultantResponse getConsultantById(Long id) {
        log.info("Fetching consultant with ID: {}", id);

        Consultant consultant = consultantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultant", "id", id));

        return consultantMapper.toResponse(consultant);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultantResponse getConsultantByBullhornId(String bullhornId) {
        log.info("Fetching consultant with Bullhorn ID: {}", bullhornId);

        Consultant consultant = consultantRepository.findByBullhornId(bullhornId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultant", "bullhornId", bullhornId));

        return consultantMapper.toResponse(consultant);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultantResponse> getAllConsultants(Pageable pageable) {
        log.info("Fetching all consultants");

        Page<Consultant> consultants = consultantRepository.findAll(pageable);
        return consultants.map(consultantMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultantResponse> getActiveConsultants(Pageable pageable) {
        log.info("Fetching active consultants");

        Page<Consultant> consultants = consultantRepository.findAllActive(pageable);
        return consultants.map(consultantMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConsultantResponse> searchConsultants(String query, Pageable pageable) {
        log.info("Searching consultants with query: {}", query);

        Page<Consultant> consultants = consultantRepository.searchConsultants(query, pageable);
        return consultants.map(consultantMapper::toResponse);
    }

    @Override
    @Transactional
    public void deleteConsultant(Long id) {
        log.info("Deleting consultant with ID: {}", id);

        Consultant consultant = consultantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultant", "id", id));

        // Convert to response before deleting
        ConsultantResponse response = consultantMapper.toResponse(consultant);

        consultantRepository.deleteById(id);
        log.info("Deleted consultant with ID: {}", id);

        // Notify webhook subscribers
        webhookNotificationService.notifySubscribers(WebhookEventType.CONSULTANT_DELETED, response);
    }
}
