package com.xhzb.nursing.domain.dto.checkin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 入住配置信息（床位、护理等级与费用）
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Data
@Schema(description = "入住配置信息")
public class CheckInConfigDto {

    /** 床位ID */
    @Schema(title = "床位ID")
    private Long bedId;

    /** 床位编号 */
    @Schema(title = "床位编号")
    private String bedNumber;

    /** 房间ID */
    @Schema(title = "房间ID")
    private String roomId;

    /** 房间编号 */
    @Schema(title = "房间编号")
    private String code;

    /** 楼层ID */
    @Schema(title = "楼层ID")
    private String floorId;

    /** 楼层名称 */
    @Schema(title = "楼层名称")
    private String floorName;

    /** 护理等级ID */
    @Schema(title = "护理等级ID")
    private Long nursingLevelId;

    /** 护理等级名称 */
    @Schema(title = "护理等级名称")
    private String nursingLevelName;

    /** 入住开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "入住开始日期")
    private LocalDateTime startDate;

    /** 入住结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "入住结束日期")
    private LocalDateTime endDate;

    /** 费用开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "费用开始日期")
    private LocalDateTime feeStartDate;

    /** 费用结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "费用结束日期")
    private LocalDateTime feeEndDate;

    /** 押金（元） */
    @Schema(title = "押金（元）")
    private BigDecimal deposit;

    /** 护理费用（元/月） */
    @Schema(title = "护理费用（元/月）")
    private BigDecimal nursingFee;

    /** 床位费用（元/月） */
    @Schema(title = "床位费用（元/月）")
    private BigDecimal bedFee;

    /** 医保支付（元/月） */
    @Schema(title = "医保支付（元/月）")
    private BigDecimal insurancePayment;

    /** 政府补贴（元/月） */
    @Schema(title = "政府补贴（元/月）")
    private BigDecimal governmentSubsidy;

    /** 其他费用（元/月） */
    @Schema(title = "其他费用（元/月）")
    private BigDecimal otherFees;
}
