package com.xhzb.nursing.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.nursing.domain.Bed;
import com.xhzb.nursing.domain.CheckIn;
import com.xhzb.nursing.domain.CheckInConfig;
import com.xhzb.nursing.domain.Contract;
import com.xhzb.nursing.domain.Elder;
import com.xhzb.nursing.domain.HealthAssessment;
import com.xhzb.nursing.domain.HealthAssessmentReport;
import com.xhzb.nursing.domain.dto.checkin.CheckInApplyDto;
import com.xhzb.nursing.domain.dto.checkin.CheckInConfigDto;
import com.xhzb.nursing.domain.dto.checkin.CheckInContractDto;
import com.xhzb.nursing.domain.dto.checkin.CheckInElderDto;
import com.xhzb.nursing.domain.dto.checkin.ElderFamilyDto;
import com.xhzb.nursing.domain.vo.checkin.CheckInConfigVo;
import com.xhzb.nursing.domain.vo.checkin.CheckInDetailVo;
import com.xhzb.nursing.domain.vo.checkin.CheckInElderVo;
import com.xhzb.nursing.domain.vo.checkin.ElderFamilyVo;
import com.xhzb.nursing.mapper.CheckInMapper;
import com.xhzb.nursing.service.IBedService;
import com.xhzb.nursing.service.ICheckInConfigService;
import com.xhzb.nursing.service.ICheckInService;
import com.xhzb.nursing.service.IContractService;
import com.xhzb.nursing.service.IElderService;
import com.xhzb.nursing.service.IHealthAssessmentReportService;
import com.xhzb.nursing.service.IHealthAssessmentService;
import com.xhzb.nursing.util.IDCardUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 入住管理Service业务层处理
 * 包含入住办理（申请入住）与入住详情查询
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Service
public class CheckInServiceImpl extends ServiceImpl<CheckInMapper, CheckIn> implements ICheckInService
{
    private static final Logger log = LoggerFactory.getLogger(CheckInServiceImpl.class);

    /** 合同编号前缀 */
    private static final String CONTRACT_NUMBER_PREFIX = "HT";

    /** 入住备注字段长度上限（check_in.remark 为 varchar(255)，用于保存家属信息JSON） */
    private static final int REMARK_MAX_LENGTH = 255;

    /** 入住状态：0-已入住 */
    private static final int STATUS_CHECKED_IN = 0;

    @Autowired
    private CheckInMapper checkInMapper;

    @Autowired
    private IBedService bedService;

    @Autowired
    private IElderService elderService;

    @Autowired
    private IContractService contractService;

    @Autowired
    private ICheckInConfigService checkInConfigService;

    @Autowired
    private IHealthAssessmentService healthAssessmentService;

    @Autowired
    private IHealthAssessmentReportService healthAssessmentReportService;

    /**
     * 查询入住管理
     *
     * @param id 入住管理主键
     * @return 入住管理
     */
    @Override
    public CheckIn selectCheckInById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询入住管理列表
     *
     * @param checkIn 入住管理
     * @return 入住管理集合
     */
    @Override
    public List<CheckIn> selectCheckInList(CheckIn checkIn)
    {
        return checkInMapper.selectCheckInList(checkIn);
    }

    /**
     * 新增入住管理
     *
     * @param checkIn 入住管理
     * @return 结果
     */
    @Override
    public int insertCheckIn(CheckIn checkIn)
    {
        return save(checkIn) ? 1 : 0;
    }

    /**
     * 修改入住管理
     *
     * @param checkIn 入住管理
     * @return 结果
     */
    @Override
    public int updateCheckIn(CheckIn checkIn)
    {
        return updateById(checkIn) ? 1 : 0;
    }

    /**
     * 批量删除入住管理
     *
     * @param ids 需要删除的入住管理主键
     * @return 结果
     */
    @Override
    public int deleteCheckInByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除入住管理信息
     *
     * @param id 入住管理主键
     * @return 结果
     */
    @Override
    public int deleteCheckInById(Long id)
    {
        return removeById(id) ? 1 : 0;
    }

