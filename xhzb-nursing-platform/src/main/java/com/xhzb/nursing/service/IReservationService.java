package com.xhzb.nursing.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.Reservation;
import com.xhzb.nursing.domain.dto.member.MemberReservationDto;
import com.xhzb.nursing.domain.vo.member.ReservationTimeCountVo;

/**
 * 预约信息Service接口
 *
 * @author xujingbo
 * @date 2026-10-18
 */
public interface IReservationService extends IService<Reservation>
{
    /**
     * 家属小程序新增预约
     *
     * @param memberId 家属ID
     * @param dto      预约参数
     * @return 预约记录ID
     */
    Long createReservation(Long memberId, MemberReservationDto dto);

    /**
     * 分页查询当前家属的预约列表（按预约时间升序）
     *
     * @param memberId 家属ID
     * @param status   预约状态（为空时查询全部）
     * @return 预约集合
     */
    List<Reservation> listMemberReservations(Long memberId, Integer status);

    /**
     * 取消预约
     *
     * @param memberId 家属ID
     * @param id       预约记录ID
     */
    void cancelReservation(Long memberId, Long id);

    /**
     * 查询当天取消预约次数
     *
     * @param memberId 家属ID
     * @return 当天取消次数
     */
    int countCancelledToday(Long memberId);

    /**
     * 查询今天每个时间段剩余的预约次数
     * 未约满的时间段不返回，前端按默认剩余次数处理
     *
     * @return 时间段与剩余次数集合
     */
    List<ReservationTimeCountVo> countRemainingByTime();

    /**
     * 把预约时间已过且仍为待报道的预约标记为过期
     *
     * @return 本次标记过期的预约数量
     */
    int expireOverdueReservations();
}
