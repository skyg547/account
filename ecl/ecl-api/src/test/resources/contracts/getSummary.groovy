import org.springframework.cloud.contract.spec.Contract

/**
 * [Contract] 신용리스크 요약 정보 조회 약속
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 이 문서는 게이트웨이와 신용리스크 팀 간의 '계약서'입니다.
 * "요청이 /api/v1/credit-risk/summary 로 오면, 
 * 성공(200) 응답과 함께 baseDate, totalEad 등의 데이터를 주겠다"는 약속입니다.
 */
Contract.make {
    description("신용리스크 전사 요약 지표를 조회하는 API 계약")
    
    request {
        method GET()
        url("/api/v1/credit-risk/summary") {
            queryParameters {
                parameter("baseDate": "2026-04-20")
            }
        }
    }
    
    response {
        status OK()
        body([
            status: "SUCCESS",
            message: "요청이 성공적으로 처리되었습니다.",
            data: [
                baseDate: "2026-04-20",
                totalEad: 1000000.0,
                totalRwaIrb: 500000.0,
                avgPd: 0.02
            ]
        ])
        headers {
            contentType(applicationJson())
        }
    }
}
