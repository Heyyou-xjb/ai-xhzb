package com.xhzb.nursing.service.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.xhzb.nursing.domain.vo.MessageVO;
import com.xhzb.nursing.service.ChatHistoryService;
import com.xhzb.nursing.service.IAiChatSessionService;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会话历史处理业务实现类
 * 会话ID与消息都保存在MySQL（ai_chat_session、ai_chat_message表），不再使用Redis
 *
 * @Author mr.wu
 * @Date 2026-8-14 9:50
 */
@Service
public class ChatHistoryServiceImpl implements ChatHistoryService {

    @Autowired
    private IAiChatSessionService aiChatSessionService;

    /** 会话记忆（当前为MySQL实现 MysqlChatMemory） */
    @Autowired
    private ChatMemory chatMemory;

    /**
     * 保存用户的会话ID
     *
     * @param userId 用户ID（登录用户ID的字符串形式）
     * @param chatId 会话ID
     */
    @Override
    public void saveHistory(String userId, String chatId) {
        aiChatSessionService.saveSession(Convert.toLong(userId), chatId);
    }

    /**
     * 获取历史会话ID
     *
     * @param userId 用户ID
     * @return 会话ID集合，最新创建的排在最前面
     */
    @Override
    public List<String> findChatIdList(String userId) {
        return aiChatSessionService.listChatIdsByUser(Convert.toLong(userId));
    }

    /**
     * 获取聊天记录详情
     *
     * @param chatId 会话ID
     * @return 用户提问与AI回复
     */
    @Override
    public List<MessageVO> findChatHistoryDetails(String chatId) {
        //从会话记忆（MySQL）中读取该会话的历史消息
        List<Message> messageList = chatMemory.get(chatId);
        if (CollUtil.isEmpty(messageList)) {
            return List.of();
        }

        //只返回用户与AI的真实对话内容：过滤掉系统提示等非对话消息
        return messageList.stream()
                .filter(message -> message.getMessageType() == MessageType.USER
                        || message.getMessageType() == MessageType.ASSISTANT)//只保留用户消息与AI回复
                .filter(message -> StrUtil.isNotBlank(message.getText()))//过滤内容为空的中间消息
                .map(MessageVO::new) //将SpringAI消息转为MessageVO
                .toList();
    }

    /**
     * 删除会话及聊天信息
     *
     * @param chatId 会话ID
     * @param userId 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChatHistory(String chatId, String userId) {
        //删除用户对应的会话记录
        aiChatSessionService.deleteSession(Convert.toLong(userId), chatId);

        //删除会话ID对应的全部消息
        chatMemory.clear(chatId);
    }
}
