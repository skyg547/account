package com.ho.account.expenditure.application.port.in;

import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.dto.ExpenditureResolutionDto;
import com.ho.account.expenditure.dto.ExpenditureResolutionRequestDto;
import java.time.LocalDate;
import java.util.List;

public interface ExpenditureResolutionUseCase {
    ExpenditureResolution createResolution(ExpenditureResolutionRequestDto requestDto);
    ExpenditureResolution createResolution(ExpenditureResolution resolution);
    ExpenditureResolution updateResolution(Long id, ExpenditureResolutionRequestDto requestDto);
    void requestApproval(Long id);
    void approveResolution(Long id);
    void rejectResolution(Long id, String reason);
    ExpenditureResolution getResolution(Long id);
    List<ExpenditureResolution> getResolutionsByDate(LocalDate startDate, LocalDate endDate);
    ExpenditureResolutionDto toDto(ExpenditureResolution resolution);
    List<ExpenditureResolutionDto> toDtoList(List<ExpenditureResolution> resolutions);
}
