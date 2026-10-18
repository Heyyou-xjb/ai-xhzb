package com.xhzb.nursing.domain.dto.member;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 家属小程序登录参数
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Data
@Schema(description = "家属小程序登录参数")
public class MemberLoginDto {

    /** 小程序登录凭证code（wx.login获取） */
    @NotBlank(message = "登录凭证code不能为空")
    @Schema(title = "小程序登录凭证code")
    private String code;

    /** 微信用户昵称（前端传入；新增家属时服务端使用随机昵称） */
    @Schema(title = "微信用户昵称")
    private String nickName;

    /** 获取手机号的授权code（getPhoneNumber获取） */
    @NotBlank(message = "手机号授权code不能为空")
    @Schema(title = "获取手机号的授权code")
    private String phoneCode;
}
