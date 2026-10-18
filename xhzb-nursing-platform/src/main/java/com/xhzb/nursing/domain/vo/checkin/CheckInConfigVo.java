package com.xhzb.nursing.domain.vo.checkin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 入住详情-入住配置VO（包含入住时间与床位信息）
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Data
@Schema(description = "入住详情-入住配置")
public class CheckInConfigVo {

    /** 主键ID */
    @Schema(title = "主键ID")
    private Long id;

    /** 入住记录ID */
    @Schema(title = "入住记录ID")
    private Long checkInId;

    /** 护理等级ID */
    @Schema(title = "护理等级ID")
    private Long nursingLevelId;

    /** 护理等级名称 */
    @Schema(title = "护理等级名称")
    private String nursingLevelName;

    /** 费用开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "费用开始时间")
    private LocalDateTime feeStartDate;

    /** 费用结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "费用结束时间")
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

    /** 排序编号 */
    @Schema(title = "排序编号")
    private Integer sortOrder;

    /** 入住开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "入住开始时间")
    private LocalDateTime startDate;

    /** 入住结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "入住结束时间")
    private LocalDateTime endDate;

    /** 入住床位编号 */
    @Schema(title = "入住床位编号")
    private String bedNumber;

    /** 创建者 */
    @Schema(title = "创建者")
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "创建时间")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "更新时间")
    private Date updateTime;
}
