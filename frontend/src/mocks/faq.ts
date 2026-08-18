export interface FaqItem {
  id: string;
  category: FaqCategory;
  question: string;
  answer: {
    summary: string;
    steps?: string[];
    notice?: string;
    tips?: string[];
    relatedLink?: {
      label: string;
      href: string;
    };
  };
  isPopular?: boolean;
  viewCount: number;
  helpfulCount: number;
  unhelpfulCount: number;
  tags: string[];
}

export type FaqCategory =
  | 'ALL'
  | 'POPULAR'
  | 'JOURNAL_LEDGER'
  | 'CLOSING'
  | 'LOAN'
  | 'DEPOSIT'
  | 'EXPENDITURE'
  | 'TAX'
  | 'AUTH_SECURITY'
  | 'SYSTEM';

export interface CategoryInfo {
  id: FaqCategory;
  label: string;
  iconName: string;
  description: string;
}

export const FAQ_CATEGORIES: CategoryInfo[] = [
  { id: 'ALL', label: '전체', iconName: 'Grid', description: '전체 자주 묻는 질문' },
  { id: 'POPULAR', label: '자주 묻는 질문', iconName: 'Flame', description: '가장 많이 찾는 질문' },
  { id: 'JOURNAL_LEDGER', label: '전표/원장', iconName: 'BookOpen', description: '전표 작성, 승인, 총계정원장' },
  { id: 'CLOSING', label: '결산/마감', iconName: 'CalendarCheck', description: '일마감, 월결산, 재무제표 확정' },
  { id: 'LOAN', label: '여신/대출', iconName: 'Coins', description: '대출 실행, 이자 계산, 상환 스케줄' },
  { id: 'DEPOSIT', label: '수신/예금', iconName: 'Wallet', description: '예금 계좌 관리, 이자 지급, 거래내역' },
  { id: 'EXPENDITURE', label: '지출/경비', iconName: 'Receipt', description: '지출결의서, 예산 통제, 경비 청구' },
  { id: 'TAX', label: '세무/부가세', iconName: 'FileSpreadsheet', description: '전자세금계산서, 부가세 신고, 국세청 연동' },
  { id: 'AUTH_SECURITY', label: '인증/보안', iconName: 'ShieldCheck', description: '로그인, 2FA, 권한(RBAC), PAT 토큰' },
  { id: 'SYSTEM', label: '시스템/설정', iconName: 'Settings', description: '기준정보, 환율, 계정과목 매핑' },
];

export const POPULAR_SEARCH_TAGS: string[] = [
  '전표 승인 반려',
  '월결산 마감 해제',
  '대출 이자 계산식',
  '전자세금계산서 국세청 전송',
  '예산 잔액 초과',
  '2차 인증(2FA) 재설정',
  '계정과목 추가',
  '외화 환산 손익',
];

