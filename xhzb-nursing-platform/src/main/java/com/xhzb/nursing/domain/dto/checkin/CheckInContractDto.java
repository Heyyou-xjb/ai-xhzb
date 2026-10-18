package com.xhzb.nursing.domain.dto.checkin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 入住签约合同信息
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Data
@Schema(description = "入住签约合同信息")
public class CheckInContractDto {

    /** 合同名称 */
    @Schema(title = "合同名称")
    private String contractName;

    /** 签约日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "签约日期")
    private Date signDate;

    /** 丙方（家属）姓名 */
    @Schema(title = "丙方姓名")
    private String thirdPartyName;

    /** 丙方（家属）手机号 */
    @Schema(title = "丙方手机号")
    private String thirdPartyPhone;

    /** 协议地址（文件路径或URL） */
    @Schema(title = "协议地址")
    private String agreementPath;
}
