package com.xhzb.nursing.util;

import cn.hutool.core.util.StrUtil;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.utils.ServletUtils;
import com.xhzb.framework.web.service.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 家属小程序登录信息解析工具
 * 小程序登录令牌由TokenService基于家属ID与昵称生成，这里从请求头中解析出家属ID
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Component
public class MemberTokenResolver {

    /** 令牌请求头 */
    private static final String TOKEN_HEADER = "Authorization";

    /** 令牌前缀 */
    private static final String TOKEN_PREFIX = "Bearer ";

    @Autowired
    private TokenService tokenService;

    /**
     * 获取当前登录的家属ID
     *
     * @return 家属ID
     */
    public Long getCurrentMemberId() {
        HttpServletRequest request = ServletUtils.getRequest();
        String token = request == null ? null : request.getHeader(TOKEN_HEADER);
        if (StrUtil.isBlank(token)) {
            throw new ServiceException("请先登录");
        }
        //兼容"Bearer xxx"与直接传令牌两种方式
        if (token.startsWith(TOKEN_PREFIX)) {
            token = token.substring(TOKEN_PREFIX.length());
        }

        Claims claims;
        try {
            claims = tokenService.parseToken(token);
        } catch (Exception e) {
            throw new ServiceException("登录状态已失效，请重新登录");
        }
        Object userId = claims.get("userId");
        if (userId == null || StrUtil.isBlank(userId.toString())) {
            throw new ServiceException("登录状态无效，请重新登录");
        }
        try {
            return Long.valueOf(userId.toString());
        } catch (NumberFormatException e) {
            throw new ServiceException("登录状态无效，请重新登录");
        }
    }
}