    /**
     * 申请入住
     * 业务顺序：校验评估是否完成 → 校验老人是否已入住 → 校验床位是否已入住
     *          → 更新床位状态 → 新增或更新老人 → 新增签约合同 → 新增入住记录
     *          → 新增入住配置 → 关联评估记录 → 更新评估结果为已入住
     *
     * @param dto 入住申请参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyCheckIn(CheckInApplyDto dto) {
        //0. 基础参数校验
        validateApplyParams(dto);
        CheckInElderDto elderDto = dto.getCheckInElderDto();
        CheckInConfigDto configDto = dto.getCheckInConfigDto();

        //1. 判断评估是否完成：评估进度为1（已完成）才允许入住
        HealthAssessment healthAssessment = healthAssessmentService.getById(dto.getHealthAssessmentId());
        if (healthAssessment == null) {
            throw new ServiceException("健康评估记录不存在");
        }
        if (healthAssessment.getEvaluationProgress() == null || healthAssessment.getEvaluationProgress() != 1) {
            throw new ServiceException("未完成评估不能入住");
        }

        //2. 校验老人是否已入住：入住表中存在该身份证号且状态为已入住的记录
        Long livingCount = count(Wrappers.<CheckIn>lambdaQuery()
                .eq(CheckIn::getIdCardNo, elderDto.getIdCardNo())
                .eq(CheckIn::getStatus, STATUS_CHECKED_IN));
        if (livingCount != null && livingCount > 0) {
            throw new ServiceException("老人已入住");
        }

        //3. 判断床位是否已被其他老人入住
        Bed bed = bedService.getById(configDto.getBedId());
        if (bed == null) {
            throw new ServiceException("床位不存在");
        }
        if (bed.getBedStatus() != null && bed.getBedStatus() == 1) {
            throw new ServiceException("有其他老人已入住");
        }

        //4. 更新床位状态为已入住
        Bed bedUpdate = new Bed();
        bedUpdate.setId(bed.getId());
        bedUpdate.setBedStatus(1);
        bedUpdate.setUpdateTime(DateUtils.getNowDate());
        bedUpdate.setUpdateBy(SecurityUtils.getUserId().toString());
        bedService.updateById(bedUpdate);

        //5. 新增或更新老人信息：该老人以前入住过（老人表中已存在）时更新
        Elder elder = elderService.getOne(Wrappers.<Elder>lambdaQuery()
                .eq(Elder::getName, elderDto.getName())
                .eq(Elder::getIdCardNo, elderDto.getIdCardNo())
                .last("limit 1"));
        Elder elderToSave = buildElder(elderDto, bed);
        if (elder == null) {
            elderToSave.setCreateBy(SecurityUtils.getUserId().toString());
            elderToSave.setCreateTime(DateUtils.getNowDate());
            elderService.save(elderToSave);
        } else {
            elderToSave.setId(elder.getId());
            elderToSave.setUpdateBy(SecurityUtils.getUserId().toString());
            elderToSave.setUpdateTime(DateUtils.getNowDate());
            elderService.updateById(elderToSave);
        }

        //6. 新增签约合同：合同号 HT + yyyyMMddHHmmss + 4位递增值，合同状态按签约日期判断
        Contract contract = buildContract(dto.getCheckInContractDto(), elderToSave, configDto);
        contractService.save(contract);

        //7. 新增入住记录：家属信息转为JSON保存到备注字段
        CheckIn checkIn = buildCheckIn(elderToSave, bed, configDto, dto.getElderFamilyDtoList());
        save(checkIn);

        //8. 新增入住配置
        CheckInConfig checkInConfig = buildCheckInConfig(checkIn.getId(), configDto);
        checkInConfigService.save(checkInConfig);

        //9. 修改评估记录：关联老人，并标记为已入住
        HealthAssessment assessmentUpdate = new HealthAssessment();
        assessmentUpdate.setId(dto.getHealthAssessmentId());
        assessmentUpdate.setElderId(elderToSave.getId());
        assessmentUpdate.setCheckInStatus(1);
        assessmentUpdate.setUpdateBy(SecurityUtils.getUserId().toString());
        assessmentUpdate.setUpdateTime(DateUtils.getNowDate());
        healthAssessmentService.updateById(assessmentUpdate);

        //10. 修改评估结果（报告）：入住状态改为已入住
        healthAssessmentReportService.lambdaUpdate()
                .set(HealthAssessmentReport::getCheckInStatus, 1)
                .eq(HealthAssessmentReport::getHealthAssessmentId, dto.getHealthAssessmentId())
                .update();

        log.info("入住办理成功，评估ID={}，老人ID={}，入住ID={}", dto.getHealthAssessmentId(), elderToSave.getId(), checkIn.getId());
    }

    /**
     * 查询入住详情（老人信息、家属信息、入住配置与签约合同）
     *
     * @param id 入住记录ID
     * @return 入住详情
     */
    @Override
    public CheckInDetailVo selectCheckInDetail(Long id) {
        CheckIn checkIn = getById(id);
        if (checkIn == null) {
            throw new ServiceException("入住记录不存在");
        }

        CheckInDetailVo detail = new CheckInDetailVo();

        //1. 老人信息：优先取老人表数据，老人表缺失时用入住记录中的姓名与身份证号兜底
        Elder elder = checkIn.getElderId() == null ? null : elderService.getById(checkIn.getElderId());
        detail.setCheckInElderVo(buildElderVo(elder, checkIn));

        //2. 家属信息：入住记录的备注中保存的是家属信息JSON
        detail.setElderFamilyVoList(parseFamilyList(checkIn.getRemark()));

        //3. 入住配置（包含入住时间与床位）
        detail.setCheckInConfigVo(buildCheckInConfigVo(checkIn));

        //4. 签约合同：取该老人最新的一份合同
        if (checkIn.getElderId() != null) {
            Contract contract = contractService.getOne(Wrappers.<Contract>lambdaQuery()
                    .eq(Contract::getElderId, checkIn.getElderId())
                    .orderByDesc(Contract::getId)
                    .last("limit 1"));
            detail.setContract(contract);
        }
        return detail;
    }

