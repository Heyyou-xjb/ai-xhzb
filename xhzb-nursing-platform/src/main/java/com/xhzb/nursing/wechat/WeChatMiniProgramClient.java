package com.xhzb.nursing.wechat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xhzb.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 微信小程序开放接口客户端
 * 负责调用微信官方接口：code2Session（登录凭证换openid）、getPhoneNumber（获取手机号）、access_token（接口调用凭证）
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@Component
public class WeChatMiniProgramClient {

    private static final Logger log = LoggerFactory.getLogger(WeChatMiniProgramClient.class);

    /** 登录凭证校验接口 */
    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    /** 稳定版access_token接口 */
    private static final String STABLE_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/stable_token";

    /** 获取手机号接口 */
    private static final String PHONE_NUMBER_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";

    /** access_token缓存key前缀 */
    private static final String ACCESS_TOKEN_CACHE_PREFIX = "wechat:mini_program:access_token:";

    /** access_token提前过期时间（秒），避免临界失效 */
    private static final long ACCESS_TOKEN_EXPIRE_AHEAD_SECONDS = 300L;

    /** 小程序AppID */
    @Value("${wechat.mini-program.appid}")
    private String appid;

    /** 小程序AppSecret */
    @Value("${wechat.mini-program.secret}")
    private String secret;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 登录凭证校验：使用前端传入的code换取openid
     *
     * @param code 小程序登录凭证
     * @return openid
     */
    public String getOpenId(String code) {
        Map<String, Object> params = new HashMap<>();
        params.put("appid", appid);
        params.put("secret", secret);
        params.put("js_code", code);
        params.put("grant_type", "authorization_code");

        String response;
        try {
            response = HttpUtil.get(CODE2SESSION_URL, params);
        } catch (Exception e) {
            log.error("调用微信code2Session接口失败", e);
            throw new ServiceException("微信登录服务调用失败，请稍后重试");
        }

        JSONObject json = parseResponse(response, "code2Session");
        Integer errcode = json.getInt("errcode");
        if (errcode != null && errcode != 0) {
            log.error("微信code2Session返回异常，errcode={}，errmsg={}", errcode, json.getStr("errmsg"));
            throw new ServiceException("微信登录失败：" + StrUtil.nullToEmpty(json.getStr("errmsg")));
        }
        String openId = json.getStr("openid");
        if (StrUtil.isBlank(openId)) {
            throw new ServiceException("微信登录失败：未获取到openid");
        }
        return openId;
    }

    /**
     * 获取手机号：使用前端手机号授权得到的phoneCode换取微信绑定的手机号
     *
     * @param phoneCode 手机号授权code
     * @return 手机号（不带国家码）
     */
    public String getPhoneNumber(String phoneCode) {
        String accessToken = getAccessToken();
        String url = PHONE_NUMBER_URL + "?access_token=" + accessToken;

        JSONObject requestBody = JSONUtil.createObj().set("code", phoneCode);
        String response;
        try {
            response = HttpUtil.createPost(url)
                    .body(requestBody.toString(), "application/json")
                    .execute()
                    .body();
        } catch (Exception e) {
            log.error("调用微信getPhoneNumber接口失败", e);
            throw new ServiceException("微信手机号获取失败，请稍后重试");
        }

        JSONObject json = parseResponse(response, "getPhoneNumber");
        Integer errcode = json.getInt("errcode");
        if (errcode != null && errcode != 0) {
            log.error("微信getPhoneNumber返回异常，errcode={}，errmsg={}", errcode, json.getStr("errmsg"));
            throw new ServiceException("微信手机号获取失败：" + StrUtil.nullToEmpty(json.getStr("errmsg")));
        }
        JSONObject phoneInfo = json.getJSONObject("phone_info");
        if (phoneInfo == null) {
            throw new ServiceException("微信手机号获取失败：返回结果缺少手机号信息");
        }
        //优先取不带国家码的手机号
        String phone = phoneInfo.getStr("purePhoneNumber");
        if (StrUtil.isBlank(phone)) {
            phone = phoneInfo.getStr("phoneNumber");
        }
        if (StrUtil.isBlank(phone)) {
            throw new ServiceException("微信手机号获取失败：手机号为空");
        }
        return phone;
    }

    /**
     * 获取接口调用凭证access_token
     * 使用稳定版接口获取，并缓存到Redis，避免频繁调用触发频率限制
     *
     * @return access_token
     */
    private String getAccessToken() {
        String cacheKey = ACCESS_TOKEN_CACHE_PREFIX + appid;
        String cachedToken = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cachedToken)) {
            return cachedToken;
        }

        JSONObject requestBody = JSONUtil.createObj()
                .set("grant_type", "client_credential")
                .set("appid", appid)
                .set("secret", secret)
                .set("force_refresh", false);

        String response;
        try {
            response = HttpUtil.createPost(STABLE_TOKEN_URL)
                    .body(requestBody.toString(), "application/json")
                    .execute()
                    .body();
        } catch (Exception e) {
            log.error("调用微信stableToken接口失败", e);
            throw new ServiceException("微信服务调用失败，请稍后重试");
        }

        JSONObject json = parseResponse(response, "stableToken");
        Integer errcode = json.getInt("errcode");
        if (errcode != null && errcode != 0) {
            log.error("微信stableToken返回异常，errcode={}，errmsg={}", errcode, json.getStr("errmsg"));
            throw new ServiceException("微信服务调用失败：" + StrUtil.nullToEmpty(json.getStr("errmsg")));
        }
        String accessToken = json.getStr("access_token");
        if (StrUtil.isBlank(accessToken)) {
            throw new ServiceException("微信服务调用失败：未获取到access_token");
        }

        //缓存access_token，过期时间比微信返回的有效期提前5分钟
        long expiresIn = json.getLong("expires_in", 7200L);
        long cacheSeconds = Math.max(expiresIn - ACCESS_TOKEN_EXPIRE_AHEAD_SECONDS, 60L);
        stringRedisTemplate.opsForValue().set(cacheKey, accessToken, cacheSeconds, TimeUnit.SECONDS);
        return accessToken;
    }

    /**
     * 校验并解析微信接口返回内容
     *
     * @param response 接口返回内容
     * @param apiName  接口名称（用于日志）
     * @return 解析后的JSON对象
     */
    private JSONObject parseResponse(String response, String apiName) {
        if (StrUtil.isBlank(response)) {
            log.error("微信{}接口返回内容为空", apiName);
            throw new ServiceException("微信服务调用失败：返回内容为空");
        }
        try {
            return JSONUtil.parseObj(response);
        } catch (Exception e) {
            log.error("解析微信{}接口返回内容失败，response={}", apiName, response, e);
            throw new ServiceException("微信服务调用失败：返回内容解析失败");
        }
    }
}
