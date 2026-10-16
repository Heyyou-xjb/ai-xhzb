package com.xhzb.nursing.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.AiChatMessage;
import org.springframework.ai.chat.messages.Message;

/**
 * AI会话消息Service接口
 * 同时承担大模型会话记忆的读写（Spring AI中的ChatMemory）
 *
 * @author ruoyi
 * @date 2026-10-15
 */
public interface IAiChatMessageService extends IService<AiChatMessage>
{
    /**
     * 保存会话消息（跳过工具调用等不参与会话记忆的消息，内容为空的消息也不落库）
     *
     * @param chatId 会话ID
     * @param messages Spring AI消息集合
     */
    void saveMessages(String chatId, List<Message> messages);

    /**
     * 按消息产生的先后顺序读取会话消息
     *
     * @param chatId 会话ID
     * @return Spring AI消息集合
     */
    List<Message> listMessages(String chatId);

    /**
     * 删除会话下的全部消息
     *
     * @param chatId 会话ID
     */
    void deleteMessagesByChatId(String chatId);

    /**
     * 保存被打断的AI回复（已生成的内容）
     * 会话记忆顾问只在流正常结束时保存AI回复，打断场景需要在此补存；
     * 若已存在相同内容的AI回复（例如停止请求与模型自然结束几乎同时发生），则不再重复入库
     *
     * @param chatId 会话ID
     * @param content 已生成的回复内容
     */
    void saveInterruptedReply(String chatId, String content);
}
