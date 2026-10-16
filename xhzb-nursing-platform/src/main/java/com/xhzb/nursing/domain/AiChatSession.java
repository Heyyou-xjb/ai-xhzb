package com.xhzb.nursing.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI会话对象 ai_chat_session
 * 记录每个用户在智能客服中创建过的会话，用于展示"历史会话"列表
 *
 * @author ruoyi
 * @date 2026-10-15
 */
@Data
@Schema(description = "AI会话对象")
public class AiChatSession implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Schema(title = "主键")
    private Long id;

    /** 用户ID（对应sys_user.user_id） */
    @Schema(title = "用户ID")
    private Long userId;

    /** 会话ID（前端生成的唯一标识，与ai_chat_message.chat_id对应） */
    @Schema(title = "会话ID")
    private String chatId;

    /** 创建时间 */
    @Schema(title = "创建时间")
    private Date createTime;
}
