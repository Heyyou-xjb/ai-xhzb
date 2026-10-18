package com.xhzb.nursing.controller.member;

import java.util.List;

import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.core.page.TableDataInfo;
import com.xhzb.nursing.domain.NursingProject;
import com.xhzb.nursing.service.INursingProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 家属小程序-服务项Controller
 *
 * @author xujingbo
 * @date 2026-10-18
 */
@RestController
@RequestMapping("/member/orders/project")
@Tag(name = "家属小程序-服务项")
public class MemberProjectController extends BaseController {

    @Autowired
    private INursingProjectService nursingProjectService;

    /**
     * 分页查询护理项目列表（服务页）
     *
     * @param nursingProject 查询条件：name-名称，status-状态
     * @return 分页后的护理项目列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询护理项目列表")
    public AjaxResult page(NursingProject nursingProject) {
        startPage();
        List<NursingProject> list = nursingProjectService.listForMember(nursingProject);
        TableDataInfo tableDataInfo = getDataTable(list);
        return success(tableDataInfo);
    }

    /**
     * 根据编号查询护理项目信息（服务详情页）
     *
     * @param id 护理项目主键
     * @return 护理项目详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据编号查询护理项目信息")
    public AjaxResult getInfo(@Schema(name = "护理项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
                              @PathVariable Long id) {
        return success(nursingProjectService.getMemberProjectDetail(id));
    }
}
