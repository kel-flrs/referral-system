package com.bullhorn.mockservice.service;

import com.bullhorn.mockservice.dto.request.CreateConsultantRequest;
import com.bullhorn.mockservice.dto.request.UpdateConsultantRequest;
import com.bullhorn.mockservice.dto.response.ConsultantResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Consultant operations
 */
public interface ConsultantService {

    ConsultantResponse createConsultant(CreateConsultantRequest request);

    ConsultantResponse updateConsultant(Long id, UpdateConsultantRequest request);

    ConsultantResponse getConsultantById(Long id);

    ConsultantResponse getConsultantByBullhornId(String bullhornId);

    Page<ConsultantResponse> getAllConsultants(Pageable pageable);

    Page<ConsultantResponse> getActiveConsultants(Pageable pageable);

    Page<ConsultantResponse> searchConsultants(String query, Pageable pageable);

    void deleteConsultant(Long id);
}
