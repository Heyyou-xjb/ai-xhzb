package com.xhzb.nursing.service;

import java.util.List;

import com.xhzb.nursing.domain.HealthAssessment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.HealthAssessmentDataCollection;
import com.xhzb.nursing.domain.HealthAssessmentReport;
import com.xhzb.nursing.domain.dto.health.ElderAssessmentDto;

/**
 * 健康评估记录Service接口
 *
 * @author xujingbo
 * @date 2026-10-17
 */
public interface IHealthAssessmentService extends IService<HealthAssessment> {
    /**
     * 查询健康评估记录
     *
     * @param id 健康评估记录主键
     * @return 健康评估记录
     */
    public HealthAssessmentDataCollection selectHealthAssessmentById(Long id);

    /**
     * 查询健康评估记录列表
     *
     * @param healthAssessment 健康评估记录
     * @return 健康评估记录集合
     */
    public List<HealthAssessment> selectHealthAssessmentList(HealthAssessment healthAssessment);

    /**
     * 新增健康评估记录
     *
     * @param healthAssessment 健康评估记录
     * @return 结果
     */
    public int insertHealthAssessment(HealthAssessment healthAssessment);

    /**
     * 新增或更新健康评估记录
     *
     * @param dto 健康评估收集信息
     * @return 结果
     */
    public Long insertOrUpdateHealthAssessment(ElderAssessmentDto dto);

    /**
     * 修改健康评估记录
     *
     * @param healthAssessment 健康评估记录
     * @return 结果
     */
    public int updateHealthAssessment(HealthAssessment healthAssessment);

    /**
     * 批量删除健康评估记录
     *
     * @param ids 需要删除的健康评估记录主键集合
     * @return 结果
     */
    public int deleteHealthAssessmentByIds(Long[] ids);

    /**
     * 删除健康评估记录信息
     *
     * @param id 健康评估记录主键
     * @return 结果
     */
    public int deleteHealthAssessmentById(Long id);

    /**
     * AI评估
     * @param dto
     * @return
     */
    Long assessmentData(ElderAssessmentDto dto);

    /**
     * 查看评估详情(评估结果)
     * @param id
     * @return
     */
    HealthAssessmentReport getAssessmentDetail(Long id);
}
