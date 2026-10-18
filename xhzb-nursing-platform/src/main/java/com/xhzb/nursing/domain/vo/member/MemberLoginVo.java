package com.xhzb.nursing.domain.vo.member;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 家属小程序登录结果
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Data
@Schema(description = "家属小程序登录结果")
public class MemberLoginVo {

    /** 登录令牌 */
    @Schema(title = "登录令牌")
    private String token;

    /** 家属昵称 */
    @Schema(title = "家属昵称")
    private String nickName;
}
