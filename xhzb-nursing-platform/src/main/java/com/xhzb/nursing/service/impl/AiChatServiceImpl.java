package com.xhzb.nursing.service.impl;

import com.xhzb.nursing.service.IAiChatMessageService;
import com.xhzb.nursing.service.IAiChatService;
import com.xhzb.nursing.stream.AiChatStreamManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

/**
 * AI智能客服业务层处理：流式对话输出与打断
 *
 * @author xujingbo
 * @date 2026-10-16
 */
@Service
public class AiChatServiceImpl implements IAiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    /** 智能客服会话客户端：带会话记忆(MessageChatMemoryAdvisor)与知识库RAG(QuestionAnswerAdvisor) */
    @Autowired
    @Qualifier("deepseekChatClient")
    private ChatClient chatClient;

    @Autowired
    private AiChatStreamManager aiChatStreamManager;

    @Autowired
    private IAiChatMessageService aiChatMessageService;

    /**
     * 发起流式对话
     * 1. 登记打断信号：调用stopStream后，本轮输出立即结束；
     * 2. 逐段收集已生成内容：会话记忆顾问只在流"正常结束"时才保存AI回复，
     *    被打断或客户端断开时不会保存，因此这里在流结束时补存已生成的内容，保证已生成的消息不丢失。
     *
     * @param userId 用户ID
     * @param chatId 会话ID
     * @param prompt 用户提问
     * @return 逐段返回的AI回复内容
     */
    @Override
    public Flux<String> streamChat(String userId, String chatId, String prompt) {
        return Flux.defer(() -> {
            StringBuilder generatedContent = new StringBuilder();

            return chatClient.prompt()
                    .user(prompt)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId)) //每次对话时传递唯一对话ID
                    .stream()//持续输出效果
                    .content()
                    .doOnNext(generatedContent::append)//收集本次已生成的内容
                    .takeUntilOther(aiChatStreamManager.register(chatId, userId))//收到打断信号立即结束输出
                    .doFinally(signalType -> {
                        boolean stopped = aiChatStreamManager.release(chatId);
                        //被打断（点了停止按钮）或客户端断开连接时，补存已生成的内容
                        if (stopped || signalType == SignalType.CANCEL) {
                            aiChatMessageService.saveInterruptedReply(chatId, generatedContent.toString());
                        }
                    });
        });
    }

    /**
     * 打断指定会话正在进行的流式输出
     *
     * @param userId 用户ID
     * @param chatId 会话ID
     * @return true表示确实打断了一个正在进行的流式对话
     */
    @Override
    public boolean stopStream(String userId, String chatId) {
        boolean stopped = aiChatStreamManager.stop(chatId, userId);
        log.info("打断流式对话，chatId={}，userId={}，是否命中进行中的对话={}", chatId, userId, stopped);
        return stopped;
    }
}
