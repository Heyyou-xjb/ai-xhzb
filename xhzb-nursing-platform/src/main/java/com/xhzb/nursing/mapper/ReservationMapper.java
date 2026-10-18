package com.xhzb.nursing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhzb.nursing.domain.Reservation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预约信息Mapper接口
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation>
{
}
