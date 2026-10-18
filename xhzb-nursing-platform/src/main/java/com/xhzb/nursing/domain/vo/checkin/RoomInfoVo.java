package com.xhzb.nursing.domain.vo.checkin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 房间信息VO（楼层、房间编号与房型价格）
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Data
@Schema(description = "房间信息")
public class RoomInfoVo {

    /** 楼层名称 */
    @Schema(title = "楼层名称")
    private String floorName;

    /** 楼层ID */
    @Schema(title = "楼层ID")
    private String floorId;

    /** 房间ID */
    @Schema(title = "房间ID")
    private String roomId;

    /** 房间编号 */
    @Schema(title = "房间编号")
    private String code;

    /** 床位费用（房型价格） */
    @Schema(title = "床位费用")
    private String price;
}
