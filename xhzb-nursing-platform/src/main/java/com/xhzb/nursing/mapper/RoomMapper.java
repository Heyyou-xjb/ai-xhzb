package com.xhzb.nursing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhzb.nursing.domain.Room;
import com.xhzb.nursing.domain.vo.RoomVo;
import com.xhzb.nursing.domain.vo.checkin.RoomInfoVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 房间Mapper接口
 * 
 * @author ruoyi
 * @date 2025-03-28
 */
@Mapper
public interface RoomMapper extends BaseMapper<Room>
{
    /**
     * 查询房间
     * 
     * @param id 房间主键
     * @return 房间
     */
    public Room selectRoomById(Long id);

    /**
     * 查询房间列表
     * 
     * @param room 房间
     * @return 房间集合
     */
    public List<Room> selectRoomList(Room room);

    /**
     * 新增房间
     * 
     * @param room 房间
     * @return 结果
     */
    public int insertRoom(Room room);

    /**
     * 修改房间
     * 
     * @param room 房间
     * @return 结果
     */
    public int updateRoom(Room room);

    /**
     * 删除房间
     * 
     * @param id 房间主键
     * @return 结果
     */
    public int deleteRoomById(Long id);

    /**
     * 批量删除房间
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteRoomByIds(Long[] ids);

    List<RoomVo> selectByFloorId(Long floorId);

    List<RoomVo> selectByFloorIdWithNur(Long floorId);

    /**
     * 根据房间ID查询房间信息（关联楼层、房型，room.type_name = room_type.name）
     *
     * @param id 房间ID
     * @return 房间信息
     */
    RoomInfoVo selectRoomInfoById(@Param("id") Long id);

}
