package com.xhzb.nursing.domain.dto.checkin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 老人家属信息
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Data
@Schema(description = "老人家属信息")
public class ElderFamilyDto {

    /** 家属姓名 */
    @Schema(title = "家属姓名")
    private String name;

    /** 家属电话 */
    @Schema(title = "家属电话")
    private String phone;

    /** 与老人关系（0:子女 1:配偶 2:亲属 3:其他，按前端下拉值原样保存） */
    @Schema(title = "与老人关系")
    private String kinship;
}
