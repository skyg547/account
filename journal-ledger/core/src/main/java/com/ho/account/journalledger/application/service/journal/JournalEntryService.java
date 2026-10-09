package com.ho.account.journalledger.application.service.journal;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.JournalReversalPersistencePort;
import com.ho.account.journalledger.application.port.out.SlipNumberAllocationException;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalReversalOperation;
import com.ho.account.journalledger.domain.journal.domain.ReversalOperationStatus;
import com.ho.account.journalledger.application.service.journal.validator.JournalValidationEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 전표 애플리케이션 서비스 (Journal Entry Service).
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 전표(Journal Entry)의 생성, 조회, 승인, 전기를 담당하는 핵심 서비스입니다.
 *
 * 전표 처리 흐름:
 *   1. createJournalEntry()       → 전표 작성 (DRAFT 상태)
 *   2. requestJournalEntryApproval() → 승인 요청 (DRAFT → REQUESTED)
 *   3. approveJournalEntry()      → 전표 승인 (REQUESTED → APPROVED)
 *   4. postJournalEntry()         → 전기 처리 (APPROVED → POSTED)
 *                                    → GL/SL 원장에 잔액 반영
 *
 * 이벤트 기반 자동 전표:
 *   createJournalEntryFromEvent() → JournalRuleEngine이 이벤트 분석 후 자동 전표 생성
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * 헥사고날 아키텍처에서 Application Service 계층에 위치합니다.
 *
 * 역할:
 *   - JournalUseCase (Inbound Port) 구현체
 *   - 비즈니스 흐름 조정 (Orchestration): 도메인 객체 호출 순서 결정
 *   - 도메인 로직은 JournalEntry 도메인 엔티티에 위임 (Rich Domain Model)
 *   - 영속성 기술과의 접점은 JournalPersistencePort (Outbound Port)를 통해서만 접근
 *
 * 의존성 (모두 인터페이스, 구체 구현체에 의존하지 않음):
 *   - JournalPersistencePort → JournalPersistenceAdapter (JPA) 구현체가 주입됨
 *   - JournalRuleEngine → 자동분개 규칙 엔진
 *
 * 트랜잭션 전략:
 *   - 쓰기 메서드(@Transactional): createJournalEntry, approveJournalEntry, postJournalEntry
 *   - 읽기 메서드: 별도 @Transactional 없음 (Repository 레벨에서 처리)
 * ─────────────────────────────────────────────────
 */
@Service
public class JournalEntryService implements JournalUseCase {

    private static final long MAX_SLIP_NUMBER = 2_821_109_907_455L; // 36^8 - 1
    private static final Pattern AUTOMATIC_SLIP_NAMESPACE =
            Pattern.compile("JE-[0-9]{8}-[0-9A-Z]{8}");

    private static final JournalReversalPersistencePort UNAVAILABLE_REVERSAL_PERSISTENCE =
            new JournalReversalPersistencePort() {
                @Override
                public JournalReversalOperation save(JournalReversalOperation operation) {
                    throw unavailableReversalPersistence();
                }

                @Override
                public Optional<JournalReversalOperation> findByOriginalJournalEntryId(Long originalJournalEntryId) {
                    throw unavailableReversalPersistence();
                }

                @Override
                public Optional<JournalReversalOperation> findByReversalJournalEntryId(Long reversalJournalEntryId) {
                    throw unavailableReversalPersistence();
                }
            };

    /**
     * 전표 영속성 포트 (Outbound Port).
     * 실제 DB 접근은 이 포트의 구현체(JournalPersistenceAdapter)가 담당합니다.
     * 이 서비스는 "저장해줘", "찾아줘"라는 의도만 표현하고, 기술 세부사항은 모릅니다.
     */
    private final JournalPersistencePort journalPersistencePort;

    /** 원본별 단일 역분개 작업과 그 수명주기를 저장합니다. */
    private final JournalReversalPersistencePort journalReversalPersistencePort;

