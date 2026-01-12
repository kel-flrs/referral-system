package com.bullhorn.mockservice.mapper;

import com.bullhorn.mockservice.dto.request.CreateCandidateRequest;
import com.bullhorn.mockservice.dto.request.UpdateCandidateRequest;
import com.bullhorn.mockservice.dto.response.CandidateResponse;
import com.bullhorn.mockservice.entity.Candidate;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for Candidate entity
 */
@Mapper(componentModel = "spring", uses = {ExperienceMapper.class, EducationMapper.class})
public interface CandidateMapper {

    CandidateResponse toResponse(Candidate candidate);

    List<CandidateResponse> toResponseList(List<Candidate> candidates);

    @Mapping(target = "bullhornId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "experience", ignore = true)
    @Mapping(target = "education", ignore = true)
    Candidate toEntity(CreateCandidateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "bullhornId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "experience", ignore = true)
    @Mapping(target = "education", ignore = true)
    void updateEntityFromRequest(UpdateCandidateRequest request, @MappingTarget Candidate candidate);
}
