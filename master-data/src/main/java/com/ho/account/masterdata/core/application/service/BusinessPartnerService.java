package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.usecase.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import com.ho.account.masterdata.core.port.out.BusinessPartnerPersistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BusinessPartnerService implements BusinessPartnerUseCase {

    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;

    public BusinessPartnerService(BusinessPartnerPersistencePort businessPartnerPersistencePort) {
        this.businessPartnerPersistencePort = businessPartnerPersistencePort;
    }

    /**
     * 嫄곕옒泥섎? ?좉퇋 ?깅줉?⑸땲??
     *
     * <p>??硫붿꽌?쒕뒗 DTO??JPA Repository瑜?紐⑤쫭?덈떎. command瑜??꾨찓???뷀떚?곕줈 諛붽씀怨?
     * 以묐났 肄붾뱶? ?좏슚湲곌컙 湲곕낯媛?媛숈? ?낅Т 洹쒖튃留??곸슜????異쒕젰 ?ы듃濡???μ쓣 ?꾩엫?⑸땲??</p>
     */
    public BusinessPartner createBusinessPartner(BusinessPartnerCommand command) {
        BusinessPartner businessPartner = command.toEntity();
        if (businessPartnerPersistencePort.existsByBusinessPartnerCode(businessPartner.getBusinessPartnerCode())) {
            throw new IllegalArgumentException("?대? 議댁옱?섎뒗 嫄곕옒泥?肄붾뱶?낅땲?? " + businessPartner.getBusinessPartnerCode());
        }
        MasterDataValidityPolicy.applyDefaultWindow(businessPartner::getValidFrom, businessPartner::setValidFrom,
                businessPartner::getValidTo, businessPartner::setValidTo);
        return businessPartnerPersistencePort.save(businessPartner);
    }

    // ?꾩껜 嫄곕옒泥?議고쉶
    @Transactional(readOnly = true)
    public List<BusinessPartner> getAllBusinessPartners() {
        return businessPartnerPersistencePort.findAll();
    }

    // ?ъ슜 以묒씤 嫄곕옒泥섎쭔 議고쉶
    @Transactional(readOnly = true)
    public List<BusinessPartner> getActiveBusinessPartners() {
        return businessPartnerPersistencePort.findByUseYnTrue();
    }

    // 嫄곕옒泥??곸꽭 議고쉶 (肄붾뱶)
    @Transactional(readOnly = true)
    public Optional<BusinessPartner> getBusinessPartnerByCode(String businessPartnerCode) {
        return businessPartnerPersistencePort.findByBusinessPartnerCode(businessPartnerCode);
    }

    // 嫄곕옒泥?寃??(?대쫫)
    @Transactional(readOnly = true)
    public List<BusinessPartner> searchBusinessPartnersByName(String name) {
        return businessPartnerPersistencePort.findByBusinessPartnerNameContaining(name);
    }

    /**
     * 嫄곕옒泥섎? ?섏젙?⑸땲??
     *
     * <p>?꾩옱??媛숈? row瑜??섏젙?섎뒗 ?댁쁺??蹂寃쎌엯?덈떎. 怨쇨굅 ?λ? ?ы쁽源뚯? ?꾩슂???듭떖 ?꾨뱶 蹂寃쎌?
     * ?댄썑 蹂꾨룄 SCD2 踰꾩쟾 ?앹꽦 ?좎뒪耳?댁뒪濡?遺꾨━?????덉뒿?덈떎.</p>
     */
    public BusinessPartner updateBusinessPartner(Long id, BusinessPartnerCommand command) {
        BusinessPartner businessPartner = businessPartnerPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("嫄곕옒泥섎? 李얠쓣 ???놁뒿?덈떎. ID: " + id));

        businessPartner.setBusinessPartnerName(command.businessPartnerName());
        businessPartner.setRegistrationNumber(command.registrationNumber());
        businessPartner.setCeoName(command.ceoName());
        businessPartner.setBusinessType(command.businessType());
        businessPartner.setBusinessItem(command.businessItem());
        if (command.partnerType() != null) {
            businessPartner.setPartnerType(command.partnerType());
        }
        if (command.useYn() != null) {
            businessPartner.setUseYn(command.useYn());
        }
        if (command.kycStatus() != null) {
            businessPartner.setKycStatus(command.kycStatus());
        }
        if (command.riskRating() != null) {
            businessPartner.setRiskRating(command.riskRating());
        }
        businessPartner.setValidFrom(command.validFrom());
        businessPartner.setValidTo(command.validTo());
        MasterDataValidityPolicy.applyDefaultWindow(businessPartner::getValidFrom, businessPartner::setValidFrom,
                businessPartner::getValidTo, businessPartner::setValidTo);

        return businessPartnerPersistencePort.save(businessPartner);
    }

    // 嫄곕옒泥???젣 (?쇰━????젣)
    public void deleteBusinessPartner(Long id) {
        BusinessPartner businessPartner = businessPartnerPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("嫄곕옒泥섎? 李얠쓣 ???놁뒿?덈떎. ID: " + id));
        businessPartner.setUseYn(false);
        businessPartnerPersistencePort.save(businessPartner);
    }
}