    /**
     * 자동분개 규칙 엔진.
     * 이벤트 데이터를 분석해 자동으로 전표를 생성하는 로직을 담당합니다.
     * 예: "매입 이벤트" → 매입채무 차변/대변 자동 분개
     */
    private final JournalRuleEngine journalRuleEngine;

    /**
     * 전기 서비스 (Posting Service).
     * 전표 승인 후 실제 원장(GL/SL)에 반영하는 역할을 담당합니다.
     */
    private final PostingService postingService;

    /**
     * 전표 검증 엔진.
     * 차대일치, 계정유효성, 마감잠금 등 복합 검증을 수행합니다.
     */
    private final JournalValidationEngine journalValidationEngine;

    /** Production wiring uses the durable reversal operation port. */
    @Autowired
    public JournalEntryService(
            JournalPersistencePort journalPersistencePort,
            JournalReversalPersistencePort journalReversalPersistencePort,
            JournalRuleEngine journalRuleEngine,
            PostingService postingService,
            JournalValidationEngine journalValidationEngine) {
        this.journalPersistencePort = journalPersistencePort;
        this.journalReversalPersistencePort = journalReversalPersistencePort;
        this.journalRuleEngine = journalRuleEngine;
        this.postingService = postingService;
        this.journalValidationEngine = journalValidationEngine;
    }

    /**
     * 기존 직접 생성 소비자의 source compatibility를 유지합니다.
     *
     * <p>일반 전표 기능은 유지하지만 영속 operation이 없는 역분개 생성·취소는 fail-closed합니다.</p>
     */
    public JournalEntryService(
            JournalPersistencePort journalPersistencePort,
            JournalRuleEngine journalRuleEngine,
            PostingService postingService,
            JournalValidationEngine journalValidationEngine) {
        this(journalPersistencePort, UNAVAILABLE_REVERSAL_PERSISTENCE,
                journalRuleEngine, postingService, journalValidationEngine);
    }

    private static IllegalStateException unavailableReversalPersistence() {
        return new IllegalStateException("역분개 작업 영속성이 구성되지 않아 역분개 명령을 처리할 수 없습니다.");
    }

    /**
     * 전표를 수동으로 생성합니다.
     *
     * [업무 설명]
     * 회계 담당자가 직접 작성한 전표를 저장합니다.
     * 저장 전 검증 엔진을 통해 차대일치, 계정유효성, 마감잠금 등을 확인합니다.
     *
     * @param journalEntry 저장할 전표 (Controller에서 DTO → 도메인 변환 후 전달)
     * @return 저장된 전표 (DB에서 생성된 id, slipNo 포함)
     */
    @Override
    @Transactional
    public JournalEntry createJournalEntry(JournalEntry journalEntry) {
        if (journalEntry.isReversal()) {
            // 공개 생성 포트가 operation claim 없이 REVERSAL을 만들 수 있으면 exactly-once 통제를
            // 우회합니다. 역분개는 reverseJournalEntry가 원본 잠금과 작업 저장을 함께 수행합니다.
            throw new IllegalArgumentException(
                    "역분개 전표는 reverseJournalEntry 유스케이스로만 생성할 수 있습니다.");
        }
        return validateAndSaveJournalEntry(journalEntry);
    }

