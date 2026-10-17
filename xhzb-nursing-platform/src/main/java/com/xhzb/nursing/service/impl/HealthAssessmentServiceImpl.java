package com.xhzb.nursing.service.impl;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.nursing.constant.SystemConstant;
import com.xhzb.nursing.domain.HealthAssessment;
import com.xhzb.nursing.domain.HealthAssessmentDataCollection;
import com.xhzb.nursing.domain.HealthAssessmentReport;
import com.xhzb.nursing.domain.RoomType;
import com.xhzb.nursing.domain.dto.health.ElderAssessmentDto;
import com.xhzb.nursing.domain.vo.ElderLevelResultVO;
import com.xhzb.nursing.domain.vo.HealthReportResultVO;
import com.xhzb.nursing.mapper.HealthAssessmentMapper;
import com.xhzb.nursing.service.IHealthAssessmentDataCollectionService;
import com.xhzb.nursing.service.IHealthAssessmentReportService;
import com.xhzb.nursing.service.IHealthAssessmentService;
import com.xhzb.nursing.service.IRoomTypeService;
import com.xhzb.nursing.util.PDFUtil;
import com.xhzb.oss.client.OSSAliyunFileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 健康评估记录Service业务层处理
 * 包含AI健康评估的两阶段业务流程：老人多维度能力评估 + 体检报告分析
 *
 * @author xujingbo
 * @date 2026-10-17
 */
