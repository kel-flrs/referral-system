package com.bullhorn.mockservice.service.impl;

import com.bullhorn.mockservice.entity.*;
import com.bullhorn.mockservice.repository.CandidateRepository;
import com.bullhorn.mockservice.repository.ConsultantRepository;
import com.bullhorn.mockservice.repository.JobOrderRepository;
import com.bullhorn.mockservice.service.MockDataGeneratorService;
import com.bullhorn.mockservice.service.PostgresCopyBulkInsertService;
import com.bullhorn.mockservice.util.Constants;
import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of MockDataGeneratorService using JavaFaker
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockDataGeneratorServiceImpl implements MockDataGeneratorService {

    private final Faker faker = new Faker();
    private final CandidateRepository candidateRepository;
    private final ConsultantRepository consultantRepository;
    private final JobOrderRepository jobOrderRepository;
    private final PostgresCopyBulkInsertService copyService;

    @Value("${app.mock-data.use-postgres-copy:true}")
    private boolean usePostgresCopy;

    private int candidateCounter = 1;
    private int consultantCounter = 1;
    private int jobOrderCounter = 1;

    @Override
    public List<Candidate> generateCandidates(int count) {
        log.info("Generating {} mock candidates", count);
        List<Candidate> candidates = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String bullhornId = String.valueOf(10000 + candidateCounter);

            Candidate candidate = Candidate.builder()
                    .bullhornId(bullhornId)
                    .firstName(faker.name().firstName())
                    .lastName(faker.name().lastName())
                    .email(String.format("candidate%d@example.com", candidateCounter))
                    .phone(String.format("+1%010d", candidateCounter))
                    .currentTitle(faker.job().title())
                    .currentCompany(faker.company().name())
                    .location(faker.address().city())
                    .skills(generateSkills())
                    .summary(faker.lorem().sentence(15))
                    .status(Constants.STATUS_ACTIVE)
                    .isDeleted(false)
                    .build();

            // Add experiences
            List<Experience> experiences = generateExperiences(candidate);
            candidate.setExperience(experiences);

            // Add education
            List<Education> educations = generateEducations(candidate);
            candidate.setEducation(educations);

            candidates.add(candidate);
            candidateCounter++;
        }

        return candidates;
    }

    @Override
    public List<Consultant> generateConsultants(int count) {
        log.info("Generating {} mock consultants", count);
        List<Consultant> consultants = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String bullhornId = String.valueOf(20000 + consultantCounter);

            Consultant consultant = Consultant.builder()
                    .bullhornId(bullhornId)
                    .firstName(faker.name().firstName())
                    .lastName(faker.name().lastName())
                    .email(String.format("consultant%d@example.com", consultantCounter))
                    .phone(String.format("+2%010d", consultantCounter))
                    .isActive(true)
                    .lastActivityAt(convertToLocalDateTime(faker.date().past(30, TimeUnit.DAYS)))
                    .totalPlacements(faker.number().numberBetween(0, 100))
                    .totalReferrals(faker.number().numberBetween(0, 50))
                    .build();

            consultants.add(consultant);
            consultantCounter++;
        }

        return consultants;
    }

    @Override
    public List<JobOrder> generateJobOrders(int count) {
        log.info("Generating {} mock job orders", count);
        List<JobOrder> jobOrders = new ArrayList<>();

        String[] employmentTypes = {
                Constants.EMPLOYMENT_FULL_TIME,
                Constants.EMPLOYMENT_PART_TIME,
                Constants.EMPLOYMENT_CONTRACT,
                Constants.EMPLOYMENT_TEMPORARY
        };

        String[] experienceLevels = {
                Constants.LEVEL_ENTRY,
                Constants.LEVEL_JUNIOR,
                Constants.LEVEL_MID,
                Constants.LEVEL_SENIOR,
                Constants.LEVEL_LEAD,
                Constants.LEVEL_PRINCIPAL
        };

        for (int i = 0; i < count; i++) {
            String bullhornId = String.valueOf(30000 + jobOrderCounter);
            String clientBullhornId = String.valueOf(40000 + jobOrderCounter);

            JobOrder jobOrder = JobOrder.builder()
                    .bullhornId(bullhornId)
                    .title(faker.job().title())
                    .description(faker.lorem().paragraph(3))
                    .employmentType(employmentTypes[faker.random().nextInt(employmentTypes.length)])
                    .requiredSkills(new HashSet<>(generateSkills()))
                    .preferredSkills(new HashSet<>(generateSkills()))
                    .experienceLevel(experienceLevels[faker.random().nextInt(experienceLevels.length)])
                    .location(faker.address().city())
                    .salary(new BigDecimal(faker.number().numberBetween(50000, 200000)))
                    .clientName(faker.company().name())
                    .clientBullhornId(clientBullhornId)
                    .status(Constants.STATUS_OPEN)
                    .openDate(convertToLocalDate(faker.date().past(60, TimeUnit.DAYS)))
                    .closeDate(convertToLocalDate(faker.date().future(180, TimeUnit.DAYS)))
                    .isDeleted(false)
                    .build();

            jobOrders.add(jobOrder);
            jobOrderCounter++;
        }

        return jobOrders;
    }

    @Override
    @Transactional
    public void initializeMockData(int candidateCount, int consultantCount, int jobOrderCount) {
        log.info("Initializing mock data - Candidates: {}, Consultants: {}, JobOrders: {}",
                candidateCount, consultantCount, jobOrderCount);
        log.info("Mode: {} (PostgreSQL COPY)", usePostgresCopy ? "TURBO" : "STANDARD JPA");

        // Check if data already exists
        long existingCandidates = candidateRepository.count();
        long existingConsultants = consultantRepository.count();
        long existingJobOrders = jobOrderRepository.count();

        if (existingCandidates > 0 || existingConsultants > 0 || existingJobOrders > 0) {
            log.info("Mock data already exists. Skipping initialization.");
            return;
        }

        long totalStartTime = System.currentTimeMillis();

        if (usePostgresCopy) {
            // TURBO MODE: Use PostgreSQL COPY for 10-50x faster inserts
            initializeWithPostgresCopy(candidateCount, consultantCount, jobOrderCount);
        } else {
            // STANDARD MODE: Use JPA with batch inserts
            initializeWithJpa(candidateCount, consultantCount, jobOrderCount);
        }

        long totalDuration = System.currentTimeMillis() - totalStartTime;
        int totalRecords = candidateCount + consultantCount + jobOrderCount;
        double overallRate = (totalRecords * 1000.0) / totalDuration;

        log.info("========================================");
        log.info("Mock data initialization completed!");
        log.info("Total records: {}", totalRecords);
        log.info("Total time: {}s", totalDuration / 1000);
        log.info("Overall rate: {:.0f} records/sec", overallRate);
        log.info("========================================");
    }

    /**
     * TURBO MODE: Initialize data using PostgreSQL COPY command
     * Expected: 500k records in 30-60 seconds
     */
    private void initializeWithPostgresCopy(int candidateCount, int consultantCount, int jobOrderCount) {
        log.info("Using TURBO MODE (PostgreSQL COPY)");

        // For COPY, we can use larger batch sizes (less memory sensitive)
        int batchSize = 10000;

        // Generate and insert consultants
        if (consultantCount > 0) {
            log.info("Generating {} consultants...", consultantCount);
            List<Consultant> consultants = generateConsultants(consultantCount);
            assignSequenceIds(consultants, "base_sequence");
            copyService.bulkInsertConsultantsWithCopy(consultants);
        }

        // Generate and insert job orders
        if (jobOrderCount > 0) {
            log.info("Generating {} job orders...", jobOrderCount);
            List<JobOrder> jobOrders = generateJobOrders(jobOrderCount);
            assignSequenceIds(jobOrders, "base_sequence");
            copyService.bulkInsertJobOrdersWithCopy(jobOrders);
        }

        // Generate and insert candidates in batches (to avoid memory issues with large counts)
        if (candidateCount > 0) {
            log.info("Generating {} candidates...", candidateCount);
            int remaining = candidateCount;
            int processed = 0;

            while (remaining > 0) {
                int currentBatch = Math.min(batchSize, remaining);
                List<Candidate> candidates = generateCandidates(currentBatch);
                assignSequenceIds(candidates, "base_sequence");
                copyService.bulkInsertCandidatesWithCopy(candidates);

                processed += currentBatch;
                remaining -= currentBatch;
                log.info("Progress: {}/{} candidates inserted", processed, candidateCount);
            }
        }
    }

    /**
     * STANDARD MODE: Initialize data using JPA batch inserts
     * Expected: 500k records in 8-12 minutes
     */
    private void initializeWithJpa(int candidateCount, int consultantCount, int jobOrderCount) {
        log.info("Using STANDARD MODE (JPA batch inserts)");

        // Generate and save in batches aligned with hibernate.jdbc.batch_size
        int batchSize = 500;

        // Generate Consultants first (no dependencies)
        generateAndSaveBatch(consultantCount, batchSize, this::generateConsultants,
                consultantRepository::saveAll, "Consultants");

        // Generate Job Orders (no dependencies)
        generateAndSaveBatch(jobOrderCount, batchSize, this::generateJobOrders,
                jobOrderRepository::saveAll, "Job Orders");

        // Generate Candidates with nested entities (Experience, Education)
        // Using smaller batch size for candidates due to cascade operations
        generateAndSaveBatch(candidateCount, 200, this::generateCandidates,
                candidateRepository::saveAll, "Candidates");
    }

    /**
     * Assign sequential IDs from PostgreSQL sequence to entities
     * Required for COPY mode to work with relationships
     */
    private <T extends BaseEntity> void assignSequenceIds(List<T> entities, String sequenceName) {
        if (entities.isEmpty()) {
            return;
        }

        Long[] ids = copyService.allocateSequenceValues(sequenceName, entities.size());
        for (int i = 0; i < entities.size(); i++) {
            entities.get(i).setId(ids[i]);
        }
    }

    // Helper methods

    private List<String> generateSkills() {
        int skillCount = faker.number().numberBetween(2, 5);
        List<String> skills = new ArrayList<>();

        String[] skillPool = {
                "Java", "Python", "JavaScript", "React", "Angular", "Vue.js", "Node.js",
                "Spring Boot", "Django", "Flask", "SQL", "PostgreSQL", "MongoDB",
                "Docker", "Kubernetes", "AWS", "Azure", "GCP", "Git", "CI/CD",
                "Microservices", "REST APIs", "GraphQL", "Agile", "Scrum"
        };

        while (skills.size() < skillCount) {
            skills.add(skillPool[faker.random().nextInt(skillPool.length)]);
        }

        return skills;
    }

    private List<Experience> generateExperiences(Candidate candidate) {
        int expCount = faker.number().numberBetween(1, 3);
        List<Experience> experiences = new ArrayList<>();

        for (int i = 0; i < expCount; i++) {
            Experience experience = Experience.builder()
                    .candidate(candidate)
                    .companyName(faker.company().name())
                    .jobTitle(faker.job().title())
                    .startDate(convertToLocalDate(faker.date().past(1825, TimeUnit.DAYS)))
                    .endDate(convertToLocalDate(faker.date().past(365, TimeUnit.DAYS)))
                    .description(faker.lorem().paragraph())
                    .build();
            experiences.add(experience);
        }

        return experiences;
    }

    private List<Education> generateEducations(Candidate candidate) {
        int eduCount = faker.number().numberBetween(1, 2);
        List<Education> educations = new ArrayList<>();

        for (int i = 0; i < eduCount; i++) {
            Education education = Education.builder()
                    .candidate(candidate)
                    .school(faker.university().name())
                    .degree(faker.educator().course())
                    .startDate(convertToLocalDate(faker.date().past(3650, TimeUnit.DAYS)))
                    .endDate(convertToLocalDate(faker.date().past(1825, TimeUnit.DAYS)))
                    .fieldOfStudy(faker.educator().campus())
                    .build();
            educations.add(education);
        }

        return educations;
    }

    private LocalDate convertToLocalDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private java.time.LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private <T> void generateAndSaveBatch(
            int totalCount,
            int batchSize,
            java.util.function.Function<Integer, List<T>> generator,
            java.util.function.Consumer<List<T>> saver,
            String entityName
    ) {
        int remaining = totalCount;
        int processed = 0;
        long startTime = System.currentTimeMillis();

        while (remaining > 0) {
            int currentBatchSize = Math.min(batchSize, remaining);
            long batchStartTime = System.currentTimeMillis();

            List<T> batch = generator.apply(currentBatchSize);
            saver.accept(batch);

            processed += currentBatchSize;
            remaining -= currentBatchSize;

            long batchDuration = System.currentTimeMillis() - batchStartTime;
            double itemsPerSecond = (currentBatchSize * 1000.0) / batchDuration;

            log.info("Generated and saved {}/{} {} (batch: {} items in {}ms, rate: {:.0f} items/sec)",
                    processed, totalCount, entityName, currentBatchSize, batchDuration, itemsPerSecond);
        }

        long totalDuration = System.currentTimeMillis() - startTime;
        double overallRate = (totalCount * 1000.0) / totalDuration;
        log.info("Completed {} {} in {}ms (overall rate: {:.0f} items/sec)",
                totalCount, entityName, totalDuration, overallRate);
    }
}