    /** 검증과 채번을 공유하되 역분개 우회 권한을 외부 유스케이스에 노출하지 않습니다. */
    private JournalEntry validateAndSaveJournalEntry(JournalEntry journalEntry) {
        // 날짜를 유지하되 전역 DB sequence의 8자리 base36 값을 사용합니다.
        // 4자리 legacy suffix와 길이가 달라 과거 전표와 번호 공간이 겹치지 않습니다.
        if (journalEntry.getSlipNo() == null) {
            if (journalEntry.getSlipDate() == null) {
                // 작성일 누락 시 도메인 불변성 검증으로 빠른 예외 발생
                journalEntry.validateInvariants();
            }
            LocalDate slipDate = journalEntry.getSlipDate();
            if (slipDate.getYear() < 1 || slipDate.getYear() > 9999) {
                throw new IllegalArgumentException("전표 작성일은 0001~9999년이어야 합니다.");
            }
            String datePart = slipDate.format(DateTimeFormatter.BASIC_ISO_DATE);
            long number = journalPersistencePort.nextSlipNumber();
            if (number < 1 || number > MAX_SLIP_NUMBER) {
                throw new SlipNumberAllocationException("전표번호 채번 범위를 소진했습니다.");
            }
            String suffix = Long.toString(number, 36).toUpperCase(java.util.Locale.ROOT);
            journalEntry.setSlipNo("JE-" + datePart + "-" + "0".repeat(8 - suffix.length()) + suffix);
        } else if (AUTOMATIC_SLIP_NAMESPACE.matcher(journalEntry.getSlipNo()).matches()) {
            // Reserving this format prevents caller-supplied numbers from occupying a future
            // sequence value. Existing four-character and custom slip numbers remain valid.
            throw new IllegalArgumentException("자동 전표번호 형식은 직접 지정할 수 없습니다.");
        }

        // ─────────────────────────────────────────────────
        // [DDD 응집도 및 검증 파이프라인]
        // Application Service는 도메인 검증 로직을 직접 수행하지 않고,
        // journalValidationEngine을 통해 Aggregate Root의 validateInvariants() 및
        // 마감/계정유효성 등 외부 도메인 서브시스템 검증 필터들을 통합 호출합니다.
        // ─────────────────────────────────────────────────
        journalValidationEngine.validate(journalEntry);

        return journalPersistencePort.save(journalEntry);
    }

    /**
     * 이벤트 데이터로부터 자동 전표를 생성합니다.
     *
     * [업무 설명]
     * 매입, 지출결의, 리스 납부 등 외부 이벤트가 발생하면 자동으로 전표를 생성합니다.
     * 담당자가 직접 전표를 입력하지 않아도 되므로, 수작업 오류를 줄이고 자동화가 가능합니다.
     *
     * [개발 설명]
     * JournalRuleEngine에 이벤트 데이터를 넘기면, 등록된 규칙(JournalRule)을 조회해
     * 적합한 전표를 자동 생성합니다.
     * 규칙이 없거나 매칭되지 않으면 Optional.empty()를 반환합니다.
     * 이 경우 상위 호출자(Contract Adapter 등)가 예외 처리 또는 수동 처리를 결정합니다.
     *
     * @param eventData      이벤트 데이터 (예: { "eventType": "PURCHASE", "amount": 100000 })
     * @param accountingDate 회계 반영일
     * @return 생성된 전표 Optional (규칙 미매칭 시 empty)
     */
    @Override
    @Transactional
    public Optional<JournalEntry> createJournalEntryFromEvent(Map<String, Object> eventData, LocalDate accountingDate) {
        return journalRuleEngine.generateJournalEntry(eventData, accountingDate)
                .map(this::createJournalEntry);
    }

    /**
     * 특정 기간의 전표 목록을 조회합니다.
     *
     * [업무 설명]
     * 월별 마감, 원장 검토, 감사 대응 시 특정 기간의 전표 전체를 조회합니다.
     *
     * @param startDate 조회 시작일 (포함)
     * @param endDate   조회 종료일 (포함)
     * @return 해당 기간 전표 목록
     */
    @Override
    public List<JournalEntry> getJournalEntriesByDate(LocalDate startDate, LocalDate endDate) {
        return journalPersistencePort.findByAccountingDateBetween(startDate, endDate);
    }

    @Override
    public List<JournalEntry> getJournalEntriesBySource(String sourceType, String sourceId) {
        return journalPersistencePort.findBySource(sourceType, sourceId);
    }