export const mockFaqList: FaqItem[] = [
  {
    id: 'faq-01',
    category: 'JOURNAL_LEDGER',
    question: '작성 완료된 전표를 수정하거나 취소(역분개)하려면 어떻게 해야 하나요?',
    isPopular: true,
    viewCount: 1420,
    helpfulCount: 382,
    unhelpfulCount: 8,
    tags: ['전표', '역분개', '수정', '취소', '대차차액'],
    answer: {
      summary: '이미 [확정(POSTED)] 또는 [승인(APPROVED)]된 전표는 회계 무결성 원칙에 따라 원본 직접 수정이 불가능하며, 반드시 역분개(Reversal Journal Entry) 전표를 생성하여 상계 처리해야 합니다.',
      steps: [
        '상단 메뉴 [재무회계 > 분개 관리 > 전표 조회/수정]으로 이동합니다.',
        '취소하고자 하는 대상 전표 번호를 검색 후 상세 모달을 엽니다.',
        '우측 상단의 [역분개 전표 생성] 버튼을 클릭합니다.',
        '역분개 일자(당일 또는 원거래일)와 사유를 입력한 후 [승인 요청]을 진행합니다.',
      ],
      notice: '검토대기(PENDING) 또는 작성중(DRAFT) 상태인 전표는 작성자 본인이 즉시 내용 수정 및 삭제가 가능합니다.',
      tips: [
        '역분개 생성 시 차변/대변 금액과 계정과목이 자동 반전되어 대차 차액 0이 보장됩니다.',
        '전표 승인 권한(SoD) 규정에 따라 본인이 작성한 전표는 직접 승인할 수 없습니다.',
      ],
      relatedLink: {
        label: '전표 조회/수정 바로가기',
        href: '/journal/list',
      },
    },
  },
  {
    id: 'faq-02',
    category: 'CLOSING',
    question: '월결산 마감(Period Lock) 후 긴급 전표를 추가해야 할 때 마감 해제는 어떻게 진행하나요?',
    isPopular: true,
    viewCount: 1290,
    helpfulCount: 310,
    unhelpfulCount: 12,
    tags: ['결산', '마감해제', '회계기간', 'Period Lock', '승인권한'],
    answer: {
      summary: '마감된 회계 기간에 전표를 추가 입력하려면 시스템 관리자 또는 최고재무책임자(CFO) 권한으로 일시적 [마감 잠금 해제(Unlock)] 승인을 받아야 합니다.',
      steps: [
        '[결산 관리 > 회계기간 잠금/해제] 메뉴로 이동합니다.',
        '해당 회계연도 및 월(Period)을 선택 후 [마감 해제 신청] 버튼을 누릅니다.',
        '해제 사유(감사 지적사항 수정, 세무 조정 전표 반영 등)와 해제 희망 기한을 입력합니다.',
        '결산 승인권자의 2단계 전자결재가 완료되면 상태가 [OPEN_TEMPORARY]로 전환됩니다.',
      ],
      notice: '마감 해제 상태에서 작업이 완료되면 반드시 즉시 [재마감 및 잔액 재집계(Re-aggregation)] 배치를 실행해야 원장 불일치를 방지할 수 있습니다.',
      tips: ['재마감 후에는 결산 검증 게이트(Checklist)를 통해 총계정원장(GL)과 보조원장(SL) 일치 여부를 재검증하세요.'],
      relatedLink: {
        label: '회계기간 마감 관리 바로가기',
        href: '/closing/period-lock',
      },
    },
  },
  {
    id: 'faq-03',
    category: 'LOAN',
    question: '대출 이자 유효이자율법(EIR) 상각과 미수이자 일할 계산 방식이 궁금합니다.',
    isPopular: true,
    viewCount: 980,
    helpfulCount: 245,
    unhelpfulCount: 5,
    tags: ['대출', 'EIR', '유효이자율', '미수이자', '상환스케줄'],
    answer: {
      summary: 'Account.AI 여신 모듈은 IFRS 9 기준에 따라 대출 부대비용 및 차감항목을 반영한 유효이자율(EIR)을 산출하고, 1일 단위 일할 계산으로 미수이자를 매일 자동 분개합니다.',
      steps: [
        '대출 약정 등록 시 약정금액, 명목금리, 만기일, 거치기간, 부대수수료를 입력합니다.',
        '시스템이 내부수익률(IRR) 기반의 유효이자율(EIR) 및 상환 스케줄 테이블을 자동 생성합니다.',
        '매일 자정 실행되는 배치(loanInterestAccrualJob)가 각 계좌별 미수이자 및 이자수익을 자동 계상합니다.',
      ],
      notice: '원화(KRW) 대출은 원단위 절사(FLOOR) 정책이 적용되며, 외화(USD/EUR) 대출은 소수점 둘째 자리 반올림(HALF_UP) 규격이 엄격히 준수됩니다.',
      tips: ['대출 원리금 조기상환 시에는 상환 당일까지의 경과 일수에 대한 일할 이자가 자동 정산됩니다.'],
      relatedLink: {
        label: '대출 계약 관리 바로가기',
        href: '/loan/contracts',
      },
    },
  },
  {
    id: 'faq-04',
    category: 'TAX',
    question: '국세청 홈택스 전자세금계산서 발행 시 전송 결과 오류(Error)가 발생하면 어떻게 조치하나요?',
    isPopular: true,
    viewCount: 1150,
    helpfulCount: 298,
    unhelpfulCount: 7,
    tags: ['세금계산서', '국세청', '홈택스', 'NTS', '부가세'],
    answer: {
      summary: '전자세금계산서 국세청(NTS) 전송 오류는 주로 거래처 사업자등록번호 상태(휴폐업), 공급가액/세액 불일치, 또는 인증서 유효기간 만료로 인해 발생합니다.',
      steps: [
        '[세무 관리 > 국세청 전송 검증(NTS)] 메뉴에서 실패 목록을 조회합니다.',
        '해당 건의 [오류 코드 및 사유(Error Response)]를 확인합니다.',
        '거래처 사업자등록번호 유효성 검증 API를 재호출하여 휴폐업 여부를 확인합니다.',
        '필요 시 정정 세금계산서(수정발행)를 작성하거나 인증서 갱신 후 [재전송(Retry)]을 클릭합니다.',
      ],
      notice: '국세청 규정상 발행일의 다음 달 11일까지 국세청 전송이 완료되지 않으면 가산세가 부과될 수 있으므로 즉시 조치하시기 바랍니다.',
      tips: ['대량 세금계산서 전송은 500건 단위 페이징 배치로 안전하게 전송 처리됩니다.'],
      relatedLink: {
        label: '국세청 전송 검증 바로가기',
        href: '/tax/nts-verification',
      },
    },
  },
  {
    id: 'faq-05',
    category: 'EXPENDITURE',
    question: '지출결의서 작성 시 예산 초과(Budget Overrun) 경고가 발생했을 때 해결 방법은 무엇인가요?',
    isPopular: true,
    viewCount: 890,
    helpfulCount: 215,
    unhelpfulCount: 4,
    tags: ['지출결의', '예산통제', '예산초과', '예산전용'],
    answer: {
      summary: '지출결의 금액이 부서별/계정과목별 배정 예산 잔액을 초과할 경우, 시스템 예산 통제 정책에 의해 결재 상신이 차단됩니다.',
      steps: [
        '[자금운영 > 예산 관리 > 부서별 예산 현황]에서 배정 예산과 집행 잔액을 확인합니다.',
        '해당 예산 과목에 잔여 예산이 부족한 경우 [예산 전용(Budget Transfer) 신청]을 등록합니다.',
        '예산 총괄 부서의 전용 승인이 완료되면 추가 예산이 실시간 증액 배정됩니다.',
        '지출결의서로 돌아와 [예산 유효성 재검증]을 누른 후 상신합니다.',
      ],
      notice: '지출결의서가 반려(REJECTED)되면 가점유(Hold)되었던 예산 잔액은 자동으로 100% 원상 복구됩니다.',
      tips: ['긴급 지출 건의 경우 사전 배정된 예비비(Contingency Budget) 과목으로 신청할 수 있습니다.'],
      relatedLink: {
        label: '예산 집행 현황 바로가기',
        href: '/expenditure/budget',
      },
    },
  },
  {
    id: 'faq-06',
    category: 'AUTH_SECURITY',
    question: '로그인 2차 인증(OTP/2FA) 기기를 분실했거나 인증번호 수신이 되지 않습니다.',
    isPopular: true,
    viewCount: 1540,
    helpfulCount: 420,
    unhelpfulCount: 15,
    tags: ['2FA', 'OTP', '로그인', '인증기기', '비밀번호'],
    answer: {
      summary: '2차 인증 기기 분실 시에는 본인 확인 절차를 거쳐 보안 관리자가 2FA 등록을 초기화해 드립니다.',
      steps: [
        '로그인 화면 하단의 [2차 인증 초기화 요청] 링크를 클릭합니다.',
        '사번, 업무용 등록 이메일, 본인확인 정보를 입력하여 임시 복구 코드를 발급받습니다.',
        '또는 사내 정보보안팀(내선 8800) 또는 고객센터(1522-1000)로 신분증 사본과 함께 초기화 요청을 접수합니다.',
        '관리자 승인 후 로그인 시 새로운 Google Authenticator 또는 사내 OTP 앱으로 QR코드를 재스캔합니다.',
      ],
      notice: '비밀번호 5회 연속 오류 시 계정이 30분간 자동 잠금(Lock) 처리됩니다.',
      tips: ['초기 설정 시 제공되는 1회용 백업 복구 코드(8자리 10개)를 안전한 곳에 보관해 두시면 즉시 자체 복구가 가능합니다.'],
      relatedLink: {
        label: '로그인 화면 바로가기',
        href: '/login',
      },
    },
  },
  {
    id: 'faq-07',
    category: 'DEPOSIT',
    question: '수신 계좌의 입출금 거래 시 낙관적 잠금(Optimistic Lock) 충돌 오류가 발생하는 이유는 무엇인가요?',
    isPopular: false,
    viewCount: 620,
    helpfulCount: 180,
    unhelpfulCount: 2,
    tags: ['수신', '예금', '동시성', '낙관적락', '잔액업데이트'],
    answer: {
      summary: '동일 계좌에 대해 다수의 대량 이체 또는 자동이체가 1초 미만의 동일 시점에 동시 인입될 때, 데이터 정합성(Lost Update 방지)을 보장하기 위해 버전 충돌을 감지하고 안전하게 재시도(Retry)하는 정상적인 보호 메커니즘입니다.',
      steps: [
        '시스템은 내부적으로 지수 백오프(Exponential Backoff) 기반 최대 3회 자동 재시도를 수행합니다.',
        '지속적인 충돌로 실패 시 [수신 관리 > 거래내역 조회]에서 실제 이체 반영 여부를 먼저 확인하세요.',
        '미반영 건은 [재시도] 버튼을 누르거나 대량 이체 배치 주기를 분산 설정합니다.',
      ],
      notice: '계좌 잔액의 금융 무결성을 지키기 위해 이중 출금이나 잔액 왜곡은 100% 방지됩니다.',
      tips: ['대량 급여 이체 등은 단건 호출 대신 [대용량 청크 배치 이체] 기능을 사용하는 것을 권장합니다.'],
      relatedLink: {
        label: '수신 계좌 관리 바로가기',
        href: '/loan/deposit',
      },
    },
  },
  {
    id: 'faq-08',
    category: 'SYSTEM',
    question: '매일 고시되는 일일 기준환율(외화 FX)은 언제, 어떻게 시스템에 자동 업데이트되나요?',
    isPopular: false,
    viewCount: 540,
    helpfulCount: 165,
    unhelpfulCount: 1,
    tags: ['환율', 'FX', '서울외국환중개', '매매기준율', '외화평가'],
    answer: {
      summary: '서울외국환중개(SMBS) 및 한국은행 OpenAPI와 실시간 연동되어 매 영업일 오전 08:40과 오후 15:40에 기준환율(USD, EUR, JPY, CNY 등 30개 주요 통화)이 자동 수신 및 검증 반영됩니다.',
      steps: [
        '[기준정보 > 환율 관리 > 일일 고시 환율]에서 당일 고시된 환율을 확인할 수 있습니다.',
        '통신 장애 등으로 자동 수신이 지연될 경우 [환율 수동 동기화(Sync Now)] 버튼으로 즉시 수신 가능합니다.',
        '필요 시 환율 승인권자가 수기 고시 환율을 등록하고 승인할 수 있습니다.',
      ],
      notice: '외화 전표 작성 시 전표 기표일자의 매매기준율이 자동 적용되며, 임의 환율 적용 시 환율 차이 사유를 기재해야 합니다.',
      tips: ['월말 결산 시점에는 월말 최종 매매기준율로 외화 자산/부채의 외화환산손익이 자동 계상됩니다.'],
      relatedLink: {
        label: '환율 조회 및 관리 바로가기',
        href: '/mart/exchange-rates',
      },
    },
  },
  {
    id: 'faq-09',
    category: 'JOURNAL_LEDGER',
    question: '총계정원장(GL)과 보조원장(SL) 간의 잔액 불일치(Reconciliation Diff)가 발견되면 어떻게 해결하나요?',
    isPopular: true,
    viewCount: 1080,
    helpfulCount: 275,
    unhelpfulCount: 6,
    tags: ['원장', 'GL', 'SL', '대사', '불일치', '재집계'],
    answer: {
      summary: '원장 간 대사 차이는 과거 일자 소급 전표 입력이나 배치 비정상 종료 시 발생할 수 있으며, N:M 정밀 대사 엔진과 잔액 재집계 기능으로 원인을 파악하고 보정할 수 있습니다.',
      steps: [
        '[데이터마트 > 대사 차이 분석(Reconciliation Diff)]으로 이동합니다.',
        '차이가 발생한 계정과목과 기간을 조회하여 [N:M 대사 엔진 실행]을 누릅니다.',
        '미매칭(MISSING_TARGET / MISSING_SOURCE) 및 금액 불일치 항목의 트랜잭션 ID를 확인합니다.',
        '원인 파악 후 [원장 잔액 재집계(Balance Re-aggregation)] 배치를 구동하여 원장 스냅샷을 최신화합니다.',
      ],
      notice: '원장 재집계 배치는 데이터 유실 없이 모든 기표 전표의 합계를 DB 푸시다운 쿼리로 재계산하여 OOM 없이 안전하게 수행됩니다.',
      tips: ['매월 결산 마감 전 필수 체크리스트 항목으로 대사 차이 0건 여부를 자동 검증합니다.'],
      relatedLink: {
        label: '대사 차이 분석 바로가기',
        href: '/mart/reconciliation-diff',
      },
    },
  },
  {
    id: 'faq-10',
    category: 'AUTH_SECURITY',
    question: '개인용 API 액세스 토큰(Personal Access Token, PAT)은 어떻게 발급받고 관리하나요?',
    isPopular: false,
    viewCount: 470,
    helpfulCount: 130,
    unhelpfulCount: 2,
    tags: ['PAT', 'API토큰', '연동', '보안', '유효기간'],
    answer: {
      summary: '외부 배치 시스템이나 파이썬/엑셀 데이터 연동을 위한 PAT는 본인의 권한 범위 내에서 유효기간(최대 90일)을 지정하여 안전하게 발급받을 수 있습니다.',
      steps: [
        '우측 상단 프로필 클릭 후 [개인 보안 설정 > API 토큰(PAT) 관리]로 이동합니다.',
        '[새 토큰 발급] 버튼을 누르고 토큰 이름, 허용 스코프(읽기전용/쓰기), 만료일을 지정합니다.',
        '발급 즉시 화면에 표시되는 토큰 문자열을 복사하여 안전한 곳에 저장합니다.',
      ],
      notice: '보안 정책상 토큰 문자열은 생성 시점에 단 한 번만 조회 가능하며 이후 다시 확인할 수 없습니다.',
      tips: ['토큰 유출 의심 시 즉시 [토큰 폐기(Revoke)] 버튼을 눌러 비활성화 처리하세요.'],
      relatedLink: {
        label: 'PAT 토큰 관리 바로가기',
        href: '/profile/pat',
      },
    },
  },
  {
    id: 'faq-11',
    category: 'CLOSING',
    question: 'IFRS 16 리스 자산(사용권자산) 감가상각 시 장부가액이 마이너스(-)가 되지 않도록 하려면 어떻게 하나요?',
    isPopular: false,
    viewCount: 510,
    helpfulCount: 142,
    unhelpfulCount: 0,
    tags: ['리스', 'IFRS16', '사용권자산', '감가상각', '장부가하한'],
    answer: {
      summary: '시스템은 IFRS 16 기준에 따라 상각액이 잔존 장부가액을 초과할 수 없도록 [비음수 장부가 하한(Non-Negative Book Value Floor)] 룰을 강제 적용하고 있습니다.',
      steps: [
        '[공정가치/자산 > 리스 자산 관리]에서 대상 리스 계약의 감가상각 스케줄을 확인합니다.',
        '감가상각 배치 실행 시 최종 잔여 장부가가 0원이 되면 자산 상태가 자동으로 [상각완료(FULLY_DEPRECIATED)]로 전이됩니다.',
        '계약 변경(Modification)이나 리스 기간 연장이 발생한 경우 [리스 재측정(Remeasurement)] 기능을 통해 장부가와 리스부채를 일괄 재산출합니다.',
      ],
      notice: '수기 감가상각 전표 작성 시에도 장부가액 초과 상각은 전표 유효성 검증 필터에 의해 사전 차단됩니다.',
      tips: ['리스료 지급액 중 원금상환분과 이자비용 배부는 유효이자율법 스케줄에 따라 자동 분리 계산됩니다.'],
      relatedLink: {
        label: '리스 자산 관리 바로가기',
        href: '/fair-value/lease',
      },
    },
  },
  {
    id: 'faq-12',
    category: 'SYSTEM',
    question: '신규 계정과목(COA) 등록 및 재무제표 맵핑 절차는 어떻게 되나요?',
    isPopular: true,
    viewCount: 920,
    helpfulCount: 230,
    unhelpfulCount: 3,
    tags: ['계정과목', 'COA', '재무상태표', '손익계산서', '마스터데이터'],
    answer: {
      summary: '계정과목 체계는 재무제표의 근간이므로 4단계 계층 구조(대분류-중분류-소분류-세분류)와 표준 BS/IS 표시 라인 맵핑을 필수로 등록해야 합니다.',
      steps: [
        '[기준정보 > 계정과목 관리(COA)] 메뉴로 이동합니다.',
        '[신규 계정과목 등록] 버튼을 클릭하고 과목 코드, 과목명, 대차 구분(차변/대변 정상잔액)을 입력합니다.',
        '재무상태표(BS) 또는 손익계산서(IS) 출력 그룹 라인을 연결합니다.',
        '승인 요청 후 기준정보 관리자 승인을 받으면 즉시 전표 작성 시 계정 조회가 가능해집니다.',
      ],
      notice: '이미 기표 이력이 존재하는 계정과목은 코드 변경 및 삭제가 불가하며, 사용 중단 시 [사용여부: N]으로 비활성화 처리해야 합니다.',
      tips: ['부서별 보조원장(SL) 관리가 필요한 계정과목은 [보조원장 관리 대상] 체크박스를 필히 활성화하세요.'],
      relatedLink: {
        label: '계정과목 관리 바로가기',
        href: '/master/account',
      },
    },
  },
];
