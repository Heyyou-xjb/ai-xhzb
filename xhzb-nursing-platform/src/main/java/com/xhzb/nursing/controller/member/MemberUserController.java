package com.xhzb.nursing.controller.member;

import com.xhzb.common.annotation.Anonymous;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.nursing.domain.dto.member.MemberLoginDto;
import com.xhzb.nursing.service.IFamilyMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 家属小程序用户Controller
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@RestController
@RequestMapping("/member/user")
@Tag(name = "家属小程序用户相关接口")
public class MemberUserController {

    @Autowired
    private IFamilyMemberService familyMemberService;

    /**
     * 微信小程序登录
     * 用登录凭证code换取openid、用手机号授权code获取手机号，完成家属注册/更新并返回登录令牌
     *
     * @param dto 登录参数
     * @return 登录结果（token与昵称）
     */
    @Anonymous
    @PostMapping("/login")
    @Operation(summary = "微信小程序登录")
    public AjaxResult login(@Valid @RequestBody MemberLoginDto dto) {
        return AjaxResult.success(familyMemberService.login(dto));
    }
}
