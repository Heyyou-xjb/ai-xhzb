package com.xhzb.nursing.domain.vo.checkin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 楼层-房间-床位关联查询结果（用于在Service层组装成树形结构）
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Data
@Schema(description = "楼层-房间-床位关联查询结果")
public class FloorBedRowVo {

    /** 楼层ID */
    @Schema(title = "楼层ID")
    private Long floorId;

    /** 楼层名称 */
    @Schema(title = "楼层名称")
    private String floorName;

    /** 房间ID */
    @Schema(title = "房间ID")
    private Long roomId;

    /** 房间编号 */
    @Schema(title = "房间编号")
    private String roomCode;

    /** 床位ID */
    @Schema(title = "床位ID")
    private Long bedId;

    /** 床位编号 */
    @Schema(title = "床位编号")
    private String bedNumber;
}
