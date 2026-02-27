package com.coiflow.mapper;

import com.coiflow.dto.salon.SalonResponse;
import com.coiflow.model.salon.Salon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SalonMapper {

    @Mapping(target = "managerId", ignore = true)
    @Mapping(target = "managerName", ignore = true)
    @Mapping(target = "createdAt", expression = "java(salon.getCreatedAt() != null ? salon.getCreatedAt().toString() : null)")
    SalonResponse toResponse(Salon salon);
}
