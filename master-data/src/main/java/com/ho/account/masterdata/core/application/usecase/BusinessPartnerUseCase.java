package com.ho.account.masterdata.core.application.usecase;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.application.command.BusinessPartnerCommand;
import java.util.List;
import java.util.Optional;

/**
 * 嫄곕옒泥?留덉뒪???낅젰 ?ы듃?낅땲??
 *
 * <p>Controller 媛숈? inbound adapter?????명꽣?섏씠?ㅻ쭔 ?몄텧?⑸땲?? ?대젃寃??먮㈃ REST媛 ?꾨땶
 * Kafka, batch, CLI媛 ?ㅼ뼱?ㅻ뜑?쇰룄 媛숈? ?좎뒪耳?댁뒪 洹쒖튃???ъ궗?⑺븷 ???덉뒿?덈떎.</p>
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
