package com.xhzb.nursing.controller;

import cn.hutool.core.util.StrUtil;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.nursing.domain.vo.MessageVO;
import com.xhzb.nursing.service.ChatHistoryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

import static com.xhzb.common.core.domain.AjaxResult.success;

@RestController
@RequestMapping("/ai")
public class ChatController {

    /**
     * 智能客服会话客户端：必须使用带会话记忆(MessageChatMemoryAdvisor)和知识库RAG(QuestionAnswerAdvisor)的
     * deepseekChatClient，这里用@Qualifier显式指定，避免与同名的简易Bean混淆。
     */
    @Autowired
    @Qualifier("deepseekChatClient")
    private ChatClient chatClient;

    @Autowired
    private ChatHistoryService chatHistoryService;

    /**
     * 智能问答接口：以流式（SSE）方式返回 DeepSeek 回答，支持多轮会话记忆与知识库 RAG。
     *
     * @param prompt 用户本次提问内容
     * @param chatId 会话唯一标识，用于关联会话记忆与历史记录
     * @return 逐字输出的回答内容流
     */
    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(String prompt,String chatId) {

        // 未携带合法会话 ID 时，提示用户先新建会话
        if(StrUtil.isBlank(chatId) || "null".equals(chatId.trim())) {
            throw new BaseException("请先新建会话");
        }

        // 保存当前用户与会话的交互历史记录
        chatHistoryService.saveHistory(SecurityUtils.getUserId().toString(), chatId);

        return chatClient.prompt()
                .user(prompt)
                .advisors(x-> x.param(ChatMemory.CONVERSATION_ID,chatId)) //每次对话时传递唯一对话ID
                .stream()// 持续输出效果
                .content();
    }

    /**
     * 获取聊天记录详情
     * @param chatId
     * @return
     */
    @GetMapping("/{chatId}")
    public AjaxResult findChatHistoryDetails(@PathVariable String chatId){
        List<MessageVO> messageVOList = chatHistoryService.findChatHistoryDetails(chatId);
        return success(messageVOList);
    }
}