    /**
     * 校验入住申请的基础参数，避免后续流程出现空指针
     */
    private void validateApplyParams(CheckInApplyDto dto) {
        if (dto == null) {
            throw new ServiceException("入住申请参数不能为空");
        }
        if (dto.getHealthAssessmentId() == null) {
            throw new ServiceException("健康评估ID不能为空");
        }
        CheckInElderDto elderDto = dto.getCheckInElderDto();
        if (elderDto == null || StrUtil.isBlank(elderDto.getName()) || StrUtil.isBlank(elderDto.getIdCardNo())) {
            throw new ServiceException("老人姓名和身份证号不能为空");
        }
        CheckInConfigDto configDto = dto.getCheckInConfigDto();
        if (configDto == null || configDto.getBedId() == null) {
            throw new ServiceException("请选择入住床位");
        }
        if (configDto.getStartDate() == null) {
            throw new ServiceException("请选择入住开始日期");
        }
        if (dto.getCheckInContractDto() == null) {
            throw new ServiceException("签约合同信息不能为空");
        }
    }

    /**
     * 封装老人信息：入住后状态为已入住，并关联床位
     */
    private Elder buildElder(CheckInElderDto dto, Bed bed) {
        Elder elder = new Elder();
        elder.setName(dto.getName());
        elder.setIdCardNo(dto.getIdCardNo());
        elder.setSex(dto.getSex());
        elder.setPhone(dto.getPhone());
        elder.setBirthday(dto.getBirthday());
        elder.setAddress(dto.getAddress());
        elder.setImage(dto.getImage());
        elder.setIdCardNationalEmblemImg(dto.getIdCardNationalEmblemImg());
        elder.setIdCardPortraitImg(dto.getIdCardPortraitImg());
        elder.setNation(dto.getNation());
        elder.setEducationLevel(dto.getEducationLevel());
        elder.setSocialSecurityCard(dto.getSocialSecurityCard());
        elder.setLivingSituation(dto.getLivingSituation());
        elder.setReligiousBelief(dto.getReligiousBelief());
        elder.setEconomicSource(dto.getEconomicSource());
        elder.setMaritalStatus(dto.getMaritalStatus());
        elder.setMedicalPaymentMethod(dto.getMedicalPaymentMethod());
        elder.setCoreSuggestion(dto.getCoreSuggestion());
        //老人状态：1-已入住
        elder.setStatus(1L);
        elder.setBedId(bed.getId());
        elder.setBedNumber(bed.getBedNumber());
        return elder;
    }

    /**
     * 封装签约合同：合同编号按"HT+yyyyMMddHHmmss+4位递增值"生成，签约日期不晚于当前时间则为已生效
     */
    private Contract buildContract(CheckInContractDto dto, Elder elder, CheckInConfigDto configDto) {
        Date now = DateUtils.getNowDate();
        Contract contract = new Contract();
        contract.setElderId(elder.getId());
        contract.setElderName(elder.getName());
        contract.setContractName(StrUtil.isBlank(dto.getContractName())
                ? elder.getName() + "的入住合同" : dto.getContractName());
        contract.setContractNumber(generateContractNumber(now));
        contract.setAgreementPath(dto.getAgreementPath());
        contract.setThirdPartyName(dto.getThirdPartyName());
        contract.setThirdPartyPhone(dto.getThirdPartyPhone());
        contract.setStartDate(configDto.getStartDate());
        contract.setEndDate(configDto.getEndDate());
        //签约日期为空时按当前时间处理；签约日期早于或等于当前时间则合同已生效(1)，否则未生效(0)
        Date signDate = dto.getSignDate() == null ? now : dto.getSignDate();
        contract.setSignDate(signDate);
        contract.setStatus(signDate.getTime() <= now.getTime() ? 1L : 0L);
        contract.setSortOrder(0L);
        contract.setCreateBy(SecurityUtils.getUserId().toString());
        contract.setCreateTime(now);
        return contract;
    }