    /**
     * 전표번호로 전표를 조회합니다.
     *
     * [업무 설명]
     * "JE-20260101-001" 같은 전표번호로 특정 전표를 조회합니다.
     * 거래 명세서나 감사 요청 시 특정 전표를 검색할 때 사용합니다.
     *
     * @param slipNo 전표번호
     * @return 해당 전표 (없으면 Optional.empty())
     */
    @Override
    public Optional<JournalEntry> getJournalEntryBySlipNo(String slipNo) {
        return journalPersistencePort.findBySlipNo(slipNo);
    }

    /**
     * 전표를 상세 라인까지 함께 조회합니다.
     *
     * [업무 설명]
     * 전표 상세 화면에서 차변/대변 각 라인(계정과목, 금액, 적요)을 보여줄 때 사용합니다.
     *
     * [개발 설명]
     * findByIdWithDetails()는 짧은 트랜잭션에서 헤더를 잠그고 최신 헤더/상세를 읽습니다.
     * 명시적인 읽기 전용 트랜잭션에서는 잠금 없이 JOIN FETCH합니다.
     * 일반 findById()는 헤더만 가져오므로 상세 라인 접근 시 LazyInitializationException이 발생합니다.
     *
     * @param id 전표 내부 PK
     * @return 상세 라인 포함 전표 (없으면 Optional.empty())
     */
    @Override
    public Optional<JournalEntry> getJournalEntryWithDetails(Long id) {
        return journalPersistencePort.findByIdWithDetails(id);
    }

    @Override
    public Optional<JournalEntry> getJournalEntry(Long id) {
        return journalPersistencePort.findById(id);
    }

    /** 작성자가 자신의 DRAFT 전표를 REQUESTED로 제출합니다. */
    @Override
    @Transactional
    public void requestJournalEntryApproval(Long id, String requester) {
        // 승인 수명주기도 취소/전기와 같은 전표 행을 먼저 잠가, 대기 후 최신 상태로 전이합니다.
        JournalEntry entry = journalPersistencePort.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 전표입니다: " + id));
        entry.requestApproval(requester);
        journalPersistencePort.save(entry);
    }

    /** 별도 결재자가 REQUESTED 전표를 APPROVED로 전환합니다. */
    @Override
    @Transactional
    public void approveJournalEntry(Long id, String approver) {
        // 취소/전기와 동일한 전표 행 잠금 순서를 사용해 stale REQUESTED 승인을 차단합니다.
        JournalEntry entry = journalPersistencePort.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 전표입니다: " + id));

        // 도메인 엔티티에 상태 전환 위임 (Rich Domain Model)
        //    approve() 내부에서 REQUESTED 상태와 maker-checker 분리를 검증합니다.
        entry.approve(approver);

        // 변경된 상태를 저장
        journalPersistencePort.save(entry);
    }

    /**
     * 전표를 전기(Post)합니다.
     *
     * [업무 설명]
     * 전기는 회계상 가장 중요한 단계입니다.
     * 전기가 완료되어야 총계정원장(GL)과 보조원장(SL)에 잔액이 반영되고,
     * 재무제표(재무상태표, 손익계산서)에 해당 거래가 포함됩니다.
     *
     * 전기 후 다음 처리가 연이어 발생합니다 (PostingService가 담당):
     *   1. GL Entry 생성 → GL Balance 잔액 업데이트
     *   2. SL Entry 생성 → SL Balance 잔액 업데이트
     *   3. 미결 항목(채권·채무) 생성 (UnsettledItem)
     *
     * [개발 설명]
     * 도메인 엔티티의 post(poster) 메서드 호출로 상태를 POSTED로 변경합니다.
     * APPROVED 상태인 전표만 전기 가능합니다 (도메인 엔티티 내부 검증).
     * 전기 후 원장 갱신은 PostingService가 별도로 처리합니다.
     *
     * @param id     전기할 전표의 내부 PK
     * @param poster 전기 처리자 식별자
     * @throws IllegalArgumentException 존재하지 않는 전표 ID
     */
    @Override
    @Transactional
    public void postJournalEntry(Long id, String poster) {
        // 상태 전이와 GL/SL 원장 반영은 PostingService에서 하나의 트랜잭션으로 처리합니다.
        postingService.postJournalEntry(id, poster);
    }

