package com.tracenest.backend.Subscription.mapper;

import com.tracenest.backend.Subscription.dto.SubscriptionDto;
import com.tracenest.backend.Subscription.model.Subscription;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {
    SubscriptionDto toDto(Subscription subscription);
}