    /**
     * 生成合同编号：HT + yyyyMMddHHmmss + 4位递增值，例如 HT202408261544080001
     */
    private String generateContractNumber(Date now) {
        String prefix = CONTRACT_NUMBER_PREFIX + DateUtils.parseDateToStr("yyyyMMddHHmmss", now);
        //统计同一时间前缀下已有的合同数量，作为本次的递增值
        Long existCount = contractService.count(Wrappers.<Contract>lambdaQuery()
                .likeRight(Contract::getContractNumber, prefix));
        long sequence = (existCount == null ? 0L : existCount) + 1L;
        if (sequence > 9999L) {
            throw new ServiceException("合同编号生成失败，请稍后重试");
        }
        return prefix + String.format("%04d", sequence);
    }

    /**
     * 封装入住记录：家属信息转为JSON保存到备注字段
     */
    private CheckIn buildCheckIn(Elder elder, Bed bed, CheckInConfigDto configDto, List<ElderFamilyDto> familyList) {
        String familyJson = CollUtil.isEmpty(familyList) ? "[]" : JSONUtil.toJsonStr(familyList);
        //备注字段为varchar(255)，家属信息过多会超出长度限制
        if (familyJson.length() > REMARK_MAX_LENGTH) {
            throw new ServiceException("家属信息过多，无法保存，请精简后再提交");
        }

        CheckIn checkIn = new CheckIn();
        checkIn.setElderName(elder.getName());
        checkIn.setElderId(elder.getId());
        checkIn.setIdCardNo(elder.getIdCardNo());
        checkIn.setStartDate(configDto.getStartDate());
        checkIn.setEndDate(configDto.getEndDate());
        checkIn.setNursingLevelName(configDto.getNursingLevelName());
        //床位编号以床位表中的真实编号为准
        checkIn.setBedNumber(bed.getBedNumber());
        //0-已入住
        checkIn.setStatus(STATUS_CHECKED_IN);
        checkIn.setSortOrder(0L);
        checkIn.setRemark(familyJson);
        checkIn.setCreateBy(SecurityUtils.getUsername());
        checkIn.setCreateTime(DateUtils.getNowDate());
        return checkIn;
    }

    /**
     * 封装入住配置：费用字段为空时按0处理（数据库对应字段不允许为空）
     */
    private CheckInConfig buildCheckInConfig(Long checkInId, CheckInConfigDto dto) {
        CheckInConfig config = new CheckInConfig();
        config.setCheckInId(checkInId);
        config.setNursingLevelId(dto.getNursingLevelId());
        config.setNursingLevelName(dto.getNursingLevelName());
        //费用开始日期为空时按入住开始日期处理
        config.setFeeStartDate(dto.getFeeStartDate() == null ? dto.getStartDate() : dto.getFeeStartDate());
        config.setFeeEndDate(dto.getFeeEndDate());
        config.setDeposit(defaultZero(dto.getDeposit()));
        config.setNursingFee(defaultZero(dto.getNursingFee()));
        config.setBedFee(defaultZero(dto.getBedFee()));
        config.setInsurancePayment(defaultZero(dto.getInsurancePayment()));
        config.setGovernmentSubsidy(defaultZero(dto.getGovernmentSubsidy()));
        config.setOtherFees(defaultZero(dto.getOtherFees()));
        config.setSortOrder(0);
        config.setCreateBy(SecurityUtils.getUsername());
        config.setCreateTime(DateUtils.getNowDate());
        return config;
    }

