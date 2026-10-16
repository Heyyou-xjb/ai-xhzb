package com.xhzb.nursing.tool;

import com.xhzb.nursing.domain.NursingProject;
import com.xhzb.nursing.service.INursingProjectService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NursingProjectTool {

    @Autowired
    private INursingProjectService nursingProjectService;

    /**
     * 查询系统内已启用的护理项目，返回名称、单位、价格、护理要求等数据库中的真实内容，
     * 供大模型回答用户关于护理项目及其收费、护理要求的咨询。
     *
     * @return 已启用的护理项目集合
     */
    @Tool(name = "find_project_tool", description = "查询系统内所有已启用的护理项目，返回每个项目的名称、单位、价格和护理要求")
    public List<NursingProject> findAllProjects() {
        return nursingProjectService.listEnabledProjects();
    }


}
