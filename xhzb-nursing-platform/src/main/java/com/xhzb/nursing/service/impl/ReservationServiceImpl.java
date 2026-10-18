package com.xhzb.nursing.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.nursing.domain.Reservation;
import com.xhzb.nursing.domain.dto.member.MemberReservationDto;
import com.xhzb.nursing.domain.vo.member.ReservationTimeCountVo;
import com.xhzb.nursing.mapper.ReservationMapper;
import com.xhzb.nursing.service.IReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 预约信息Service业务层处理
 * 包含家属小程序的参观预约与预约过期检查
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Service
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements IReservationService
{
    private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);

    /** 预约状态：0-待报道 */
    private static final int STATUS_PENDING = 0;

    /** 预约状态：2-取消 */
    private static final int STATUS_CANCELLED = 2;

    /** 预约状态：3-过期 */
    private static final int STATUS_EXPIRED = 3;

    /** 预约类型：0-参观预约 */
    private static final int TYPE_VISIT = 0;

    /** 预约类型：1-探访预约 */
    private static final int TYPE_VISIT_ELDER = 1;

    /** 参观预约每天可选的时间段 */
    private static final List<LocalTime> VISIT_TIME_SLOTS = List.of(
            LocalTime.of(8, 0), LocalTime.of(10, 30), LocalTime.of(14, 0), LocalTime.of(16, 0));

    /** 每个时间段最大预约次数 */
    private static final int SLOT_CAPACITY = 6;

    /** 当天允许取消的最大次数，超过后当天不能再预约 */
    private static final int MAX_CANCELLED_PER_DAY = 3;

    /**
     * 家属小程序新增预约
     * 依次校验预约时间、当天取消次数、重复预约与时间段是否约满，校验通过后保存预约
     *
     * @param memberId 家属ID
     * @param dto      预约参数
     * @return 预约记录ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReservation(Long memberId, MemberReservationDto dto) {
        if (memberId == null) {
            throw new ServiceException("请先登录");
        }
        if (dto == null) {
            throw new ServiceException("预约信息不能为空");
        }
        if (StrUtil.isBlank(dto.getName()) || StrUtil.isBlank(dto.getMobile()) || StrUtil.isBlank(dto.getVisitor())) {
            throw new ServiceException("预约人、手机号与探访人不能为空");
        }
        //预约时间必须晚于当前时间
        if (dto.getTime() == null || !dto.getTime().isAfter(LocalDateTime.now())) {
            throw new ServiceException("预约时间必须晚于当前时间");
        }
        //预约类型不传时默认为参观预约
        Integer type = dto.getType() == null ? TYPE_VISIT : dto.getType();
        if (type != TYPE_VISIT && type != TYPE_VISIT_ELDER) {
            throw new ServiceException("预约类型不合法");
        }

        //当天取消预约次数大于3次时，不允许继续预约
        if (countCancelledToday(memberId) > MAX_CANCELLED_PER_DAY) {
            throw new ServiceException("当天取消预约次数已达上限，暂时无法预约");
        }

        //同一手机号同一时间段只能预约一次
        Long sameTimeCount = count(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getMobile, dto.getMobile())
                .eq(Reservation::getTime, dto.getTime()));
        if (sameTimeCount != null && sameTimeCount > 0) {
            throw new ServiceException("该时间段您已预约，请勿重复提交");
        }

        //时间段约满校验：已取消的预约不占用名额
        Long bookedCount = count(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getTime, dto.getTime())
                .ne(Reservation::getStatus, STATUS_CANCELLED));
        if (bookedCount != null && bookedCount >= SLOT_CAPACITY) {
            throw new ServiceException("该时间段预约已满，请选择其他时间");
        }

        Reservation reservation = new Reservation();
        reservation.setName(dto.getName());
        reservation.setMobile(dto.getMobile());
        reservation.setTime(dto.getTime());
        reservation.setVisitor(dto.getVisitor());
        reservation.setType(type);
        reservation.setStatus(STATUS_PENDING);
        reservation.setCreateBy(String.valueOf(memberId));
        reservation.setCreateTime(DateUtils.getNowDate());
        save(reservation);
        return reservation.getId();
    }

    /**
     * 分页查询当前家属的预约列表（按预约时间升序）
     *
     * @param memberId 家属ID
     * @param status   预约状态（为空时查询全部）
     * @return 预约集合
     */
    @Override
    public List<Reservation> listMemberReservations(Long memberId, Integer status) {
        if (memberId == null) {
            throw new ServiceException("请先登录");
        }
        //只查询当前家属自己创建的预约，可选按状态过滤（如只看待上门）
        return list(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getCreateBy, String.valueOf(memberId))
                .eq(status != null, Reservation::getStatus, status)
                .orderByAsc(Reservation::getTime)
                .orderByAsc(Reservation::getId));
    }

    /**
     * 取消预约：只能取消自己的、待报道且尚未到预约时间的预约
     *
     * @param memberId 家属ID
     * @param id       预约记录ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReservation(Long memberId, Long id) {
        if (memberId == null) {
            throw new ServiceException("请先登录");
        }
        Reservation reservation = getById(id);
        if (reservation == null) {
            throw new ServiceException("预约记录不存在");
        }
        //只能操作自己的预约
        if (!String.valueOf(memberId).equals(reservation.getCreateBy())) {
            throw new ServiceException("无权操作该预约");
        }
        //只有待报道的预约可以取消
        if (reservation.getStatus() == null || reservation.getStatus() != STATUS_PENDING) {
            throw new ServiceException("该预约当前状态不可取消");
        }
        //预约时间已过的不允许取消，由过期检查任务统一标记为过期
        if (reservation.getTime() != null && !reservation.getTime().isAfter(LocalDateTime.now())) {
            throw new ServiceException("预约时间已过，无法取消");
        }

        Reservation update = new Reservation();
        update.setId(id);
        update.setStatus(STATUS_CANCELLED);
        update.setUpdateBy(String.valueOf(memberId));
        update.setUpdateTime(DateUtils.getNowDate());
        updateById(update);
    }

    /**
     * 查询当天取消预约次数（按取消操作的更新时间统计）
     *
     * @param memberId 家属ID
     * @return 当天取消次数
     */
    @Override
    public int countCancelledToday(Long memberId) {
        if (memberId == null) {
            return 0;
        }
        ZoneId zone = ZoneId.systemDefault();
        Date startOfDay = Date.from(LocalDate.now(zone).atStartOfDay(zone).toInstant());
        Date startOfNextDay = Date.from(LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant());

        Long cancelledCount = count(Wrappers.<Reservation>lambdaQuery()
                .eq(Reservation::getCreateBy, String.valueOf(memberId))
                .eq(Reservation::getStatus, STATUS_CANCELLED)
                .ge(Reservation::getUpdateTime, startOfDay)
                .lt(Reservation::getUpdateTime, startOfNextDay));
        return cancelledCount == null ? 0 : cancelledCount.intValue();
    }

    /**
     * 查询今天每个时间段剩余的预约次数
     * 已过去的时间段不再返回；未约满的时间段也不返回（前端按默认剩余次数处理）
     *
     * @return 时间段与剩余次数集合
     */
    @Override
    public List<ReservationTimeCountVo> countRemainingByTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        List<ReservationTimeCountVo> result = new ArrayList<>();

        for (LocalTime timeSlot : VISIT_TIME_SLOTS) {
            LocalDateTime slotTime = LocalDateTime.of(today, timeSlot);
            //已过去的时间段不再返回
            if (!slotTime.isAfter(now)) {
                continue;
            }
            //统计该时间段已占用的预约数量（取消的预约不占用名额）
            Long bookedCount = count(Wrappers.<Reservation>lambdaQuery()
                    .eq(Reservation::getTime, slotTime)
                    .ne(Reservation::getStatus, STATUS_CANCELLED));
            int remaining = SLOT_CAPACITY - (bookedCount == null ? 0 : bookedCount.intValue());
            //未约满的时间段不返回，前端视为可预约SLOT_CAPACITY次
            if (remaining >= SLOT_CAPACITY) {
                continue;
            }
            ReservationTimeCountVo timeCountVo = new ReservationTimeCountVo();
            timeCountVo.setTime(slotTime);
            timeCountVo.setCount(Math.max(remaining, 0));
            result.add(timeCountVo);
        }
        return result;
    }

    /**
     * 把预约时间已过且仍为待报道的预约标记为过期
     *
     * @return 本次标记过期的预约数量
     */
    @Override
    public int expireOverdueReservations() {
        int expiredCount = getBaseMapper().update(null, Wrappers.<Reservation>lambdaUpdate()
                .set(Reservation::getStatus, STATUS_EXPIRED)
                .set(Reservation::getUpdateTime, DateUtils.getNowDate())
                .eq(Reservation::getStatus, STATUS_PENDING)
                .lt(Reservation::getTime, LocalDateTime.now()));
        if (expiredCount > 0) {
            log.info("预约过期检查：{} 条未按时来访的预约已标记为过期", expiredCount);
        }
        return expiredCount;
    }
}
