package com.xhzb.nursing.domain.dto.member;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 家属小程序新增预约参数
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Data
@Schema(description = "家属小程序新增预约参数")
public class MemberReservationDto {

    /** 预约人姓名 */
    @NotBlank(message = "预约人姓名不能为空")
    @Schema(title = "预约人姓名")
    private String name;

    /** 预约人手机号 */
    @NotBlank(message = "预约人手机号不能为空")
    @Schema(title = "预约人手机号")
    private String mobile;

    /** 预约时间 */
    @NotNull(message = "预约时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "预约时间")
    private LocalDateTime time;

    /** 探访人（家人/老人姓名） */
    @NotBlank(message = "探访人不能为空")
    @Schema(title = "探访人")
    private String visitor;

    /** 预约类型：0-参观预约，1-探访预约 */
    @Schema(title = "预约类型（0-参观预约，1-探访预约）")
    private Integer type;
}
