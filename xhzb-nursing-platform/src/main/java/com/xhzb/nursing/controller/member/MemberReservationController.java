package com.xhzb.nursing.controller.member;

import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.nursing.domain.dto.member.MemberReservationDto;
import com.xhzb.nursing.service.IReservationService;
import com.xhzb.nursing.util.MemberTokenResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 家属小程序-参观预约Controller
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@RestController
@RequestMapping("/member/reservation")
@Tag(name = "家属小程序-参观预约")
public class MemberReservationController extends BaseController {

    @Autowired
    private IReservationService reservationService;

    @Autowired
    private MemberTokenResolver memberTokenResolver;

    /**
     * 查询当天取消预约次数（当天超过3次则不允许继续预约）
     *
     * @return 当天取消预约次数
     */
    @GetMapping("/cancelled-count")
    @Operation(summary = "查询取消预约数量")
    public AjaxResult cancelledCount() {
        return success(reservationService.countCancelledToday(memberTokenResolver.getCurrentMemberId()));
    }

    /**
     * 查询今天每个时间段剩余的预约次数
     *
     * @return 时间段与剩余次数集合
     */
    @GetMapping("/countByTime")
    @Operation(summary = "查询每个时间段剩余预约次数")
    public AjaxResult countByTime() {
        return success(reservationService.countRemainingByTime());
    }

    /**
     * 新增预约
     *
     * @param dto 预约参数
     * @return 操作结果
     */
    @PostMapping({"", "/"})
    @Operation(summary = "新增预约")
    public AjaxResult add(@Valid @RequestBody MemberReservationDto dto) {
        reservationService.createReservation(memberTokenResolver.getCurrentMemberId(), dto);
        return success();
    }

    /**
     * 分页查询当前家属的预约
     *
     * @param status 预约状态（可选，为空时查询全部）
     * @return 分页后的预约列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询预约")
    public AjaxResult page(@Schema(title = "预约状态（0-待报道，1-已完成，2-取消，3-过期）") Integer status) {
        startPage();
        return success(getDataTable(reservationService.listMemberReservations(
                memberTokenResolver.getCurrentMemberId(), status)));
    }

    /**
     * 取消预约
     *
     * @param id 预约记录ID
     * @return 操作结果
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消预约")
    public AjaxResult cancel(@Schema(name = "预约记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
                             @PathVariable Long id) {
        reservationService.cancelReservation(memberTokenResolver.getCurrentMemberId(), id);
        return success();
    }
}