    @Override
    @Transactional
    public JournalEntry reverseJournalEntry(Long id, LocalDate accountingDate, String creator, String reason) {
        // 원본 행이 같은 원본에 대한 모든 생성 요청의 단일 잠금 자원입니다.
        JournalEntry original = journalPersistencePort.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 전표입니다: " + id));
        original.assertPostedForReversal();

        Optional<JournalReversalOperation> existingOperation =
                journalReversalPersistencePort.findByOriginalJournalEntryId(id);
        if (existingOperation.isPresent()
                && existingOperation.get().getStatus() != ReversalOperationStatus.CANCELLED) {
            // 재시도의 날짜/사유가 달라도 최초 요청이 만든 현재 전표를 그대로 반환합니다.
            // 역분개 행을 추가 잠그지 않아 posting/cancel 경로와 잠금 순서가 뒤집히지 않습니다.
            return findExistingReversal(existingOperation.get());
        }

        JournalEntry savedReversal = validateAndSaveJournalEntry(
                original.createReversal(creator, accountingDate, reason));
        if (savedReversal.getId() == null) {
            throw new IllegalStateException("저장된 역분개 전표 ID가 없습니다.");
        }

        JournalReversalOperation operation = existingOperation.orElseGet(
                () -> JournalReversalOperation.create(id, savedReversal.getId()));
        if (existingOperation.isPresent()) {
            operation.restart(savedReversal.getId());
        }
        journalReversalPersistencePort.save(operation);
        return savedReversal;
    }

    /**
     * 현재 PENDING 역분개를 전표 행 잠금 아래 취소합니다.
     *
     * <p>operation 선조회는 잠글 역분개 ID와 명백한 terminal 상태만 확인합니다. 실제 경쟁은
     * 역분개 전표 행 잠금으로 직렬화되며, 먼저 전기/취소한 요청의 최신 전표 상태를 뒤 요청이
     * 다시 읽으므로 stale operation을 변경하기 전에 도메인 상태 검증이 실패합니다.</p>
     */
    @Override
    @Transactional
    public JournalEntry cancelReversal(Long originalJournalEntryId, String actor, String reason) {
        JournalReversalOperation operation = journalReversalPersistencePort
                .findByOriginalJournalEntryId(originalJournalEntryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "역분개 작업이 존재하지 않습니다: " + originalJournalEntryId));

        if (operation.getStatus() == ReversalOperationStatus.POSTED) {
            throw new IllegalStateException("전기 완료된 역분개 작업은 취소할 수 없습니다.");
        }
        if (operation.getStatus() == ReversalOperationStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 역분개 작업입니다.");
        }

        Long reversalJournalEntryId = operation.getReversalJournalEntryId();
        JournalEntry reversal = journalPersistencePort.findByIdWithDetails(reversalJournalEntryId)
                .orElseThrow(() -> new IllegalStateException(
                        "역분개 작업에 연결된 전표가 없습니다: " + reversalJournalEntryId));

        reversal.cancelReversal(actor, reason);
        operation.cancel(reversalJournalEntryId, actor, reason);
        journalPersistencePort.save(reversal);
        journalReversalPersistencePort.save(operation);
        return reversal;
    }

    private JournalEntry findExistingReversal(JournalReversalOperation operation) {
        Long reversalJournalEntryId = operation.getReversalJournalEntryId();
        return journalPersistencePort.findByIdWithDetailsWithoutLock(reversalJournalEntryId)
                .orElseThrow(() -> new IllegalStateException(
                        "역분개 작업에 연결된 전표가 없습니다: " + reversalJournalEntryId));
    }
}
