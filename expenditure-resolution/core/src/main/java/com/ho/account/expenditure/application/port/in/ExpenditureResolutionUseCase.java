package com.ho.account.expenditure.application.port.in;

import com.ho.account.expenditure.domain.ExpenditureResolution;
import java.time.LocalDate;
import java.util.List;

public interface ExpenditureResolutionUseCase {
    ExpenditureResolution createResolution(ExpenditureResolutionCommand command);
    ExpenditureResolution createResolution(ExpenditureResolution resolution);
    ExpenditureResolution updateResolution(Long id, ExpenditureResolutionCommand command);
    void requestApproval(Long id);
    void approveResolution(Long id);
    void rejectResolution(Long id, String reason);
    ExpenditureResolution getResolution(Long id);
    List<ExpenditureResolution> getResolutionsByDate(LocalDate startDate, LocalDate endDate);
}