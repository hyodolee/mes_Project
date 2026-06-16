package com.mes.mcs.domain.transfer.dto;

import com.mes.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferSearchDto extends PageRequest {
    private String plantCd;
    private String transferStatus;
    private String transferNo;
}
