package com.tracenest.backend.Report.mapper;

import com.tracenest.backend.Report.dto.ReportDto;
import com.tracenest.backend.Report.model.Report;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReportMapper {

    @Mapping(source = "reporter.id", target = "reporter.id")
    @Mapping(source = "reporter.name", target = "reporter.name")
    @Mapping(source = "reporter.avatarUrl", target = "reporter.avatarUrl")
    ReportDto reportToReportDto(Report report);

    @AfterMapping
    default void filterReward(@MappingTarget ReportDto dto, Report report) {
        if (dto.getReward() != null && dto.getReward() <= 0) {
            dto.setReward(null);
        }
    }
}
