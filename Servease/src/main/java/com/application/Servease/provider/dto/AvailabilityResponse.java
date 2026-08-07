package com.application.Servease.provider.dto;

import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Builder
public class AvailabilityResponse {

    private Long id;

    private DayOfWeek dayOfWeek;

    private Boolean available;

    private LocalTime startTime;

    private LocalTime endTime;
}