package com.bullhorn.mockservice.mapper;

import com.bullhorn.mockservice.dto.request.CreateConsultantRequest;
import com.bullhorn.mockservice.dto.request.UpdateConsultantRequest;
import com.bullhorn.mockservice.dto.response.ConsultantResponse;
import com.bullhorn.mockservice.entity.Consultant;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for Consultant entity
 */
@Mapper(componentModel = "spring")
public interface ConsultantMapper {

    ConsultantResponse toResponse(Consultant consultant);

    List<ConsultantResponse> toResponseList(List<Consultant> consultants);

    @Mapping(target = "bullhornId", ignore = true)
    @Mapping(target = "lastActivityAt", ignore = true)
    @Mapping(target = "totalPlacements", ignore = true)
    @Mapping(target = "totalReferrals", ignore = true)
    Consultant toEntity(CreateConsultantRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "bullhornId", ignore = true)
    @Mapping(target = "lastActivityAt", ignore = true)
    @Mapping(target = "totalPlacements", ignore = true)
    @Mapping(target = "totalReferrals", ignore = true)
    void updateEntityFromRequest(UpdateConsultantRequest request, @MappingTarget Consultant consultant);
}
