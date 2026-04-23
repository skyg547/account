package com.ho.account.masterdata.core.port.out;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import java.util.List;
import java.util.Optional;

/**
 * 怨꾩젙怨쇰ぉ ??μ냼濡??섍???異쒕젰 ?ы듃?낅땲??
 *
 * <p>?μ궗怨좊궇 ?꾪궎?띿쿂?먯꽌??application service媛 JPA Repository瑜?吏곸젒 ?뚮㈃ ???⑸땲??
 * ?쒕퉬?ㅻ뒗 ???ы듃留?蹂닿퀬, ?ㅼ젣 DB ?묎렐? infrastructure adapter媛 ?대떦?⑸땲??</p>
 */
public interface AccountSubjectPersistencePort {

    boolean existsByCode(String code);

    Optional<AccountSubject> findByCode(String code);

    List<AccountSubject> findAll();

    AccountSubject save(AccountSubject accountSubject);
}
