package com.application.Servease.payment.dto;

import com.application.Servease.payment.enums.CollectionMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConfirmBookingCollectionRequest {

    @NotNull
    private CollectionMethod collectionMethod;
}