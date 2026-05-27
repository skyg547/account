package com.ho.account.ecl.api.controller;

import com.ho.account.ecl.core.domain.model.CrGradeMaster;
import com.ho.account.ecl.core.domain.model.CrLgdSegmentMaster;
import com.ho.account.ecl.core.domain.model.CrRegulatoryParameter;
import com.ho.account.ecl.core.application.port.out.CrGradeMasterRepository;
import com.ho.account.ecl.core.application.port.out.CrLgdSegmentMasterRepository;
import com.ho.account.ecl.core.application.port.out.CrRegulatoryParameterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [API] 대손충당금(IFRS9) 메타데이터 및 마스터 관리 컨트롤러.
 * 신용등급별 부도율(PD), 담보별 손실률(LGD), 규제 파라미터 등 대손충당금(IFRS9) 산출의 '기준 정보'를 관리합니다.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 대손충당금(IFRS9) 산출을 위해서는 "만약 ~한다면"에 대한 기준값이 필요합니다.
 * "AAA 등급 고객은 통계적으로 몇 % 확률로 부도가 나는가?", "아파트 담보를 잡으면 나중에 얼마를 회수할 수 있는가?"
 * 같은 약속된 값들을 메타데이터 또는 마스터라고 합니다. 이 값들이 정확해야 전체 대손충당금(IFRS9) 산출 결과가 신뢰를 얻을 수 있습니다.
 */
@RestController
@RequestMapping("/api/v1/credit-risk/metadata")
@RequiredArgsConstructor

public class CrMetadataController {

    private final CrGradeMasterRepository gradeRepository;
    private final CrLgdSegmentMasterRepository lgdRepository;
    private final CrRegulatoryParameterRepository parameterRepository;

    @GetMapping("/grades")
    public List<CrGradeMaster> getAllGrades() {
        return gradeRepository.findAll();
    }

    @GetMapping("/lgd-segments")
    public List<CrLgdSegmentMaster> getAllLgdSegments() {
        return lgdRepository.findAll();
    }

    @GetMapping("/parameters")
    public List<CrRegulatoryParameter> getAllParameters() {
        return parameterRepository.findAll();
    }

    @PostMapping("/parameters")
    public CrRegulatoryParameter updateParameter(@RequestBody @NonNull CrRegulatoryParameter param) {
        return parameterRepository.save(param);
    }
}
