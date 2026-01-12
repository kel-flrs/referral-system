package com.bullhorn.mockservice.mapper;

import com.bullhorn.mockservice.dto.response.ExperienceDto;
import com.bullhorn.mockservice.entity.Experience;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for Experience entity
 */
@Mapper(componentModel = "spring")
public interface ExperienceMapper {

    ExperienceDto toDto(Experience experience);

    List<ExperienceDto> toDtoList(List<Experience> experiences);
}
