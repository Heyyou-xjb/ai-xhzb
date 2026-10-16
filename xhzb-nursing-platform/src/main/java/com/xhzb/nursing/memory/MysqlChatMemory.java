package com.xhzb.nursing.memory;

import java.util.List;

import com.xhzb.nursing.service.IAiChatMessageService;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 基于MySQL的会话记忆处理类
 * 会话消息持久化到 ai_chat_message 表，按会话ID(chat_id)读写
 *
 * @author ruoyi
 * @date 2026-10-15
 */
@Component
public class MysqlChatMemory implements ChatMemory {

    @Autowired
    private IAiChatMessageService aiChatMessageService;

    /**
     * 保存会话消息
     *
     * @param conversationId 会话ID
     * @param messages Spring AI消息集合
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        aiChatMessageService.saveMessages(conversationId, messages);
    }

    /**
     * 读取会话消息（按消息产生的先后顺序返回）
     *
     * @param conversationId 会话ID
     */
    @Override
    public List<Message> get(String conversationId) {
        return aiChatMessageService.listMessages(conversationId);
    }

    /**
     * 清空会话消息
     *
     * @param conversationId 会话ID
     */
    @Override
    public void clear(String conversationId) {
        aiChatMessageService.deleteMessagesByChatId(conversationId);
    }
}
