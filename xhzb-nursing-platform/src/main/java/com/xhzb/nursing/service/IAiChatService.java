package com.xhzb.nursing.service;

import reactor.core.publisher.Flux;

/**
 * AI智能客服Service接口
 * 负责流式对话的输出与打断
 *
 * @author xujingbo
 * @date 2026-10-16
 */
public interface IAiChatService
{
    /**
     * 发起流式对话
     *
     * @param userId 用户ID
     * @param chatId 会话ID
     * @param prompt 用户提问
     * @return 逐段返回的AI回复内容
     */
    Flux<String> streamChat(String userId, String chatId, String prompt);

    /**
     * 打断指定会话正在进行的流式输出（已生成的内容保留）
     *
     * @param userId 用户ID
     * @param chatId 会话ID
     * @return true表示确实打断了一个正在进行的流式对话
     */
    boolean stopStream(String userId, String chatId);
}
