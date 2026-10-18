package com.xhzb.nursing.domain.vo.checkin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 楼层-房间-床位树形节点VO（前端级联选择使用）
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Data
@Schema(description = "楼层-房间-床位树形节点")
public class BedTreeVo {

    /** 节点值（楼层ID/房间ID/床位ID） */
    @Schema(title = "节点值")
    private String value;

    /** 节点名称（楼层名称/房间编号/床位编号） */
    @Schema(title = "节点名称")
    private String label;

    /** 子节点 */
    @Schema(title = "子节点")
    private List<BedTreeVo> children;
}
