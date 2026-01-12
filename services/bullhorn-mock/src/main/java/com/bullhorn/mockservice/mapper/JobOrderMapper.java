package com.bullhorn.mockservice.mapper;

import com.bullhorn.mockservice.dto.request.CreateJobOrderRequest;
import com.bullhorn.mockservice.dto.request.UpdateJobOrderRequest;
import com.bullhorn.mockservice.dto.response.JobOrderResponse;
import com.bullhorn.mockservice.entity.JobOrder;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for JobOrder entity
 */
@Mapper(componentModel = "spring")
public interface JobOrderMapper {

    JobOrderResponse toResponse(JobOrder jobOrder);

    List<JobOrderResponse> toResponseList(List<JobOrder> jobOrders);

    @Mapping(target = "bullhornId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    JobOrder toEntity(CreateJobOrderRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "bullhornId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    void updateEntityFromRequest(UpdateJobOrderRequest request, @MappingTarget JobOrder jobOrder);
}
