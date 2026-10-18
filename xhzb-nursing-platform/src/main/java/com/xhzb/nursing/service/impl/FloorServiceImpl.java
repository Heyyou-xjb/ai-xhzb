package com.xhzb.nursing.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.collection.CollUtil;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.nursing.domain.Floor;
import com.xhzb.nursing.mapper.FloorMapper;
import com.xhzb.nursing.service.IFloorService;
import com.xhzb.nursing.domain.vo.FloorVo;
import com.xhzb.nursing.domain.vo.checkin.BedTreeVo;
import com.xhzb.nursing.domain.vo.checkin.FloorBedRowVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 楼层Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-03-28
 */
@Service
public class FloorServiceImpl extends ServiceImpl<FloorMapper, Floor> implements IFloorService
{
    @Autowired
    private FloorMapper floorMapper;

    /**
     * 查询楼层
     * 
     * @param id 楼层主键
     * @return 楼层
     */
    @Override
    public Floor selectFloorById(Long id)
    {
        return getById(id);
    }

    /**
     * 新增楼层
     * 
     * @param floor 楼层
     * @return 结果
     */
    @Override
    public int insertFloor(Floor floor)
    {
        return save(floor) ? 1 : 0;
    }

    /**
     * 修改楼层
     * 
     * @param floor 楼层
     * @return 结果
     */
    @Override
    public int updateFloor(Floor floor)
    {
        return updateById(floor) ? 1 : 0;
    }

    /**
     * 批量删除楼层
     * 
     * @param ids 需要删除的楼层主键
     * @return 结果
     */
    @Override
    public int deleteFloorByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除楼层信息
     * 
     * @param id 楼层主键
     * @return 结果
     */
    @Override
    public int deleteFloorById(Long id)
    {
        return removeById(id) ? 1 : 0;
    }


    /**
     * 查询负责老人楼层
     * @return
     */
    @Override
    public List<Floor> selectAllByNur() {
        return floorMapper.selectAllByNur();
    }

    /**
     * 根据床位状态查询 楼层-房间-床位 树形数据
     * 先按床位状态关联查询楼层、房间、床位，再在内存中组装成 楼层 → 房间 → 床位 的树形结构
     *
     * @param status 床位状态：0-未入住，1-已入住
     * @return 楼层-房间-床位树形数据
     */
    @Override
    public List<BedTreeVo> getRoomAndBedByBedStatus(Integer status) {
        //床位状态只允许未入住(0)与已入住(1)
        if (status == null || (status != 0 && status != 1)) {
            throw new ServiceException("床位状态不合法");
        }

        List<FloorBedRowVo> rows = floorMapper.selectFloorBedRows(status);
        if (CollUtil.isEmpty(rows)) {
            return List.of();
        }

        //LinkedHashMap 保证楼层与房间按SQL中的排序顺序输出
        Map<Long, BedTreeVo> floorNodes = new LinkedHashMap<>();
        Map<Long, BedTreeVo> roomNodes = new HashMap<>();
        for (FloorBedRowVo row : rows) {
            //1. 楼层节点
            BedTreeVo floorNode = floorNodes.get(row.getFloorId());
            if (floorNode == null) {
                floorNode = buildTreeNode(String.valueOf(row.getFloorId()), row.getFloorName());
                floorNodes.put(row.getFloorId(), floorNode);
            }
            //2. 房间节点，挂在所属楼层下
            BedTreeVo roomNode = roomNodes.get(row.getRoomId());
            if (roomNode == null) {
                roomNode = buildTreeNode(String.valueOf(row.getRoomId()), row.getRoomCode());
                roomNodes.put(row.getRoomId(), roomNode);
                floorNode.getChildren().add(roomNode);
            }
            //3. 床位节点，挂在所属房间下
            roomNode.getChildren().add(buildTreeNode(String.valueOf(row.getBedId()), row.getBedNumber()));
        }
        return new ArrayList<>(floorNodes.values());
    }

    /**
     * 构建楼层/房间/床位树形节点
     *
     * @param value 节点值
     * @param label 节点名称
     * @return 树形节点
     */
    private BedTreeVo buildTreeNode(String value, String label) {
        BedTreeVo node = new BedTreeVo();
        node.setValue(value);
        node.setLabel(label);
        node.setChildren(new ArrayList<>());
        return node;
    }
}
