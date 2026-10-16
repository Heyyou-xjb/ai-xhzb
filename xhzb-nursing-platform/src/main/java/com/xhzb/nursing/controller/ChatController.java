package com.xhzb.nursing.controller;

import cn.hutool.core.util.StrUtil;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.nursing.domain.vo.MessageVO;
import com.xhzb.nursing.service.ChatHistoryService;
import com.xhzb.nursing.service.IAiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

import static com.xhzb.common.core.domain.AjaxResult.success;

@RestController
@RequestMapping("/ai")
public class ChatController {

    @Autowired
    private IAiChatService aiChatService;

    @Autowired
    private ChatHistoryService chatHistoryService;

    /**
     * 智能问答接口：以流式方式返回 DeepSeek 回答，支持多轮会话记忆与知识库 RAG。
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

        String userId = SecurityUtils.getUserId().toString();

        // 保存当前用户与会话的交互历史记录
        chatHistoryService.saveHistory(userId, chatId);

        return aiChatService.streamChat(userId, chatId, prompt);
    }

    /**
     * 打断接口：停止正在进行的流式输出
     * 只结束模型的输出，已经生成的内容不会清除，并会补存到会话记录中
     *
     * @param chatId 会话唯一标识
     * @return 是否打断了一个正在进行的流式对话
     */
    @PostMapping("/chat/stop")
    public AjaxResult stopChat(String chatId) {

        // 未携带会话 ID 时无法定位要打断的对话
        if(StrUtil.isBlank(chatId)) {
            throw new BaseException("会话ID不能为空");
        }

        return success(aiChatService.stopStream(SecurityUtils.getUserId().toString(), chatId));
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
