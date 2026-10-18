package com.xhzb.nursing.service.impl;

import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.nursing.domain.NursingProjectPlan;
import com.xhzb.nursing.domain.vo.NursingProjectVo;
import com.xhzb.nursing.mapper.NursingProjectPlanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.NursingProjectMapper;
import com.xhzb.nursing.domain.NursingProject;
import com.xhzb.nursing.service.INursingProjectService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Arrays;
/**
 * 护理项目Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-03-28
 */
@Service
public class NursingProjectServiceImpl extends ServiceImpl<NursingProjectMapper,NursingProject> implements INursingProjectService
{
    @Autowired
    private NursingProjectMapper nursingProjectMapper;

    @Autowired
    private NursingProjectPlanMapper nursingProjectPlanMapper;

    /**
     * 查询护理项目
     * 
     * @param id 护理项目主键
     * @return 护理项目
     */
    @Override
    public NursingProject selectNursingProjectById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询护理项目列表
     * 
     * @param nursingProject 护理项目
     * @return 护理项目
     */
    @Override
    public List<NursingProject> selectNursingProjectList(NursingProject nursingProject)
    {
        return nursingProjectMapper.selectNursingProjectList(nursingProject);
    }

    /**
     * 新增护理项目
     * 
     * @param nursingProject 护理项目
     * @return 结果
     */
    @Override
    public int insertNursingProject(NursingProject nursingProject)
    {
        return save(nursingProject)?1:0;
    }

    /**
     * 修改护理项目
     * 
     * @param nursingProject 护理项目
     * @return 结果
     */
    @Override
    public int updateNursingProject(NursingProject nursingProject)
    {
        //判断护理项目是否被引用，如果被引用，则不能修改
        Long count = nursingProjectPlanMapper.selectCount(Wrappers.<NursingProjectPlan>lambdaQuery().eq(NursingProjectPlan::getProjectId, nursingProject.getId()));
        if(count > 0){
            throw new ServiceException("护理项目被引用，不能修改");
        }


        return updateById(nursingProject)?1:0;
    }

    /**
     * 批量删除护理项目
     * 
     * @param ids 需要删除的护理项目主键
     * @return 结果
     */
    @Override
    public int deleteNursingProjectByIds(Long[] ids)
    {
        //判断护理项目是否被引用，如果被引用，则不能修改
        Long count = nursingProjectPlanMapper.selectCount(Wrappers.<NursingProjectPlan>lambdaQuery().in(NursingProjectPlan::getProjectId, Arrays.asList(ids)));
        if(count > 0){
            throw new ServiceException("护理项目被引用，不能删除");
        }
        return removeByIds(Arrays.asList(ids))?1:0;
    }

    /**
     * 删除护理项目信息
     * 
     * @param id 护理项目主键
     * @return 结果
     */
    @Override
    public int deleteNursingProjectById(Long id)
    {
        //判断护理项目是否被引用，如果被引用，则不能修改
        Long count = nursingProjectPlanMapper.selectCount(Wrappers.<NursingProjectPlan>lambdaQuery().eq(NursingProjectPlan::getProjectId, id));
        if(count > 0){
            throw new ServiceException("护理项目被引用，不能删除");
        }

        return removeById(id)?1:0;
    }

    /**
     * 查询所有的护理项目
     * @return
     */
    @Override
    public List<NursingProjectVo> listAll() {
        return nursingProjectMapper.listAll();
    }

    /**
     * 查询所有已启用的护理项目（含名称、单位、价格、护理要求等完整信息）
     * 供智能客服大模型检索数据库内容使用
     *
     * @return 已启用的护理项目集合
     */
    @Override
    public List<NursingProject> listEnabledProjects() {
        //只查询启用状态（status=1）的护理项目，按排序号升序展示
        return list(Wrappers.<NursingProject>lambdaQuery()
                .eq(NursingProject::getStatus, 1)
                .orderByAsc(NursingProject::getOrderNo));
    }

    /**
     * 小程序服务页：按名称与状态分页查询护理项目
     * 名称支持模糊匹配，结果按排序号升序展示；分页由Controller中的startPage完成
     *
     * @param query 查询条件（name：名称模糊匹配，status：状态）
     * @return 护理项目集合
     */
    @Override
    public List<NursingProject> listForMember(NursingProject query) {
        NursingProject condition = query == null ? new NursingProject() : query;
        return list(Wrappers.<NursingProject>lambdaQuery()
                .like(StrUtil.isNotBlank(condition.getName()), NursingProject::getName, condition.getName())
                .eq(condition.getStatus() != null, NursingProject::getStatus, condition.getStatus())
                .orderByAsc(NursingProject::getOrderNo)
                .orderByAsc(NursingProject::getId));
    }

    /**
     * 小程序服务页：根据编号查询护理项目详情
     *
     * @param id 护理项目主键
     * @return 护理项目详情
     */
    @Override
    public NursingProject getMemberProjectDetail(Long id) {
        if (id == null) {
            throw new ServiceException("服务项目编号不能为空");
        }
        NursingProject nursingProject = getById(id);
        if (nursingProject == null) {
            throw new ServiceException("服务项目不存在");
        }
        return nursingProject;
    }
}
