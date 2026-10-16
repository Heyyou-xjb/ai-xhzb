package com.xhzb.nursing.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhzb.nursing.domain.AiChatMessage;

/**
 * AI会话消息Mapper接口
 *
 * @author ruoyi
 * @date 2026-10-15
 */
@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage>
{
}