    /**
     * 费用金额为空时返回0
     */
    private BigDecimal defaultZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    /**
     * 封装入住详情中的老人信息
     */
    private CheckInElderVo buildElderVo(Elder elder, CheckIn checkIn) {
        CheckInElderVo elderVo = new CheckInElderVo();
        if (elder == null) {
            //老人表缺失时用入住记录兜底，保证详情页可正常展示
            elderVo.setName(checkIn.getElderName());
            elderVo.setIdCardNo(checkIn.getIdCardNo());
            return elderVo;
        }
        elderVo.setId(elder.getId());
        elderVo.setName(elder.getName());
        elderVo.setIdCardNo(elder.getIdCardNo());
        elderVo.setBirthday(elder.getBirthday());
        elderVo.setSex(elder.getSex());
        elderVo.setPhone(elder.getPhone());
        elderVo.setAddress(elder.getAddress());
        elderVo.setImage(elder.getImage());
        elderVo.setIdCardNationalEmblemImg(elder.getIdCardNationalEmblemImg());
        elderVo.setIdCardPortraitImg(elder.getIdCardPortraitImg());
        elderVo.setNation(elder.getNation());
        elderVo.setEducationLevel(elder.getEducationLevel());
        elderVo.setSocialSecurityCard(elder.getSocialSecurityCard());
        elderVo.setLivingSituation(elder.getLivingSituation());
        elderVo.setReligiousBelief(elder.getReligiousBelief());
        elderVo.setEconomicSource(elder.getEconomicSource());
        elderVo.setMaritalStatus(elder.getMaritalStatus());
        elderVo.setMedicalPaymentMethod(elder.getMedicalPaymentMethod());
        elderVo.setCoreSuggestion(elder.getCoreSuggestion());
        elderVo.setAge(calculateAge(elder.getBirthday(), elder.getIdCardNo()));
        return elderVo;
    }

    /**
     * 计算老人年龄：优先按出生日期计算，出生日期缺失或格式不正确时按身份证号计算
     */
    private Integer calculateAge(String birthday, String idCardNo) {
        try {
            if (StrUtil.isNotBlank(birthday) && birthday.trim().length() >= 10) {
                LocalDate birthDate = LocalDate.parse(birthday.trim().substring(0, 10));
                return Period.between(birthDate, LocalDate.now()).getYears();
            }
            if (StrUtil.isNotBlank(idCardNo) && idCardNo.length() == 18) {
                return IDCardUtils.getAgeByIdCard(idCardNo);
            }
        } catch (Exception e) {
            log.warn("计算老人年龄失败，birthday={}，idCardNo={}", birthday, idCardNo);
        }
        return null;
    }

    /**
     * 解析入住记录备注中的家属信息JSON
     */
    private List<ElderFamilyVo> parseFamilyList(String remark) {
        if (StrUtil.isBlank(remark)) {
            return List.of();
        }
        try {
            return JSONUtil.toList(JSONUtil.parseArray(remark), ElderFamilyVo.class);
        } catch (Exception e) {
            log.warn("解析入住备注中的家属信息失败，remark={}", remark);
            return List.of();
        }
    }

    /**
     * 封装入住详情中的入住配置（入住时间与床位取自入住记录）
     */
    private CheckInConfigVo buildCheckInConfigVo(CheckIn checkIn) {
        CheckInConfig config = checkInConfigService.getOne(Wrappers.<CheckInConfig>lambdaQuery()
                .eq(CheckInConfig::getCheckInId, checkIn.getId())
                .orderByDesc(CheckInConfig::getId)
                .last("limit 1"));
        if (config == null) {
            return null;
        }

        CheckInConfigVo configVo = new CheckInConfigVo();
        configVo.setId(config.getId());
        configVo.setCheckInId(config.getCheckInId());
        configVo.setNursingLevelId(config.getNursingLevelId());
        configVo.setNursingLevelName(config.getNursingLevelName());
        configVo.setFeeStartDate(config.getFeeStartDate());
        configVo.setFeeEndDate(config.getFeeEndDate());
        configVo.setDeposit(config.getDeposit());
        configVo.setNursingFee(config.getNursingFee());
        configVo.setBedFee(config.getBedFee());
        configVo.setInsurancePayment(config.getInsurancePayment());
        configVo.setGovernmentSubsidy(config.getGovernmentSubsidy());
        configVo.setOtherFees(config.getOtherFees());
        configVo.setSortOrder(config.getSortOrder());
        configVo.setStartDate(checkIn.getStartDate());
        configVo.setEndDate(checkIn.getEndDate());
        configVo.setBedNumber(checkIn.getBedNumber());
        configVo.setCreateBy(config.getCreateBy());
        configVo.setCreateTime(config.getCreateTime());
        configVo.setUpdateTime(config.getUpdateTime());
        return configVo;
    }
}
