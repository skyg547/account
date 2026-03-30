package com.ho.account.common.service;

import com.ho.account.asset.repository.FixedAssetRepository;
import com.ho.account.asset.repository.LeaseContractRepository;
import com.ho.account.expenditure.domain.PurchaseInvoiceId; // Added
import com.ho.account.expenditure.repository.PurchaseInvoiceRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository; // Added
import com.ho.account.income.repository.SalesInvoiceRepository;
import com.ho.account.loan.repository.LoanContractRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 전표의 원천 문서(Source Document)를 조회하는 서비스.
 * JournalEntry의 lineageSourceType과 lineageSourceId를 사용하여 해당 원천 문서를 찾아 반환합니다.
 * 이는 '보고서 -> 전표 -> 원천' 드릴다운의 마지막 단계입니다.
 */
@Service
public class SourceDocumentService {

    private final FixedAssetRepository fixedAssetRepository;
    private final LeaseContractRepository leaseContractRepository;
    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final LoanContractRepository loanContractRepository;
    private final BusinessPartnerRepository businessPartnerRepository; // Added

    public SourceDocumentService(FixedAssetRepository fixedAssetRepository,
                                 LeaseContractRepository leaseContractRepository,
                                 PurchaseInvoiceRepository purchaseInvoiceRepository,
                                 SalesInvoiceRepository salesInvoiceRepository,
                                 LoanContractRepository loanContractRepository,
                                 BusinessPartnerRepository businessPartnerRepository) { // Added
        this.fixedAssetRepository = fixedAssetRepository;
        this.leaseContractRepository = leaseContractRepository;
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.loanContractRepository = loanContractRepository;
        this.businessPartnerRepository = businessPartnerRepository; // Added
    }

    /**
     * 원천 시스템 타입과 ID를 사용하여 해당 원천 문서를 조회합니다.
     * 반환 타입은 Map<String, Object>로, 실제 구현에서는 각 도메인 엔티티의 DTO 또는 상세 정보가 될 수 있습니다.
     * 여기서는 간소화를 위해 Map을 사용하며, 실제 엔티티를 반환할 수도 있습니다.
     *
     * @param lineageSourceType 원천 시스템 타입 (예: "FIXED_ASSET", "IFRS16_LEASE", "P2P_AP", "O2C_AR", "LOAN")
     * @param lineageSourceId 원천 시스템의 문서 ID (P2P_AP의 경우 "invoiceNo-vendorCode" 형식)
     * @return 원천 문서의 상세 정보를 담은 Map 또는 Optional.empty()
     */
    public Optional<Map<String, Object>> getSourceDocument(String lineageSourceType, String lineageSourceId) {
        Map<String, Object> documentDetails = new HashMap<>();

        try {
            switch (lineageSourceType) {
                case "FIXED_ASSET":
                case "IFRS16_LEASE":
                case "O2C_AR":
                case "LOAN":
                    Long id = Long.valueOf(lineageSourceId); // 이 타입들은 ID가 Long이라고 가정
                    if (lineageSourceType.equals("FIXED_ASSET")) {
                        return fixedAssetRepository.findById(id).map(fa -> {
                            documentDetails.put("type", "FixedAsset");
                            documentDetails.put("data", fa);
                            return documentDetails;
                        });
                    } else if (lineageSourceType.equals("IFRS16_LEASE")) {
                        return leaseContractRepository.findById(id).map(lc -> {
                            documentDetails.put("type", "LeaseContract");
                            documentDetails.put("data", lc);
                            return documentDetails;
                        });
                    } else if (lineageSourceType.equals("O2C_AR")) {
                        return salesInvoiceRepository.findById(id).map(si -> {
                            documentDetails.put("type", "SalesInvoice");
                            documentDetails.put("data", si);
                            return documentDetails;
                        });
                    } else { // LOAN
                        return loanContractRepository.findById(id).map(lc -> {
                            documentDetails.put("type", "LoanContract");
                            documentDetails.put("data", lc);
                            return documentDetails;
                        });
                    }

                case "P2P_AP": // Purchase Invoice (복합 키)
                    String[] parts = lineageSourceId.split("-");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("Invalid lineageSourceId format for P2P_AP: " + lineageSourceId);
                    }
                    PurchaseInvoiceId purchaseInvoiceId = new PurchaseInvoiceId(parts[0], parts[1]);
                    return purchaseInvoiceRepository.findById(purchaseInvoiceId).map(pi -> {
                        documentDetails.put("type", "PurchaseInvoice");
                        documentDetails.put("data", pi);
                        return documentDetails;
                    });
                // TODO: Add more cases for other lineageSourceType values (e.g., RECONCILIATION, CLOSING, etc.)
                default:
                    return Optional.empty();
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid lineageSourceId format for type " + lineageSourceType + ": " + lineageSourceId + ". Error: " + e.getMessage());
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            System.err.println("Error processing lineageSourceId for type " + lineageSourceType + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}
