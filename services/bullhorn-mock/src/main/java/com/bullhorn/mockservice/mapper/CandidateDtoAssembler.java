package com.bullhorn.mockservice.mapper;

import com.bullhorn.mockservice.dto.response.CandidateResponse;
import com.bullhorn.mockservice.dto.response.EducationDto;
import com.bullhorn.mockservice.dto.response.ExperienceDto;
import com.bullhorn.mockservice.entity.Candidate;
import com.bullhorn.mockservice.entity.Education;
import com.bullhorn.mockservice.entity.Experience;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CandidateDtoAssembler {

    public Map<Long, CandidateResponse> initDtos(List<Candidate> candidates) {
        Map<Long, CandidateResponse> map = new HashMap<>();
        for (Candidate candidate : candidates) {
            CandidateResponse dto = CandidateResponse.builder()
                    .id(candidate.getId())
                    .bullhornId(candidate.getBullhornId())
                    .firstName(candidate.getFirstName())
                    .lastName(candidate.getLastName())
                    .email(candidate.getEmail())
                    .phone(candidate.getPhone())
                    .currentTitle(candidate.getCurrentTitle())
                    .currentCompany(candidate.getCurrentCompany())
                    .location(candidate.getLocation())
                    .summary(candidate.getSummary())
                    .status(candidate.getStatus())
                    .lastSyncedAt(candidate.getLastSyncedAt())
                    .createdAt(candidate.getCreatedAt())
                    .updatedAt(candidate.getUpdatedAt())
                    .skills(new HashSet<>())
                    .experience(new ArrayList<>())
                    .education(new ArrayList<>())
                    .build();

            map.put(candidate.getId(), dto);
        }
        return map;
    }

    public void addExperience(Map<Long, CandidateResponse> map, List<Experience> experiences) {
        for (Experience e : experiences) {
            ExperienceDto dto = ExperienceDto.builder()
                    .id(e.getId())
                    .companyName(e.getCompanyName())
                    .jobTitle(e.getJobTitle())
                    .startDate(e.getStartDate())
                    .endDate(e.getEndDate())
                    .description(e.getDescription())
                    .build();

            map.get(e.getCandidate().getId()).getExperience().add(dto);
        }
    }

    public void addEducation(Map<Long, CandidateResponse> map, List<Education> educations) {
        for (Education ed : educations) {
            EducationDto dto = EducationDto.builder()
                    .id(ed.getId())
                    .school(ed.getSchool())
                    .degree(ed.getDegree())
                    .startDate(ed.getStartDate())
                    .endDate(ed.getEndDate())
                    .fieldOfStudy(ed.getFieldOfStudy())
                    .build();

            map.get(ed.getCandidate().getId()).getEducation().add(dto);
        }
    }

    public void addSkills(Map<Long, CandidateResponse> map, List<Object[]> skills) {
        for (Object[] row : skills) {
            Long candidateId = ((Number) row[0]).longValue();
            String skill = (String) row[1];
            map.get(candidateId).getSkills().add(skill);
        }
    }
}

