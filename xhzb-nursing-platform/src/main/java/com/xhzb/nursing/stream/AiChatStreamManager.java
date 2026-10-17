package com.xhzb.nursing.stream;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * 会话流式输出管理器
 * 登记正在进行的流式对话并提供"打断"信号：调用stop方法后，对应的流式输出会立即结束。
 * 注意：使用内存Map登记，适用于单实例部署。
 *
 * @author xujingbo
 * @date 2026-10-16
 */
@Component
public class AiChatStreamManager {

    /** 正在进行的流式对话，key为会话ID */
    private final Map<String, ChatStream> runningStreams = new ConcurrentHashMap<>();

    /**
     * 登记一个流式对话并返回打断信号，供Flux.takeUntilOther使用
     *
     * @param chatId 会话ID
     * @param userId 用户ID（用于限制只能打断自己的会话）
     * @return 打断信号，收到信号即结束流式输出
     */
    public Mono<Boolean> register(String chatId, String userId) {
        ChatStream chatStream = new ChatStream(userId);
        //同一会话重复发起时覆盖旧记录，旧流不再响应打断信号（前端已限制同一会话同时只能发起一次对话）
        runningStreams.put(chatId, chatStream);
        return chatStream.stopSignal();
    }

    /**
     * 打断指定会话的流式输出
     *
     * @param chatId 会话ID
     * @param userId 用户ID
     * @return true表示确实打断了一个属于自己的、正在进行的流式对话
     */
    public boolean stop(String chatId, String userId) {
        ChatStream chatStream = runningStreams.get(chatId);
        //没有正在进行的流式对话，或不是自己创建的会话，则不做任何处理
        if (chatStream == null || !chatStream.ownedBy(userId)) {
            return false;
        }
        return chatStream.stop();
    }

    /**
     * 流式输出结束时解除登记
     *
     * @param chatId 会话ID
     * @return true表示本次输出是被打断结束的
     */
    public boolean release(String chatId) {
        ChatStream chatStream = runningStreams.remove(chatId);
        return chatStream != null && chatStream.isStopped();
    }

    /**
     * 单个流式对话的状态：打断信号 + 会话归属 + 是否已被打断
     */
    private static class ChatStream {

        /** 打断信号，发出信号即结束流式输出 */
        private final Sinks.One<Boolean> stopSink = Sinks.one();

        /** 创建该流的用户ID */
        private final String userId;

        /** 是否已被打断 */
        private final AtomicBoolean stopped = new AtomicBoolean(false);

        private ChatStream(String userId) {
            this.userId = userId;
        }

        private Mono<Boolean> stopSignal() {
            return stopSink.asMono();
        }

        private boolean ownedBy(String otherUserId) {
            return userId != null && userId.equals(otherUserId);
        }

        private boolean stop() {
            stopped.set(true);
            return stopSink.tryEmitValue(Boolean.TRUE).isSuccess();
        }

        private boolean isStopped() {
            return stopped.get();
        }
    }
}
