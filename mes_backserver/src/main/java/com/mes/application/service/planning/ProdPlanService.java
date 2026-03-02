package com.mes.application.service.planning;

import com.mes.domain.planning.prodplan.dto.ProdPlanCreateRequest;
import com.mes.domain.planning.prodplan.dto.ProdPlanDto;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.infra.persistence.mybatis.mapper.planning.ProdPlanMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProdPlanService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final ProdPlanMapper prodPlanMapper;

    public ProdPlanService(ProdPlanMapper prodPlanMapper) {
        this.prodPlanMapper = prodPlanMapper;
    }

    public List<ProdPlanDto> getProdPlans(String plantCd, String itemCd, String planStatus, LocalDate planFromDt,
            LocalDate planToDt) {
        return prodPlanMapper.selectProdPlans(plantCd, itemCd, planStatus, planFromDt, planToDt);
    }

    public ProdPlanDto getProdPlan(Long planId) {
        ProdPlanDto prodPlan = prodPlanMapper.selectProdPlanById(planId);
        if (prodPlan == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "생산계획을 찾을 수 없습니다. planId=" + planId);
        }
        return prodPlan;
    }

    public void createProdPlan(ProdPlanCreateRequest request) {
        int inserted = prodPlanMapper.insertProdPlan(request, SYSTEM_USER);
        if (inserted != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "생산계획 등록에 실패했습니다.");
        }
    }
}
