package com.xhzb.nursing.domain.dto.checkin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 入住老人基本信息（来自入住办理表单）
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Data
@Schema(description = "入住老人基本信息")
public class CheckInElderDto {

    /** 老人姓名 */
    @NotBlank(message = "老人姓名不能为空")
    @Schema(title = "老人姓名")
    private String name;

    /** 身份证号 */
    @NotBlank(message = "身份证号不能为空")
    @Schema(title = "身份证号")
    private String idCardNo;

    /** 出生日期 */
    @Schema(title = "出生日期")
    private String birthday;

    /** 年龄 */
    @Schema(title = "年龄")
    private Integer age;

    /** 性别（0:女 1:男） */
    @Schema(title = "性别（0:女 1:男）")
    private Integer sex;

    /** 联系电话 */
    @Schema(title = "联系电话")
    private String phone;

    /** 家庭住址 */
    @Schema(title = "家庭住址")
    private String address;

    /** 头像 */
    @Schema(title = "头像")
    private String image;

    /** 身份证国徽面 */
    @Schema(title = "身份证国徽面")
    private String idCardNationalEmblemImg;

    /** 身份证人像面 */
    @Schema(title = "身份证人像面")
    private String idCardPortraitImg;

    /** 民族 */
    @Schema(title = "民族")
    private String nation;

    /** 文化程度 */
    @Schema(title = "文化程度")
    private String educationLevel;

    /** 社保卡号 */
    @Schema(title = "社保卡号")
    private String socialSecurityCard;

    /** 居住情况 */
    @Schema(title = "居住情况")
    private String livingSituation;

    /** 宗教信仰 */
    @Schema(title = "宗教信仰")
    private String religiousBelief;

    /** 经济来源 */
    @Schema(title = "经济来源")
    private String economicSource;

    /** 婚姻状况 */
    @Schema(title = "婚姻状况")
    private String maritalStatus;

    /** 医疗费用支付方式 */
    @Schema(title = "医疗费用支付方式")
    private String medicalPaymentMethod;

    /** 核心建议（来自评估结果，如：建议入住/不建议入住） */
    @Schema(title = "核心建议")
    private String coreSuggestion;
}
