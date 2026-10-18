package com.xhzb.nursing.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhzb.nursing.domain.CheckInConfig;

/**
 * 入住配置管理Mapper接口
 * 
 * @author ruoyi
 * @date 2026-10-18
 */
@Mapper
public interface CheckInConfigMapper extends BaseMapper<CheckInConfig>
{
    /**
     * 查询入住配置管理
     * 
     * @param id 入住配置管理主键
     * @return 入住配置管理
     */
    public CheckInConfig selectCheckInConfigById(Long id);

    /**
     * 查询入住配置管理列表
     * 
     * @param checkInConfig 入住配置管理
     * @return 入住配置管理集合
     */
    public List<CheckInConfig> selectCheckInConfigList(CheckInConfig checkInConfig);

    /**
     * 新增入住配置管理
     * 
     * @param checkInConfig 入住配置管理
     * @return 结果
     */
    public int insertCheckInConfig(CheckInConfig checkInConfig);

    /**
     * 修改入住配置管理
     * 
     * @param checkInConfig 入住配置管理
     * @return 结果
     */
    public int updateCheckInConfig(CheckInConfig checkInConfig);

    /**
     * 删除入住配置管理
     * 
     * @param id 入住配置管理主键
     * @return 结果
     */
    public int deleteCheckInConfigById(Long id);

    /**
     * 批量删除入住配置管理
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCheckInConfigByIds(Long[] ids);
}
