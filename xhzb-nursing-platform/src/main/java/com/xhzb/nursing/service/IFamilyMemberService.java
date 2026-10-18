package com.xhzb.nursing.service;

import java.util.List;
import com.xhzb.nursing.domain.FamilyMember;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.dto.member.MemberLoginDto;
import com.xhzb.nursing.domain.vo.member.MemberLoginVo;

/**
 * 老人家属Service接口
 * 
 * @author xujingbo
 * @date 2026-10-18
 */
public interface IFamilyMemberService extends IService<FamilyMember>
{
    /**
     * 查询老人家属
     * 
     * @param id 老人家属主键
     * @return 老人家属
     */
    public FamilyMember selectFamilyMemberById(Long id);

    /**
     * 查询老人家属列表
     * 
     * @param familyMember 老人家属
     * @return 老人家属集合
     */
    public List<FamilyMember> selectFamilyMemberList(FamilyMember familyMember);

    /**
     * 新增老人家属
     * 
     * @param familyMember 老人家属
     * @return 结果
     */
    public int insertFamilyMember(FamilyMember familyMember);

    /**
     * 修改老人家属
     * 
     * @param familyMember 老人家属
     * @return 结果
     */
    public int updateFamilyMember(FamilyMember familyMember);

    /**
     * 批量删除老人家属
     * 
     * @param ids 需要删除的老人家属主键集合
     * @return 结果
     */
    public int deleteFamilyMemberByIds(Long[] ids);

    /**
     * 删除老人家属信息
     * 
     * @param id 老人家属主键
     * @return 结果
     */
    public int deleteFamilyMemberById(Long id);

    /**
     * 家属小程序登录
     * 通过code换取openid并获取微信手机号，家属不存在则新增、手机号变化则更新，最后生成登录令牌
     *
     * @param dto 登录参数
     * @return 登录结果（token与昵称）
     */
    MemberLoginVo login(MemberLoginDto dto);
}
