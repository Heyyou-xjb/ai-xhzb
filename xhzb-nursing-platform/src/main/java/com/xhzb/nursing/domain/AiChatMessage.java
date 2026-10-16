package com.xhzb.nursing.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI会话消息对象 ai_chat_message
 * 保存智能客服的会话消息，同时作为大模型的会话记忆（按chat_id读取历史消息）
 *
 * @author ruoyi
 * @date 2026-10-15
 */
@Data
@Schema(description = "AI会话消息对象")
public class AiChatMessage implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键（自增，同时作为消息的先后顺序） */
    @Schema(title = "主键")
    private Long id;

    /** 会话ID（与ai_chat_session.chat_id对应） */
    @Schema(title = "会话ID")
    private String chatId;

    /** 消息角色（USER：用户提问，ASSISTANT：AI回复，SYSTEM：系统提示） */
    @Schema(title = "消息角色")
    private String role;

    /** 消息内容 */
    @Schema(title = "消息内容")
    private String content;

    /** 创建时间 */
    @Schema(title = "创建时间")
    private Date createTime;
}
