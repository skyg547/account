package com.ho.account.masterdata.core.application.service;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import com.ho.account.masterdata.core.application.port.in.BusinessPartnerUseCase;
import com.ho.account.masterdata.core.domain.policy.MasterDataValidityPolicy;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
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
     * „ê³•?’ï?? ?‰í‡‹ ?…ì¤‰??¸ë•²??
     *
     * <p>??ë¶¿??•ë’— DTO??JPA Repository??â‘¤??ˆë–. command???°“?????•ì¤ˆ ›ë¶½???
     * ¬???„ë¶¾?? ?ìŠš²ê³Œ?²ê³•??ª›?›ìˆˆ? ??…Ğ?¹ì’–?ƒï§??¸ìŠœ?????°ì’•?????ƒæ¿¡???????—«??¸ë•²??</p>
     */
    public BusinessPartner createBusinessPartner(BusinessPartnerCommand command) {
        BusinessPartner businessPartner = command.toEntity();
        if (businessPartnerPersistencePort.existsByBusinessPartnerCode(businessPartner.getBusinessPartnerCode())) {
            throw new IllegalArgumentException("??? °ëŒ???ë’— „ê³•?’ï??„ë¶¾??…ë•²?? " + businessPartner.getBusinessPartnerCode());
        }
        MasterDataValidityPolicy.applyDefaultWindow(businessPartner::getValidFrom, businessPartner::setValidFrom,
                businessPartner::getValidTo, businessPartner::setValidTo);
        return businessPartnerPersistencePort.save(businessPartner);
    }

    // ?»œ „ê³•?’ï?°ê³ ??
    @Transactional(readOnly = true)
    public List<BusinessPartner> getAllBusinessPartners() {
        return businessPartnerPersistencePort.findAll();
    }

    // ????¬’??„ê³•?’ï?ì­” °ê³ ??
    @Transactional(readOnly = true)
    public List<BusinessPartner> getActiveBusinessPartners() {
        return businessPartnerPersistencePort.findByUseYnTrue();
    }

    // „ê³•?’ï??¸ê½­ °ê³ ??(?„ë¶¾?
    @Transactional(readOnly = true)
    public Optional<BusinessPartner> getBusinessPartnerByCode(String businessPartnerCode) {
        return businessPartnerPersistencePort.findByBusinessPartnerCode(businessPartnerCode);
    }

    // „ê³•?’ï?ƒÂ??(???
    @Transactional(readOnly = true)
    public List<BusinessPartner> searchBusinessPartnersByName(String name) {
        return businessPartnerPersistencePort.findByBusinessPartnerNameContaining(name);
    }

    /**
     * „ê³•?’ï?? ??ì ™??¸ë•²??
     *
     * <p>?˜±??›ìˆˆ? row????ì ™??ë’— ??ìº??‚ÂƒìŒ???ˆë–. ?‡¨??? ???½æº?? ?Š‚?????–– ?±¶ ‚ÂƒìŒ?
     * ??„ì‘ ‚ê¾¨?SCD2 °ê¾©????¹ê½¦ ?ë’ª?³Â??ë’ª??ºê¾¨???????‰ë’¿??ˆë–.</p>
     */
    public BusinessPartner updateBusinessPartner(Long id, BusinessPartnerCommand command) {
        BusinessPartner businessPartner = businessPartnerPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("„ê³•?’ï?? – ??????ë’¿??ˆë–. ID: " + id));

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

    // „ê³•?’ï?????(??°â”??????
    public void deleteBusinessPartner(Long id) {
        BusinessPartner businessPartner = businessPartnerPersistencePort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("„ê³•?’ï?? – ??????ë’¿??ˆë–. ID: " + id));
        businessPartner.setUseYn(false);
        businessPartnerPersistencePort.save(businessPartner);
    }
}

