package com.xhzb.nursing.service;

import java.util.List;
import com.xhzb.nursing.domain.NursingProject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.vo.NursingProjectVo;

/**
 * 护理项目Service接口
 * 
 * @author ruoyi
 * @date 2025-03-28
 */
public interface INursingProjectService extends IService<NursingProject>
{
    /**
     * 查询护理项目
     * 
     * @param id 护理项目主键
     * @return 护理项目
     */
    public NursingProject selectNursingProjectById(Long id);

    /**
     * 查询护理项目列表
     * 
     * @param nursingProject 护理项目
     * @return 护理项目集合
     */
    public List<NursingProject> selectNursingProjectList(NursingProject nursingProject);

    /**
     * 新增护理项目
     * 
     * @param nursingProject 护理项目
     * @return 结果
     */
    public int insertNursingProject(NursingProject nursingProject);

    /**
     * 修改护理项目
     * 
     * @param nursingProject 护理项目
     * @return 结果
     */
    public int updateNursingProject(NursingProject nursingProject);

    /**
     * 批量删除护理项目
     * 
     * @param ids 需要删除的护理项目主键集合
     * @return 结果
     */
    public int deleteNursingProjectByIds(Long[] ids);

    /**
     * 删除护理项目信息
     * 
     * @param id 护理项目主键
     * @return 结果
     */
    public int deleteNursingProjectById(Long id);

    /**
     * 查询所有的护理项目
     * @return
     */
    List<NursingProjectVo> listAll();

    /**
     * 查询所有已启用的护理项目（含名称、单位、价格、护理要求等完整信息）
     * 供智能客服大模型检索数据库内容使用
     *
     * @return 已启用的护理项目集合
     */
    List<NursingProject> listEnabledProjects();

    /**
     * 小程序服务页：按名称与状态分页查询护理项目
     *
     * @param query 查询条件（name：名称模糊匹配，status：状态）
     * @return 护理项目集合
     */
    List<NursingProject> listForMember(NursingProject query);

    /**
     * 小程序服务页：根据编号查询护理项目详情
     *
     * @param id 护理项目主键
     * @return 护理项目详情
     */
    NursingProject getMemberProjectDetail(Long id);
}
