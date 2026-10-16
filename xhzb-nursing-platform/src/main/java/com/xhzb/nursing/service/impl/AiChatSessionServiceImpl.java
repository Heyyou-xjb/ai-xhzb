package com.xhzb.nursing.service.impl;

import java.util.Date;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.nursing.domain.AiChatSession;
import com.xhzb.nursing.mapper.AiChatSessionMapper;
import com.xhzb.nursing.service.IAiChatSessionService;
import org.springframework.stereotype.Service;

/**
 * AI会话Service业务层处理
 *
 * @author ruoyi
 * @date 2026-10-15
 */
@Service
public class AiChatSessionServiceImpl extends ServiceImpl<AiChatSessionMapper, AiChatSession> implements IAiChatSessionService
{
    /**
     * 保存用户的会话，同一用户同一会话只保存一次
     *
     * @param userId 用户ID
     * @param chatId 会话ID
     */
    @Override
    public void saveSession(Long userId, String chatId) {
        if (userId == null || StrUtil.isBlank(chatId)) {
            return;
        }

        //先判断该会话是否已存在，避免同一次对话被重复记录
        Long count = count(Wrappers.<AiChatSession>lambdaQuery()
                .eq(AiChatSession::getUserId, userId)
                .eq(AiChatSession::getChatId, chatId));
        if (count != null && count > 0) {
            return;
        }

        AiChatSession aiChatSession = new AiChatSession();
        aiChatSession.setUserId(userId);
        aiChatSession.setChatId(chatId);
        aiChatSession.setCreateTime(new Date());
        save(aiChatSession);
    }

    /**
     * 查询用户的历史会话ID，最新创建的排在最前面
     *
     * @param userId 用户ID
     * @return 会话ID集合
     */
    @Override
    public List<String> listChatIdsByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }

        //最新创建的会话排在前面，前端默认选中第一条
        return list(Wrappers.<AiChatSession>lambdaQuery()
                .eq(AiChatSession::getUserId, userId)
                .orderByDesc(AiChatSession::getCreateTime)
                .orderByDesc(AiChatSession::getId))
                .stream()
                .map(AiChatSession::getChatId)
                .toList();
    }

    /**
     * 删除用户的指定会话
     *
     * @param userId 用户ID
     * @param chatId 会话ID
     */
    @Override
    public void deleteSession(Long userId, String chatId) {
        if (userId == null || StrUtil.isBlank(chatId)) {
            return;
        }

        remove(Wrappers.<AiChatSession>lambdaQuery()
                .eq(AiChatSession::getUserId, userId)
                .eq(AiChatSession::getChatId, chatId));
    }
}
