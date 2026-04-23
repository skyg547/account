package com.ho.account.masterdata.core.application.port.in;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import java.util.List;
import java.util.Optional;

/**
 * °ê¾§???—?Œë¾???????†ì ¾ ??????…ë¹??
 *
 * <p>Controller ?¶ì†?? inbound adapter?????ë‚ƒ???ëµ ???´ì¶¸ ?ê¾ª???ƒë¹?? ????ƒì¹°??????REST?¶ì›? ?â‘¤? * Kafka, batch, CLI?¶ì›? ???°ì„ ???³ì­??•ì ?¶ì†?? ????ˆÂ??·ë® ?Ÿë????????ë§‰ ?????°ë????ˆë¼„.</p>
 */
public interface BusinessPartnerUseCase {

    BusinessPartner createBusinessPartner(BusinessPartnerCommand command);

    List<BusinessPartner> getAllBusinessPartners();

    List<BusinessPartner> getActiveBusinessPartners();

    Optional<BusinessPartner> getBusinessPartnerByCode(String businessPartnerCode);

    List<BusinessPartner> searchBusinessPartnersByName(String name);

    BusinessPartner updateBusinessPartner(Long id, BusinessPartnerCommand command);

    void deleteBusinessPartner(Long id);
}

