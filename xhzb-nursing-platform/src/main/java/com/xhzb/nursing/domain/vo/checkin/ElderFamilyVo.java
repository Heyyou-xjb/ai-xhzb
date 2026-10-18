package com.xhzb.nursing.domain.vo.checkin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 入住详情-家属信息VO
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Data
@Schema(description = "入住详情-家属信息")
public class ElderFamilyVo {

    /** 家属姓名 */
    @Schema(title = "家属姓名")
    private String name;

    /** 家属电话 */
    @Schema(title = "家属电话")
    private String phone;

    /** 与老人关系 */
    @Schema(title = "与老人关系")
    private String kinship;
}
