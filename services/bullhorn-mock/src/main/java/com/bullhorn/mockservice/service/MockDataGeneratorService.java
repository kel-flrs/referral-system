package com.bullhorn.mockservice.service;

import com.bullhorn.mockservice.entity.Candidate;
import com.bullhorn.mockservice.entity.Consultant;
import com.bullhorn.mockservice.entity.JobOrder;

import java.util.List;

/**
 * Service interface for generating mock data using JavaFaker
 */
public interface MockDataGeneratorService {

    /**
     * Generate mock candidates
     * @param count number of candidates to generate
     * @return list of generated candidates
     */
    List<Candidate> generateCandidates(int count);

    /**
     * Generate mock consultants
     * @param count number of consultants to generate
     * @return list of generated consultants
     */
    List<Consultant> generateConsultants(int count);

    /**
     * Generate mock job orders
     * @param count number of job orders to generate
     * @return list of generated job orders
     */
    List<JobOrder> generateJobOrders(int count);

    /**
     * Initialize database with mock data
     * @param candidateCount number of candidates
     * @param consultantCount number of consultants
     * @param jobOrderCount number of job orders
     */
    void initializeMockData(int candidateCount, int consultantCount, int jobOrderCount);
}
