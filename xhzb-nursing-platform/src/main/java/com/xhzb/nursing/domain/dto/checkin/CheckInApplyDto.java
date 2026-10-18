package com.xhzb.nursing.domain.dto.checkin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 入住申请参数
 * 一次入住办理需要提交：健康评估ID、老人基本信息、家属信息、入住配置与签约合同
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Data
@Schema(description = "入住申请参数")
public class CheckInApplyDto {

    /** 健康评估ID */
    @NotNull(message = "健康评估ID不能为空")
    @Schema(title = "健康评估ID")
    private Long healthAssessmentId;

    /** 老人基本信息 */
    @NotNull(message = "老人基本信息不能为空")
    @Schema(title = "老人基本信息")
    private CheckInElderDto checkInElderDto;

    /** 家属信息列表（保存到入住记录的备注字段中） */
    @Schema(title = "家属信息列表")
    private List<ElderFamilyDto> elderFamilyDtoList;

    /** 入住配置 */
    @NotNull(message = "入住配置不能为空")
    @Schema(title = "入住配置")
    private CheckInConfigDto checkInConfigDto;

    /** 签约合同 */
    @NotNull(message = "签约合同不能为空")
    @Schema(title = "签约合同")
    private CheckInContractDto checkInContractDto;
}
