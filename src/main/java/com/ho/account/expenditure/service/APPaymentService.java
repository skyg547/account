package com.ho.account.expenditure.service;

import com.ho.account.expenditure.domain.APPayment;
import com.ho.account.expenditure.domain.ExpenditureResolution;
import com.ho.account.expenditure.dto.APPaymentRequestDto;
import com.ho.account.expenditure.repository.APPaymentRepository;
import com.ho.account.expenditure.repository.ExpenditureResolutionRepository;
import com.ho.account.tax.domain.TaxInvoice;
import com.ho.account.tax.repository.TaxInvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class APPaymentService {

    private final APPaymentRepository apPaymentRepository;
    private final ExpenditureResolutionRepository expenditureResolutionRepository;
    private final TaxInvoiceRepository taxInvoiceRepository;

    @Autowired
    public APPaymentService(APPaymentRepository apPaymentRepository,
                            ExpenditureResolutionRepository expenditureResolutionRepository,
                            TaxInvoiceRepository taxInvoiceRepository) {
        this.apPaymentRepository = apPaymentRepository;
        this.expenditureResolutionRepository = expenditureResolutionRepository;
        this.taxInvoiceRepository = taxInvoiceRepository;
    }

    /**
     * 새로운 매입 지급 (AP Payment)을 생성합니다.
     * 지급은 지출결의와 연결됩니다.
     *
     * @param requestDto 생성할 AP Payment 정보가 담긴 DTO
     * @return 생성된 APPayment 엔티티
     */
    public APPayment createAPPayment(APPaymentRequestDto requestDto) {
        ExpenditureResolution expenditureResolution = expenditureResolutionRepository.findById(requestDto.getExpenditureResolutionId())
                .orElseThrow(() -> new IllegalArgumentException("지출결의를 찾을 수 없습니다. ID: " + requestDto.getExpenditureResolutionId()));

        TaxInvoice taxInvoice = null;
        if (requestDto.getTaxInvoiceId() != null) {
            taxInvoice = taxInvoiceRepository.findById(requestDto.getTaxInvoiceId())
                    .orElseThrow(() -> new IllegalArgumentException("세금계산서를 찾을 수 없습니다. ID: " + requestDto.getTaxInvoiceId()));
        }

        APPayment apPayment = new APPayment();
        apPayment.setExpenditureResolution(expenditureResolution);
        apPayment.setTaxInvoice(taxInvoice); // Set nullable taxInvoice
        apPayment.setPaymentDate(requestDto.getPaymentDate());
        apPayment.setAmount(requestDto.getAmount());
        apPayment.setUnappliedAmount(requestDto.getAmount()); // 초기에는 전체 금액이 미적용 상태
        apPayment.setPaymentMethod(requestDto.getPaymentMethod());
        apPayment.setStatus("PENDING"); // 초기 상태

        // TODO: 지급이 발생하면 ExpenditureResolution의 상태를 업데이트하거나 JournalEntry를 생성하는 로직 추가
        // expenditureResolution.updateStatusBasedOnPayment();

        return apPaymentRepository.save(apPayment);
    }

    /**
     * ID로 AP Payment를 조회합니다.
     *
     * @param id 조회할 AP Payment ID
     * @return Optional<APPayment>
     */
    @Transactional(readOnly = true)
    public Optional<APPayment> getAPPaymentById(Long id) {
        return apPaymentRepository.findById(id);
    }

    /**
     * 특정 지출결의와 관련된 모든 AP Payment를 조회합니다.
     *
     * @param expenditureResolutionId 지출결의 ID
     * @return AP Payment 리스트
     */
    @Transactional(readOnly = true)
    public List<APPayment> getAPPaymentsByExpenditureResolution(Long expenditureResolutionId) {
        // Assuming we might need a custom method in APPaymentRepository for this,
        // or filter from findAll if performance is not an issue for small datasets.
        // For now, let's assume direct access if no custom method is added.
        return apPaymentRepository.findAll().stream()
                .filter(p -> p.getExpenditureResolution().getId().equals(expenditureResolutionId))
                .toList();
    }

    /**
     * AP Payment 정보를 수정합니다.
     * (여기서는 상태 및 미적용 금액 변경에 초점)
     *
     * @param id         수정할 AP Payment ID
     * @param requestDto 수정할 내용이 담긴 DTO
     * @return 수정된 APPayment 엔티티
     */
    public APPayment updateAPPayment(Long id, APPaymentRequestDto requestDto) {
        APPayment existingPayment = apPaymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AP Payment를 찾을 수 없습니다. ID: " + id));

        ExpenditureResolution expenditureResolution = expenditureResolutionRepository.findById(requestDto.getExpenditureResolutionId())
                .orElseThrow(() -> new IllegalArgumentException("지출결의를 찾을 수 없습니다. ID: " + requestDto.getExpenditureResolutionId()));

        TaxInvoice taxInvoice = null;
        if (requestDto.getTaxInvoiceId() != null) {
            taxInvoice = taxInvoiceRepository.findById(requestDto.getTaxInvoiceId())
                    .orElseThrow(() -> new IllegalArgumentException("세금계산서를 찾을 수 없습니다. ID: " + requestDto.getTaxInvoiceId()));
        }

        existingPayment.setExpenditureResolution(expenditureResolution);
        existingPayment.setTaxInvoice(taxInvoice);
        existingPayment.setPaymentDate(requestDto.getPaymentDate());
        existingPayment.setAmount(requestDto.getAmount());
        // For update, unappliedAmount logic might be more complex if payments are being applied/unapplied
        // For simplicity, we re-set it to the full amount if the amount changes, requiring re-application logic elsewhere.
        existingPayment.setUnappliedAmount(requestDto.getAmount());
        existingPayment.setPaymentMethod(requestDto.getPaymentMethod());
        // existingPayment.setStatus(requestDto.getStatus()); // Status update should be a separate method for workflow control

        return apPaymentRepository.save(existingPayment);
    }

    /**
     * AP Payment의 상태를 업데이트합니다.
     *
     * @param id     AP Payment ID
     * @param status 새로운 상태
     * @return 업데이트된 APPayment 엔티티
     */
    public APPayment updateAPPaymentStatus(Long id, String status) {
        APPayment existingPayment = apPaymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AP Payment를 찾을 수 없습니다. ID: " + id));
        existingPayment.setStatus(status);
        return apPaymentRepository.save(existingPayment);
    }

    /**
     * AP Payment를 삭제합니다 (실제 삭제).
     *
     * @param id 삭제할 AP Payment ID
     */
    public void deleteAPPayment(Long id) {
        APPayment existingPayment = apPaymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AP Payment를 찾을 수 없습니다. ID: " + id));
        apPaymentRepository.delete(existingPayment);
    }
}
