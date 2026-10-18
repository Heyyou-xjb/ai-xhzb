package com.xhzb.nursing.service.impl;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.framework.web.service.TokenService;
import com.xhzb.nursing.domain.dto.member.MemberLoginDto;
import com.xhzb.nursing.domain.vo.member.MemberLoginVo;
import com.xhzb.nursing.wechat.WeChatMiniProgramClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xhzb.nursing.mapper.FamilyMemberMapper;
import com.xhzb.nursing.domain.FamilyMember;
import com.xhzb.nursing.service.IFamilyMemberService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 老人家属Service业务层处理
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Service
public class FamilyMemberServiceImpl extends ServiceImpl<FamilyMemberMapper, FamilyMember> implements IFamilyMemberService {

    @Autowired
    private FamilyMemberMapper familyMemberMapper;

    @Autowired
    private WeChatMiniProgramClient weChatMiniProgramClient;

    @Autowired
    private TokenService tokenService;

    /**
     * 查询老人家属
     *
     * @param id 老人家属主键
     * @return 老人家属
     */
    @Override
    public FamilyMember selectFamilyMemberById(Long id) {
        return getById(id);
    }

    /**
     * 查询老人家属列表
     *
     * @param familyMember 老人家属
     * @return 老人家属
     */
    @Override
    public List<FamilyMember> selectFamilyMemberList(FamilyMember familyMember) {
        return familyMemberMapper.selectFamilyMemberList(familyMember);
    }

    /**
     * 新增老人家属
     *
     * @param familyMember 老人家属
     * @return 结果
     */
    @Override
    public int insertFamilyMember(FamilyMember familyMember) {
        return save(familyMember) ? 1 : 0;
    }

    /**
     * 修改老人家属
     *
     * @param familyMember 老人家属
     * @return 结果
     */
    @Override
    public int updateFamilyMember(FamilyMember familyMember) {
        return updateById(familyMember) ? 1 : 0;
    }

    /**
     * 批量删除老人家属
     *
     * @param ids 需要删除的老人家属主键
     * @return 结果
     */
    @Override
    public int deleteFamilyMemberByIds(Long[] ids) {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除老人家属信息
     *
     * @param id 老人家属主键
     * @return 结果
     */
    @Override
    public int deleteFamilyMemberById(Long id) {
        return removeById(id) ? 1 : 0;
    }

    /**
     * 家属小程序登录
     * 1. 使用登录凭证code换取openid；
     * 2. 根据openid查询家属信息；
     * 3. 使用手机号授权code获取微信绑定的手机号；
     * 4. 家属不存在时新增（openid、手机号、随机昵称：四字前缀+手机号后四位）；
     * 5. 家属存在时，手机号与之前不一致则更新手机号；
     * 6. 使用家属ID与昵称生成登录令牌。
     *
     * @param dto 登录参数
     * @return 登录结果（token与昵称）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberLoginVo login(MemberLoginDto dto) {
        if (dto == null || StrUtil.isBlank(dto.getCode()) || StrUtil.isBlank(dto.getPhoneCode())) {
            throw new ServiceException("登录凭证code和手机号授权code不能为空");
        }

        //1. 登录凭证code换取openid
        String openId = weChatMiniProgramClient.getOpenId(dto.getCode());

        //2. 根据openid查询家属信息
        FamilyMember familyMember = getOne(Wrappers.<FamilyMember>lambdaQuery()
                .eq(FamilyMember::getOpenId, openId)
                .orderByDesc(FamilyMember::getId)
                .last("limit 1"));

        //3. 手机号授权code换取手机号
        String phone = weChatMiniProgramClient.getPhoneNumber(dto.getPhoneCode());

        if (familyMember == null) {
            //4. 家属不存在：新增家属，赋值openid、手机号与随机昵称
            familyMember = new FamilyMember();
            familyMember.setOpenId(openId);
            familyMember.setPhone(phone);
            familyMember.setName(buildRandomNickName(phone));
            familyMember.setCreateTime(DateUtils.getNowDate());
            familyMember.setUpdateTime(DateUtils.getNowDate());
            save(familyMember);
        } else {
            boolean needUpdate = false;
            //5. 手机号与之前不一致时更新手机号
            if (!phone.equals(familyMember.getPhone())) {
                familyMember.setPhone(phone);
                needUpdate = true;
            }
            //历史数据缺少昵称时补充随机昵称，保证返回给小程序端有可展示的昵称
            if (StrUtil.isBlank(familyMember.getName())) {
                familyMember.setName(buildRandomNickName(phone));
                needUpdate = true;
            }
            if (needUpdate) {
                familyMember.setUpdateTime(DateUtils.getNowDate());
                updateById(familyMember);
            }
        }

        //6. 使用家属ID与昵称生成token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", familyMember.getId());
        claims.put("nickName", familyMember.getName());
        //sub为JWT标准主题，便于小程序端解析昵称
        claims.put("sub", familyMember.getName());
        String token = tokenService.createToken(claims);

        MemberLoginVo loginVo = new MemberLoginVo();
        loginVo.setToken(token);
        loginVo.setNickName(familyMember.getName());
        return loginVo;
    }

    /**
     * 生成随机昵称：从预设的四字前缀中随机取一个，拼接手机号后四位
     *
     * @param phone 手机号
     * @return 随机昵称
     */
    private String buildRandomNickName(String phone) {
        //用户昵称前缀集合
        List<String> nickNameList = new ArrayList<>(Arrays.asList(
                "大吉大利", "今晚吃鸡", "福星高照", "好事发生",
                "匆匆忙忙", "连滚带爬", "从从容容", "游刃有余"));
        //打乱集合中全部元素的顺序，取第一个作为昵称前缀
        Collections.shuffle(nickNameList);
        String prefix = nickNameList.get(0);

        //后缀优先使用手机号后四位，手机号异常时用4位随机数字兜底
        String postfix = StrUtil.isBlank(phone)
                ? RandomStringUtils.randomNumeric(4)
                : phone.substring(Math.max(phone.length() - 4, 0));
        return prefix + postfix;
    }
}