@Service
public class HealthAssessmentServiceImpl extends ServiceImpl<HealthAssessmentMapper, HealthAssessment> implements IHealthAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(HealthAssessmentServiceImpl.class);

    /** AI评估失败时返回的业务状态码：前端据此弹出"评估失败"提示 */
    private static final Integer AI_ASSESS_FAIL_CODE = 10098;

    /** 建议入住的最低健康评分 */
    private static final double MIN_CHECK_IN_SCORE = 60D;

    /** 第一阶段AI可能返回的能力等级（失能为标准表述） */
    private static final List<String> ABILITY_LEVELS = List.of("能力完好", "轻度失能", "中度失能", "重度失能");

    @Autowired
    private HealthAssessmentMapper healthAssessmentMapper;

    @Autowired
    private IHealthAssessmentDataCollectionService healthAssessmentDataCollectionService;

    @Autowired
    private IHealthAssessmentReportService reportService;

    @Autowired
    private IRoomTypeService roomTypeService;

    @Autowired
    private OSSAliyunFileStorageService ossAliyunFileStorageService;

    /** AI健康评估专用的对话实例（不带会话记忆与知识库检索，按单次提示词返回JSON） */
    @Autowired
    @Qualifier("chatClientDeepseekHealth")
    private ChatClient chatClientDeepseekHealth;

    /** 用于把"需要原子性的数据库操作"包在短事务中，避免与耗时的大模型调用共用事务 */
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 查询健康评估记录（返回评估数据采集内容，供编辑评估时回显）
     *
     * @param id 健康评估记录主键
     * @return 健康评估数据采集内容
     */
    @Override
    public HealthAssessmentDataCollection selectHealthAssessmentById(Long id) {
        return healthAssessmentDataCollectionService.getById(id);
    }

    /**
     * 查询健康评估记录列表
     *
     * @param healthAssessment 健康评估记录
     * @return 健康评估记录集合
     */
    @Override
    public List<HealthAssessment> selectHealthAssessmentList(HealthAssessment healthAssessment) {
        return healthAssessmentMapper.selectHealthAssessmentList(healthAssessment);
    }

    /**
     * 新增健康评估记录
     *
     * @param healthAssessment 健康评估记录
     * @return 结果
     */
    @Override
    public int insertHealthAssessment(HealthAssessment healthAssessment) {
        return save(healthAssessment) ? 1 : 0;
    }

    /**
     * 修改健康评估记录
     *
     * @param healthAssessment 健康评估记录
     * @return 结果
     */
    @Override
    public int updateHealthAssessment(HealthAssessment healthAssessment) {
        return updateById(healthAssessment) ? 1 : 0;
    }

    /**
     * 批量删除健康评估记录
     *
     * @param ids 需要删除的健康评估记录主键
     * @return 结果
     */
    @Override
    public int deleteHealthAssessmentByIds(Long[] ids) {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除健康评估记录信息
     *
     * @param id 健康评估记录主键
     * @return 结果
     */
    @Override
    public int deleteHealthAssessmentById(Long id) {
        return removeById(id) ? 1 : 0;
    }

    /**
     * 新增或更新健康评估记录（保存基础表单数据与评估数据采集内容）
     *
     * @param dto 健康评估收集信息
     * @return 健康评估记录主键ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long insertOrUpdateHealthAssessment(ElderAssessmentDto dto) {
        return saveAssessmentData(dto);
    }

    /**
     * AI评估：老人能力评估 + 体检报告分析
     * 流程：保存基础数据 → 提取能力数据 → 第一阶段AI（能力等级）→ 保存报告
     *      → 第二阶段AI（体检报告）→ 回填报告并更新主记录状态 → 返回评估ID
     *
     * @param dto 健康评估收集信息
     * @return 健康评估记录主键ID
     */
    @Override
    public Long assessmentData(ElderAssessmentDto dto) {
        // ===== 1. 更新保存评估数据：只把数据库写操作放在短事务中，AI调用不占用事务 =====
        Long assessmentId = transactionTemplate.execute(status -> saveAssessmentData(dto));

        // ===== 2. 提取各项能力数据 =====
        // 2.1 四大模块能力等级：中文等级转换为数字编码（能力完好-0、轻度受损-1、中度受损-2、重度受损-3）
        String dailyActivityLevel = getCurrentLevel(dto.getDailyLivingActivities().getAbilityRating(), "日常生活活动");
        String mentalStatusLevel = getCurrentLevel(dto.getMentalState().getAbilityRating(), "精神状态");
        String perceptionCommunicationLevel = getCurrentLevel(dto.getPerceptionAndCommunication().getAbilityRating(), "感知觉与沟通");
        String socialParticipationLevel = getCurrentLevel(dto.getSocialParticipation().getAbilityRating(), "社会参与");

        // 2.2 近30天意外事件次数（未填写按0次处理）
        Integer fall = defaultCount(dto.getHealthAssessmentDto().getRecent30Days().getFall());
        Integer choking = defaultCount(dto.getHealthAssessmentDto().getRecent30Days().getChoking());
        Integer suicideAttempt = defaultCount(dto.getHealthAssessmentDto().getRecent30Days().getSuicideAttempt());
        Integer lost = defaultCount(dto.getHealthAssessmentDto().getRecent30Days().getLost());
        Integer coma = defaultCount(dto.getHealthAssessmentDto().getRecent30Days().getComa());

        // 2.3 痴呆、精神疾病（0-无，1-有）
        Integer dementia = hasDisease(dto.getHealthAssessmentDto().getDiseaseDiagnosis().getDementia());
        Integer mentalIllness = hasDisease(dto.getHealthAssessmentDto().getDiseaseDiagnosis().getMentalIllness());

        // 2.4 是否确诊认知障碍：画钟测试结果为2表示确诊认知障碍
        Integer knownCognitiveImpairment = isCognitiveImpairment(dto.getMentalState().getClockDrawingTest().getResult());

        // ===== 3. 第一阶段AI分析：老人多维度能力评估 =====
        // 3.1 组装提示词，带上评估规则
        String userPrompt1 = String.format(SystemConstant.USER_PROMPT1,
                dailyActivityLevel, mentalStatusLevel, perceptionCommunicationLevel, socialParticipationLevel,
                fall, choking, suicideAttempt, lost, coma,
                dementia, mentalIllness, knownCognitiveImpairment);

        // 3.2 调用大模型，3.3 解析出JSON结果（初始能力等级、最终能力等级、等级变化原因）
        ElderLevelResultVO levelResult = chatBigModel(userPrompt1, ElderLevelResultVO.class);
        validateLevelResult(levelResult);
        String initialAbilityLevel = normalizeAbilityLevel(levelResult.getPreLevel());
        String finalAbilityLevel = normalizeAbilityLevel(levelResult.getFinalLevel());

        // 3.4 保存评估基础信息与第一阶段分析结果
        // 存在旧报告时复用（AI分析失败重试的场景），避免同一评估产生多份报告
        HealthAssessmentReport existReport = reportService.lambdaQuery()
                .eq(HealthAssessmentReport::getHealthAssessmentId, assessmentId)
                .orderByDesc(HealthAssessmentReport::getId)
                .last("limit 1")
                .one();
        HealthAssessmentReport report = existReport == null ? new HealthAssessmentReport() : existReport;
        if (existReport == null) {
            report.setHealthAssessmentId(assessmentId);
            report.setCreateBy(SecurityUtils.getUserId().toString());
            report.setCreateTime(DateUtils.getNowDate());
        }
        report.setAssessmentTime(LocalDateTime.now());
        report.setAssessorName(SecurityUtils.getUsername());
        report.setDailyActivityLevel(dailyActivityLevel);
        report.setMentalStatusLevel(mentalStatusLevel);
        report.setPerceptionCommunicationLevel(perceptionCommunicationLevel);
        report.setSocialParticipationLevel(socialParticipationLevel);
        report.setInitialAbilityLevel(initialAbilityLevel);
        report.setFinalAbilityLevel(finalAbilityLevel);
        report.setLevelChangeReason(StrUtil.nullToEmpty(levelResult.getReason()));
        report.setCheckInStatus(0);//入住状态统一标记为未入住
        reportService.saveOrUpdate(report);

        // ===== 4. 第二阶段AI分析：体检报告分析 =====
        // 4.1 从OSS下载体检报告PDF，4.2 解析提取全部文本内容
        String pdfContent = readMedicalReportText(dto);

        // 4.3 组装体检报告分析提示词，4.4 调用大模型、解析出健康评分/风险等级/异常项/系统评分/报告总结
        HealthReportResultVO healthResult = chatBigModel(
                String.format(SystemConstant.USER_PROMPT2, pdfContent), HealthReportResultVO.class);
        Double healthScore = validateHealthResult(healthResult);

        // ===== 5. 保存评估结果到评估结果表（业务判断规则） =====
        // 5.1 是否建议入住：健康评分 >= 60 分建议入住
        Integer coreSuggestion = healthScore >= MIN_CHECK_IN_SCORE ? 1 : 0;
        report.setCoreSuggestion(coreSuggestion);
        // 5.2 依据健康分数计算建议护理等级
        report.setRecommendedCareLevel(getCareLevel(healthScore));
        // 5.3 入住状态统一标记为未入住
        report.setCheckInStatus(0);
        // 5.4 第二阶段AI结果回填
        report.setHealthScore(String.valueOf(healthScore));
        report.setRiskLevel(healthResult.getRiskLevel());
        report.setReportSummary(StrUtil.nullToEmpty(healthResult.getSummarize()));
        report.setAbnormalAnalysis(JSONUtil.toJsonStr(
                healthResult.getAbnormalData() == null ? List.of() : healthResult.getAbnormalData()));
        report.setSystemScore(JSONUtil.toJsonStr(healthResult.getSystemScore()));
        // 5.5 报告其余字段按评估结果补充：推荐房型、护理重点、建议说明、家属配合、饮食、心理关怀、机构准备
        report.setRecommendedRoomType(recommendRoomType(finalAbilityLevel));
        report.setCareFocus(buildCareFocus(dailyActivityLevel, mentalStatusLevel, perceptionCommunicationLevel,
                socialParticipationLevel, fall, choking, suicideAttempt, lost, coma));
        report.setSuggestionDescription(buildSuggestionDescription(initialAbilityLevel, finalAbilityLevel,
                healthScore, healthResult.getRiskLevel(), coreSuggestion));
        report.setFamilyCooperation(buildFamilyCooperation(finalAbilityLevel, fall, choking, lost, suicideAttempt));
        report.setDietSuggestion(buildDietSuggestion(healthResult.getSystemScore()));
        report.setPsychologicalCare(buildPsychologicalCare(mentalStatusLevel, knownCognitiveImpairment));
        report.setInstitutionPreparation(buildInstitutionPreparation(finalAbilityLevel));
        report.setUpdateBy(SecurityUtils.getUsername());
        report.setUpdateTime(DateUtils.getNowDate());

        // ===== 6. 更新评估主记录的状态与入住建议，并回填评估报告 =====
        String assessmentDetails = buildAssessmentDetails(initialAbilityLevel, finalAbilityLevel,
                levelResult.getReason(), healthScore, healthResult);
        transactionTemplate.execute(status -> {
            // 6.1 回填两阶段AI产出的全部结果
            reportService.updateById(report);
            // 6.2 更新主记录：是否建议入住 + 评估进度（评估已完成）
            HealthAssessment healthAssessment = new HealthAssessment();
            healthAssessment.setId(assessmentId);
            healthAssessment.setCoreSuggestion(coreSuggestion);
            healthAssessment.setEvaluationProgress(1);
            healthAssessment.setUpdateBy(SecurityUtils.getUserId().toString());
            healthAssessment.setUpdateTime(DateUtils.getNowDate());
            this.updateById(healthAssessment);
            // 6.3 记录两阶段AI分析结果，便于后续追溯
            healthAssessmentDataCollectionService.lambdaUpdate()
                    .set(HealthAssessmentDataCollection::getAssessmentDetails, assessmentDetails)
                    .eq(HealthAssessmentDataCollection::getId, assessmentId)
                    .update();
            return null;
        });

        // ===== 7. 返回本次评估记录的唯一ID =====
        return assessmentId;
    }

    /**
     * 查看评估详情(评估结果)
     *
     * @param id 健康评估记录ID
     * @return 健康评估报告
     */
    @Override
    public HealthAssessmentReport getAssessmentDetail(Long id) {
        //一份评估记录对应一份评估报告；历史数据若存在多条，取最新一条
        return reportService.lambdaQuery()
                .eq(HealthAssessmentReport::getHealthAssessmentId, id)
                .orderByDesc(HealthAssessmentReport::getId)
                .last("limit 1")
                .one();
    }

    /**
     * 保存健康评估记录与评估数据采集内容（一个评估记录对应一份采集数据）
     *
     * @param dto 健康评估收集信息
     * @return 健康评估记录主键ID
     */
    private Long saveAssessmentData(ElderAssessmentDto dto) {
        //校验基础信息，避免后续流程出现空指针
        if (dto == null || dto.getBasicInfo() == null || StrUtil.isBlank(dto.getBasicInfo().getElderName())) {
            throw new ServiceException("评估基本信息不完整：老人姓名不能为空");
        }

        //封装健康评估记录对象
        HealthAssessment assessment = new HealthAssessment();
        assessment.setElderName(dto.getBasicInfo().getElderName());//老人姓名
        assessment.setIdCard(dto.getBasicInfo().getIdCard());//身份证号

        //1. 健康评估记录ID无值：新增评估记录
        if (dto.getId() == null) {
            assessment.setCheckInStatus(0);//未入住
            assessment.setEvaluationProgress(0);//评估中
            assessment.setCreateBy(SecurityUtils.getUserId().toString());
            assessment.setCreateTime(DateUtils.getNowDate());
            this.save(assessment);
        } else {
            //2. 健康评估记录ID有值：更新评估记录
            HealthAssessment healthAssessment = this.getById(dto.getId());
            //2.1 记录不存在时不允许更新
            if (healthAssessment == null) {
                throw new ServiceException("健康评估记录不存在，无法更新");
            }
            //2.2 只有"评估中"的记录允许修改
            if (healthAssessment.getEvaluationProgress() == null || healthAssessment.getEvaluationProgress() != 0) {
                throw new ServiceException("健康评估记录评估已完成或已取消，无法更新");
            }
            assessment.setId(dto.getId());
            assessment.setUpdateBy(SecurityUtils.getUserId().toString());
            assessment.setUpdateTime(DateUtils.getNowDate());
            this.updateById(assessment);
        }

        //3. 封装评估数据采集对象（主键与评估记录ID一致，便于按ID回显）
        HealthAssessmentDataCollection collection = new HealthAssessmentDataCollection();
        collection.setId(assessment.getId());
        collection.setBasicInfo(JSONUtil.toJsonStr(dto.getBasicInfo()));//基本信息
        collection.setHealthAssessment(JSONUtil.toJsonStr(dto.getHealthAssessmentDto()));//健康评估
        collection.setDailyLivingActivities(JSONUtil.toJsonStr(dto.getDailyLivingActivities()));//日常活动
        collection.setMentalState(JSONUtil.toJsonStr(dto.getMentalState()));//精神状态
        collection.setPerceptionCommunication(JSONUtil.toJsonStr(dto.getPerceptionAndCommunication()));//感知觉及沟通
        collection.setSocialParticipation(JSONUtil.toJsonStr(dto.getSocialParticipation()));//社会参与
        if (dto.getId() == null) {
            collection.setCreateBy(SecurityUtils.getUserId().toString());
            collection.setCreateTime(DateUtils.getNowDate());
        }
        collection.setUpdateBy(SecurityUtils.getUserId().toString());
        collection.setUpdateTime(DateUtils.getNowDate());

        //4. 新增或更新评估数据采集内容
        healthAssessmentDataCollectionService.saveOrUpdate(collection);

        //5. 返回评估记录ID
        return assessment.getId();
    }

    /**
     * 从OSS下载体检报告PDF并提取全部文本内容
     *
     * @param dto 健康评估收集信息
     * @return 体检报告文本内容
     */
    private String readMedicalReportText(ElderAssessmentDto dto) {
        String medicalReportUrl = dto.getHealthAssessmentDto().getRecent30Days().getMedicalReport();
        if (StrUtil.isBlank(medicalReportUrl)) {
            throw new ServiceException("请先上传体检报告后再进行AI评估");
        }

        String pdfContent;
        try (InputStream inputStream = ossAliyunFileStorageService.download(medicalReportUrl)) {
            pdfContent = PDFUtil.pdfToString(inputStream);
        } catch (Exception e) {
            log.error("体检报告下载失败，url={}", medicalReportUrl, e);
            throw new ServiceException("体检报告下载失败，请确认文件地址是否正确");
        }

        //解析失败或内容为空时无法进行AI分析
        if (StrUtil.isBlank(pdfContent)) {
            log.error("体检报告内容解析为空，url={}", medicalReportUrl);
            throw new ServiceException("体检报告内容解析失败，请重新上传PDF文件");
        }
        return pdfContent;
    }

    /**
     * 根据健康评分获取建议护理等级
     * 90分以上三级护理、80分以上二级护理、70分以上一级护理、60分以上特级护理、低于60分不建议入住
     *
     * @param healthScore 健康评分
     * @return 建议护理等级
     */
    private String getCareLevel(Double healthScore) {
        if (healthScore >= 90) {
            return "三级护理";
        } else if (healthScore >= 80) {
            return "二级护理";
        } else if (healthScore >= 70) {
            return "一级护理";
        } else if (healthScore >= MIN_CHECK_IN_SCORE) {
            return "特级护理";
        }
        return "不建议入住";
    }

    /**
     * 与大模型交互并按JSON解析返回结果
     *
     * @param prompt 提示词
     * @param type   解析目标类型
     * @return 解析结果
     */
    private <T> T chatBigModel(String prompt, Class<T> type) {
        String content;
        try {
            content = chatClientDeepseekHealth.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("调用大模型失败", e);
            throw new ServiceException("AI分析失败", AI_ASSESS_FAIL_CODE);
        }

        if (StrUtil.isBlank(content)) {
            log.error("大模型返回内容为空");
            throw new ServiceException("AI分析失败", AI_ASSESS_FAIL_CODE);
        }

        //剔除多余文字，只取JSON对象；解析失败统一按"AI分析失败"返回给前端
        String json = extractJsonObject(content);
        try {
            T result = JSONUtil.toBean(json, type);
            if (result == null) {
                throw new ServiceException("AI分析失败", AI_ASSESS_FAIL_CODE);
            }
            return result;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析大模型返回结果失败，content={}", content, e);
            throw new ServiceException("AI分析失败", AI_ASSESS_FAIL_CODE);
        }
    }

    /**
     * 从大模型返回内容中提取第一个完整的JSON对象
     * 兼容"纯JSON"、"带markdown代码块"以及"JSON前后夹杂说明文字"三种情况，
     * 通过括号层级与字符串状态判断，避免嵌套对象或字符串中的括号导致截取错误
     *
     * @param content 大模型返回内容
     * @return JSON字符串
     */
    private String extractJsonObject(String content) {
        int start = content.indexOf('{');
        if (start < 0) {
            log.error("大模型返回内容中未找到JSON对象，content={}", content);
            throw new ServiceException("AI分析失败", AI_ASSESS_FAIL_CODE);
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                //括号配平，说明找到了一个完整的JSON对象
                if (depth == 0) {
                    return content.substring(start, i + 1);
                }
            }
        }

        log.error("大模型返回内容中的JSON对象不完整，content={}", content);
        throw new ServiceException("AI分析失败", AI_ASSESS_FAIL_CODE);
    }

    /**
     * 校验第一阶段AI返回的能力等级结果
     *
     * @param levelResult AI返回结果
     */
    private void validateLevelResult(ElderLevelResultVO levelResult) {
        if (levelResult == null
                || !ABILITY_LEVELS.contains(normalizeAbilityLevel(levelResult.getPreLevel()))
                || !ABILITY_LEVELS.contains(normalizeAbilityLevel(levelResult.getFinalLevel()))) {
            log.error("AI返回的能力等级不合法，preLevel={}，finalLevel={}",
                    levelResult == null ? null : levelResult.getPreLevel(),
                    levelResult == null ? null : levelResult.getFinalLevel());
            throw new ServiceException("AI分析失败", AI_ASSESS_FAIL_CODE);
        }
    }

    /**
     * 统一能力等级表述：大模型可能返回"轻度受损"等写法，统一转换为"轻度失能"
     *
     * @param abilityLevel 能力等级
     * @return 统一后的能力等级
     */
    private String normalizeAbilityLevel(String abilityLevel) {
        return StrUtil.isBlank(abilityLevel) ? null : abilityLevel.trim().replace("受损", "失能");
    }

    /**
     * 校验第二阶段AI返回的体检分析结果，健康评分必须在0~100之间
     *
     * @param healthResult AI返回结果
     * @return 健康评分
     */
    private Double validateHealthResult(HealthReportResultVO healthResult) {
        Double healthScore = healthResult == null ? null : healthResult.getHealthScore();
        if (healthScore == null || healthScore < 0 || healthScore > 100) {
            log.error("AI返回的健康评分不合法：{}", healthScore);
            throw new ServiceException("AI分析失败", AI_ASSESS_FAIL_CODE);
        }
        //健康评分为0、八大系统全为0且没有任何异常项，说明模型没有识别出体检内容（例如上传的不是体检报告）
        if (isUnrecognizedHealthReport(healthResult)) {
            log.error("AI未能识别出体检报告内容，healthScore={}", healthScore);
            throw new ServiceException("体检报告内容无法识别，请上传真实的体检报告PDF", AI_ASSESS_FAIL_CODE);
        }
        //风险等级缺失时做兜底，保证报告字段有值
        if (StrUtil.isBlank(healthResult.getRiskLevel())) {
            healthResult.setRiskLevel("提示");
        }
        return healthScore;
    }

    /**
     * 判断AI是否未能识别出体检报告内容：
     * 健康评分为0、没有异常数据、并且八大系统分值全部为0（或缺失）时视为未识别
     */
    private boolean isUnrecognizedHealthReport(HealthReportResultVO healthResult) {
        if (CollUtil.isNotEmpty(healthResult.getAbnormalData())) {
            return false;
        }
        if (healthResult.getHealthScore() != null && healthResult.getHealthScore() > 0) {
            return false;
        }
        HealthReportResultVO.SystemScore systemScore = healthResult.getSystemScore();
        if (systemScore == null) {
            return true;
        }
        return isZeroScore(systemScore.getBreathingSystem())
                && isZeroScore(systemScore.getDigestiveSystem())
                && isZeroScore(systemScore.getEndocrineSystem())
                && isZeroScore(systemScore.getImmuneSystem())
                && isZeroScore(systemScore.getCirculatorySystem())
                && isZeroScore(systemScore.getUrinarySystem())
                && isZeroScore(systemScore.getMotionSystem())
                && isZeroScore(systemScore.getSenseSystem());
    }

    /**
     * 系统分值是否为0（缺失按0处理）
     */
    private boolean isZeroScore(Integer score) {
        return score == null || score == 0;
    }

    /**
     * 根据等级中文名称获取等级数值
     * 能力完好-0、轻度受损-1、中度受损-2、重度受损-3
     *
     * @param abilityRating 能力等级中文名称
     * @param moduleName    模块名称（用于异常提示）
     * @return 等级数值
     */
    private String getCurrentLevel(String abilityRating, String moduleName) {
        if (StrUtil.isBlank(abilityRating)) {
            throw new ServiceException(moduleName + "能力等级不能为空");
        }
        return switch (abilityRating.trim()) {
            case "能力完好" -> "0";
            case "轻度受损" -> "1";
            case "中度受损" -> "2";
            case "重度受损" -> "3";
            default -> throw new ServiceException(moduleName + "能力等级不合法：" + abilityRating);
        };
    }

    /**
     * 意外事件次数为空时按0次处理
     */
    private Integer defaultCount(Integer count) {
        return count == null ? 0 : count;
    }

    /**
     * 判断是否患有痴呆/精神疾病：为"无"或空表示没有，其余情况（如"轻度"、"有"）表示有
     */
    private Integer hasDisease(String diagnose) {
        return StrUtil.isBlank(diagnose) || "无".equals(diagnose.trim()) ? 0 : 1;
    }

    /**
     * 判断是否确诊认知障碍：画钟测验结果2表示确诊认知障碍
     */
    private Integer isCognitiveImpairment(Integer clockDrawingResult) {
        return clockDrawingResult != null && clockDrawingResult == 2 ? 1 : 0;
    }

    /**
     * 依据最终能力等级推荐房型：中度/重度失能优先单人间，其余优先双人间
     * 房型名称取自房型表中已启用的房型，匹配不到时返回空
     *
     * @param finalAbilityLevel 最终能力等级
     * @return 推荐房型名称
     */
    private String recommendRoomType(String finalAbilityLevel) {
        List<RoomType> roomTypes = roomTypeService.findRoomTypeListByStatus(1);
        if (CollUtil.isEmpty(roomTypes)) {
            return null;
        }

        String keyword = "中度失能".equals(finalAbilityLevel) || "重度失能".equals(finalAbilityLevel)
                ? "单人间" : "双人间";
        return roomTypes.stream()
                .map(RoomType::getName)
                .filter(StrUtil::isNotBlank)
                .filter(name -> name.contains(keyword))
                .findFirst()
                //没有匹配到目标房型时，退回房型表中的第一个可用房型
                .orElseGet(() -> roomTypes.stream()
                        .map(RoomType::getName)
                        .filter(StrUtil::isNotBlank)
                        .findFirst()
                        .orElse(null));
    }

    /**
     * 护理重点：按四大维度等级与近30天意外事件归纳
     */
    private String buildCareFocus(String dailyActivityLevel, String mentalStatusLevel,
                                  String perceptionCommunicationLevel, String socialParticipationLevel,
                                  Integer fall, Integer choking, Integer suicideAttempt,
                                  Integer lost, Integer coma) {
        List<String> careFocus = new ArrayList<>();
        if (!"0".equals(dailyActivityLevel)) {
            careFocus.add("日常生活活动协助");
        }
        if (!"0".equals(mentalStatusLevel)) {
            careFocus.add("精神状态监测与认知干预");
        }
        if (!"0".equals(perceptionCommunicationLevel)) {
            careFocus.add("感知觉与沟通协助");
        }
        if (!"0".equals(socialParticipationLevel)) {
            careFocus.add("社会参与支持");
        }
        if (fall > 0) {
            careFocus.add("跌倒防范");
        }
        if (choking > 0) {
            careFocus.add("噎食防范");
        }
        if (suicideAttempt > 0) {
            careFocus.add("情绪危机干预");
        }
        if (lost > 0) {
            careFocus.add("走失防范");
        }
        if (coma > 0) {
            careFocus.add("昏迷照护");
        }
        if (careFocus.isEmpty()) {
            careFocus.add("常规健康监测");
        }
        return String.join("；", careFocus);
    }

    /**
     * 建议说明：能力等级变化 + 健康评分 + 风险等级 + 入住建议
     */
    private String buildSuggestionDescription(String initialAbilityLevel, String finalAbilityLevel,
                                              Double healthScore, String riskLevel, Integer coreSuggestion) {
        return String.format("老人初始能力等级为%s，结合近30天风险因素最终评定为%s；体检健康评分%.2f分，风险等级为%s，%s。",
                initialAbilityLevel, finalAbilityLevel, healthScore, riskLevel,
                coreSuggestion == 1 ? "建议入住并制定个性化照护计划" : "暂不建议入住，建议先就医复查并加强居家照护");
    }

    /**
     * 家属配合事项：基础配合 + 针对近30天风险事件的配合要求
     */
    private String buildFamilyCooperation(String finalAbilityLevel, Integer fall, Integer choking,
                                          Integer lost, Integer suicideAttempt) {
        List<String> items = new ArrayList<>();
        items.add("如实提供老人既往病史与近期用药情况");
        if (fall > 0) {
            items.add("协助排查居所跌倒风险并做好防滑防护");
        }
        if (choking > 0) {
            items.add("配合调整饮食形态，进食时安排专人看护");
        }
        if (lost > 0) {
            items.add("加强外出陪伴，必要时佩戴定位设备");
        }
        if (suicideAttempt > 0) {
            items.add("关注情绪变化，配合心理疏导与日常看护");
        }
        if ("中度失能".equals(finalAbilityLevel) || "重度失能".equals(finalAbilityLevel)) {
            items.add("参与照护方案沟通，定期探望陪伴");
        }
        return String.join("；", items);
    }

    /**
     * 饮食建议：按体检报告中八大系统分值给出针对性建议
     */
    private String buildDietSuggestion(HealthReportResultVO.SystemScore systemScore) {
        List<String> items = new ArrayList<>();
        items.add("均衡膳食，少量多餐");
        if (systemScore != null) {
            if (systemScoreValue(systemScore.getCirculatorySystem()) < 80) {
                items.add("低盐低脂饮食，控制腌制及油炸食品");
            }
            if (systemScoreValue(systemScore.getDigestiveSystem()) < 80) {
                items.add("选择易消化食物，细嚼慢咽");
            }
            if (systemScoreValue(systemScore.getEndocrineSystem()) < 80) {
                items.add("控制精制糖与主食摄入量，规律监测血糖");
            }
            if (systemScoreValue(systemScore.getUrinarySystem()) < 80) {
                items.add("适量饮水，减少高嘌呤食物摄入");
            }
            if (systemScoreValue(systemScore.getImmuneSystem()) < 80) {
                items.add("增加优质蛋白与新鲜蔬果摄入");
            }
            if (systemScoreValue(systemScore.getMotionSystem()) < 80) {
                items.add("补充钙与维生素D，适度日照");
            }
        }
        return String.join("；", items);
    }

    /**
     * 系统分值缺失时按满分处理，避免误判为异常
     */
    private int systemScoreValue(Integer score) {
        return score == null ? 100 : score;
    }

    /**
     * 心理关怀建议：按精神状态等级与是否确诊认知障碍给出
     */
    private String buildPsychologicalCare(String mentalStatusLevel, Integer knownCognitiveImpairment) {
        List<String> items = new ArrayList<>();
        items.add("每日不少于10分钟的一对一沟通，关注情绪变化");
        if (!"0".equals(mentalStatusLevel)) {
            items.add("开展认知训练与怀旧疗法，减少焦虑和激越行为");
        }
        if (knownCognitiveImpairment != null && knownCognitiveImpairment == 1) {
            items.add("按认知障碍照护规范进行环境提示与定向训练");
        }
        return String.join("；", items);
    }

    /**
     * 机构准备事项：按最终能力等级准备照护环境与人员
     */
    private String buildInstitutionPreparation(String finalAbilityLevel) {
        return switch (finalAbilityLevel) {
            case "重度失能" -> "配置护理床、防压疮垫与移位辅具；安排24小时专人照护；房间去除门槛并加装扶手；建立翻身与皮肤护理计划";
            case "中度失能" -> "配置助行器与防滑设施；安排定时巡护；房间加装夜灯与扶手；建立跌倒与噎食应急预案";
            case "轻度失能" -> "房间加装防滑垫与扶手；提供助行辅具；安排每日健康巡查";
            default -> "按普通入住标准准备房间与生活用品；建立健康档案并定期体检";
        };
    }

    /**
     * 组装两阶段AI分析结果，写入评估数据采集的"详细评估报告内容"
     */
    private String buildAssessmentDetails(String initialAbilityLevel, String finalAbilityLevel, String levelChangeReason,
                                          Double healthScore, HealthReportResultVO healthResult) {
        JSONObject details = JSONUtil.createObj()
                .set("initialAbilityLevel", initialAbilityLevel)
                .set("finalAbilityLevel", finalAbilityLevel)
                .set("levelChangeReason", StrUtil.nullToEmpty(levelChangeReason))
                .set("healthScore", healthScore)
                .set("riskLevel", healthResult.getRiskLevel())
                .set("reportSummary", StrUtil.nullToEmpty(healthResult.getSummarize()))
                .set("abnormalData", healthResult.getAbnormalData() == null ? List.of() : healthResult.getAbnormalData())
                .set("systemScore", healthResult.getSystemScore());
        return details.toString();
    }
}
