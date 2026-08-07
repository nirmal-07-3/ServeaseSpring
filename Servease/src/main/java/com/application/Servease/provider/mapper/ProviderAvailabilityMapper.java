package com.application.Servease.provider.mapper;

import com.application.Servease.provider.dto.AvailabilityResponse;
import com.application.Servease.provider.dto.SaveAvailabilityRequest;
import com.application.Servease.provider.entity.ProviderAvailability;
import org.springframework.stereotype.Component;

@Component
public class ProviderAvailabilityMapper {

    public AvailabilityResponse toResponse(
            ProviderAvailability availability) {

        return AvailabilityResponse.builder()
                .id(availability.getId())
                .dayOfWeek(availability.getDayOfWeek())
                .available(availability.getAvailable())
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .build();
    }

    public void updateEntity(
            SaveAvailabilityRequest request,
            ProviderAvailability availability) {

        availability.setDayOfWeek(request.getDayOfWeek());

        availability.setAvailable(request.getAvailable());

        availability.setStartTime(request.getStartTime());

        availability.setEndTime(request.getEndTime());
    }
}