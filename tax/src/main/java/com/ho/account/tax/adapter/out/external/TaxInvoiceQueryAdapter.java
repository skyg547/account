package com.ho.account.tax.adapter.out.external;

import com.ho.account.contracts.tax.TaxInvoiceQueryPort;
import com.ho.account.contracts.tax.TaxInvoiceRef;
import com.ho.account.tax.repository.TaxInvoiceRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TaxInvoiceQueryAdapter implements TaxInvoiceQueryPort {

    private final TaxInvoiceRepository taxInvoiceRepository;

    public TaxInvoiceQueryAdapter(TaxInvoiceRepository taxInvoiceRepository) {
        this.taxInvoiceRepository = taxInvoiceRepository;
    }

    @Override
    public Optional<TaxInvoiceRef> findById(Long taxInvoiceId) {
        // @todo 논리 취소된 세금계산서를 외부 모듈 참조에서 제외할지 정책을 확정하고 TaxInvoiceRef에 상태를 포함한다.
        return taxInvoiceRepository.findById(taxInvoiceId)
                .map(taxInvoice -> new TaxInvoiceRef(
                        taxInvoice.getId(),
                        taxInvoice.getIssueId(),
                        taxInvoice.getType()));
    }
}
