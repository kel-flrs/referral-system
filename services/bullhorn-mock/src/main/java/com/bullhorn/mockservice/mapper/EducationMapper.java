package com.bullhorn.mockservice.mapper;

import com.bullhorn.mockservice.dto.response.EducationDto;
import com.bullhorn.mockservice.entity.Education;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for Education entity
 */
@Mapper(componentModel = "spring")
public interface EducationMapper {

    EducationDto toDto(Education education);

    List<EducationDto> toDtoList(List<Education> educations);
}
