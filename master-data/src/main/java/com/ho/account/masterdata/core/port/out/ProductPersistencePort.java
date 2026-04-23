package com.ho.account.masterdata.core.port.out;

import com.ho.account.masterdata.core.domain.model.Product;
import java.util.List;
import java.util.Optional;

/**
 * ?곹뭹 留덉뒪????μ냼 異쒕젰 ?ы듃?낅땲??
 *
 * <p>?곹뭹? Banking/ERP ?대깽?멸? ?뚭퀎 猷곗쓣 ????以묒슂??遺꾨쪟 ?ㅺ? ?⑸땲?? core 怨꾩링?
 * ???ы듃留??ъ슜?댁꽌 ?곹뭹 肄붾뱶 以묐났, 議고쉶, ??μ쓣 ?섑뻾?⑸땲??</p>
 */
public interface ProductPersistencePort {

    boolean existsByProductCode(String productCode);

    Optional<Product> findById(Long id);

    Optional<Product> findByProductCode(String productCode);

    List<Product> findAll();

    Product save(Product product);
}
