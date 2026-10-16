package com.xhzb.nursing.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.nursing.domain.AiChatMessage;
import com.xhzb.nursing.mapper.AiChatMessageMapper;
import com.xhzb.nursing.service.IAiChatMessageService;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI会话消息Service业务层处理
 * 同时承担大模型会话记忆的读写（Spring AI中的ChatMemory）
 *
 * @author ruoyi
 * @date 2026-10-15
 */
@Service
public class AiChatMessageServiceImpl extends ServiceImpl<AiChatMessageMapper, AiChatMessage> implements IAiChatMessageService
{
    /**
     * 保存会话消息
     * 只保存用户提问、AI回复、系统提示三类消息；工具调用(TOOL)消息和内容为空的中间消息
     * 对会话记忆没有意义，直接丢弃，避免污染大模型上下文
     *
     * @param chatId 会话ID
     * @param messages Spring AI消息集合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMessages(String chatId, List<Message> messages) {
        if (StrUtil.isBlank(chatId) || CollUtil.isEmpty(messages)) {
            return;
        }

        List<AiChatMessage> messageEntities = messages.stream()
                .filter(this::isPersistableMessage) //过滤工具调用等不需要落库的消息
                .map(message -> toEntity(chatId, message))
                .toList();

        if (CollUtil.isNotEmpty(messageEntities)) {
            saveBatch(messageEntities);
        }
    }

    /**
     * 按自增主键升序读取，即消息产生的先后顺序
     *
     * @param chatId 会话ID
     * @return Spring AI消息集合
     */
    @Override
    public List<Message> listMessages(String chatId) {
        if (StrUtil.isBlank(chatId)) {
            return List.of();
        }

        List<AiChatMessage> messageEntities = list(Wrappers.<AiChatMessage>lambdaQuery()
                .eq(AiChatMessage::getChatId, chatId)
                .orderByAsc(AiChatMessage::getId));

        return messageEntities.stream()
                .map(this::toMessage)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 删除会话下的全部消息
     *
     * @param chatId 会话ID
     */
    @Override
    public void deleteMessagesByChatId(String chatId) {
        if (StrUtil.isBlank(chatId)) {
            return;
        }

        remove(Wrappers.<AiChatMessage>lambdaQuery().eq(AiChatMessage::getChatId, chatId));
    }

    /**
     * 判断消息是否需要落库：只保留用户提问、AI回复、系统提示，且内容不能为空
     */
    private boolean isPersistableMessage(Message message) {
        if (message == null || StrUtil.isBlank(message.getText())) {
            return false;
        }

        MessageType messageType = message.getMessageType();
        return messageType == MessageType.USER
                || messageType == MessageType.ASSISTANT
                || messageType == MessageType.SYSTEM;
    }

    /**
     * Spring AI消息转换为数据库实体
     */
    private AiChatMessage toEntity(String chatId, Message message) {
        AiChatMessage messageEntity = new AiChatMessage();
        messageEntity.setChatId(chatId);
        //角色用枚举名称入库：USER/ASSISTANT/SYSTEM
        messageEntity.setRole(message.getMessageType().name());
        messageEntity.setContent(message.getText());
        messageEntity.setCreateTime(new Date());
        return messageEntity;
    }

    /**
     * 数据库实体还原为Spring AI消息，用于大模型会话记忆
     */
    private Message toMessage(AiChatMessage messageEntity) {
        MessageType messageType;
        try {
            messageType = MessageType.valueOf(messageEntity.getRole());
        } catch (IllegalArgumentException e) {
            //库中角色异常（如历史脏数据）时跳过该条消息，不影响其余会话记忆
            return null;
        }

        return switch (messageType) {
            case USER -> new UserMessage(messageEntity.getContent());
            case ASSISTANT -> new AssistantMessage(messageEntity.getContent());
            case SYSTEM -> new SystemMessage(messageEntity.getContent());
            default -> null;
        };
    }
}
