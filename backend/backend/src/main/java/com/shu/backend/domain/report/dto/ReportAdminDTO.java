package com.shu.backend.domain.report.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public class ReportAdminDTO {

    @Getter
    public static class ApproveRequest{
        @NotNull
        @Min(1)
        private Integer penaltyDays;
    }
}
