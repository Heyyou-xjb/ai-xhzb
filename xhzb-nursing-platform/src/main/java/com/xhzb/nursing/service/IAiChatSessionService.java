package com.xhzb.nursing.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.AiChatSession;

/**
 * AI会话Service接口
 *
 * @author xujingbo
 * @date 2026-10-15
 */
public interface IAiChatSessionService extends IService<AiChatSession>
{
    /**
     * 保存用户的会话（同一用户同一会话只保存一次）
     *
     * @param userId 用户ID
     * @param chatId 会话ID
     */
    void saveSession(Long userId, String chatId);

    /**
     * 查询用户的历史会话ID，最新创建的排在最前面
     *
     * @param userId 用户ID
     * @return 会话ID集合
     */
    List<String> listChatIdsByUser(Long userId);

    /**
     * 删除用户的指定会话
     *
     * @param userId 用户ID
     * @param chatId 会话ID
     */
    void deleteSession(Long userId, String chatId);
}
