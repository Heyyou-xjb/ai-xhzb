package com.xhzb.nursing.domain.vo.member;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 预约时间段剩余次数VO
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Data
@Schema(description = "预约时间段剩余次数")
public class ReservationTimeCountVo {

    /** 预约时间段 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(title = "预约时间段")
    private LocalDateTime time;

    /** 该时间段剩余预约次数（0表示已约满） */
    @Schema(title = "剩余预约次数")
    private Integer count;
}
