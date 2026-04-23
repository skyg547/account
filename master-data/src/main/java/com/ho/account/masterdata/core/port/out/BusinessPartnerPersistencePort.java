package com.ho.account.masterdata.core.port.out;

import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import java.util.List;
import java.util.Optional;

/**
 * 嫄곕옒泥???μ냼 異쒕젰 ?ы듃?낅땲??
 *
 * <p>嫄곕옒泥섎뒗 AP/AR/?먭툑/????먯쿇?먯꽌 紐⑤몢 李몄“?섎뒗 湲곗??뺣낫?낅땲?? 洹몃옒??application
 * 怨꾩링? "?대뵒????λ릺?붿?"蹂대떎 "?대뼡 議고쉶? ??μ씠 ?꾩슂?쒖?"留??뺤쓽?⑸땲??</p>
 */
public interface BusinessPartnerPersistencePort {

    boolean existsByBusinessPartnerCode(String businessPartnerCode);

    Optional<BusinessPartner> findByBusinessPartnerCode(String businessPartnerCode);

    Optional<BusinessPartner> findById(Long id);

    List<BusinessPartner> findAll();

    List<BusinessPartner> findByUseYnTrue();

    List<BusinessPartner> findByBusinessPartnerNameContaining(String name);

    BusinessPartner save(BusinessPartner businessPartner);
}
