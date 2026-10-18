package com.xhzb.nursing.domain.vo.checkin;

import com.xhzb.nursing.domain.Contract;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 入住详情VO
 * 由老人信息、家属信息、入住配置与签约合同四部分组成
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Data
@Schema(description = "入住详情")
public class CheckInDetailVo {

    /** 老人信息 */
    @Schema(title = "老人信息")
    private CheckInElderVo checkInElderVo;

    /** 家属信息列表 */
    @Schema(title = "家属信息列表")
    private List<ElderFamilyVo> elderFamilyVoList;

    /** 入住配置 */
    @Schema(title = "入住配置")
    private CheckInConfigVo checkInConfigVo;

    /** 签约合同 */
    @Schema(title = "签约合同")
    private Contract contract;
}
