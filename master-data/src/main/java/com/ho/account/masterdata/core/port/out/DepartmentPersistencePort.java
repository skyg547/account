package com.ho.account.masterdata.core.port.out;

import com.ho.account.masterdata.core.domain.model.Department;
import java.util.List;
import java.util.Optional;

/**
 * 遺????μ냼 異쒕젰 ?ы듃?낅땲??
 *
 * <p>遺??留덉뒪?곕뒗 鍮꾩슜?쇳꽣/?먯씡?쇳꽣 怨꾩링???쒗쁽?⑸땲?? ?좎뒪耳?댁뒪?????ы듃瑜??듯빐
 * ?곸쐞 遺???곌껐怨??좏슚湲곌컙 醫낅즺瑜?泥섎━?섍퀬, JPA ?몃??ы빆?먮뒗 ?섏〈?섏? ?딆뒿?덈떎.</p>
 */
public interface DepartmentPersistencePort {

    boolean existsByCode(String code);

    Optional<Department> findByCode(String code);

    List<Department> findAll();

    Department save(Department department);
}
