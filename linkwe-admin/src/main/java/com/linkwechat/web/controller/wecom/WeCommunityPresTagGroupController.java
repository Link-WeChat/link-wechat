package com.linkwechat.web.controller.wecom;


import com.linkwechat.common.constant.HttpStatus;
import com.linkwechat.common.core.controller.BaseController;
import com.linkwechat.common.core.domain.AjaxResult;
import com.linkwechat.common.core.page.TableDataInfo;
import com.linkwechat.wecom.domain.WeGroupCode;
import com.linkwechat.wecom.domain.WeTag;
import com.linkwechat.wecom.domain.dto.WePresTagGroupTaskDto;
import com.linkwechat.wecom.domain.vo.WePresTagGroupTaskStatVo;
import com.linkwechat.wecom.domain.vo.WePresTagGroupTaskVo;
import com.linkwechat.wecom.domain.vo.WeEmplVo;
import com.linkwechat.wecom.service.IWePresTagGroupTaskService;
import com.linkwechat.wecom.service.IWeGroupCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/wecom/communityPresTagGroup")
public class WeCommunityPresTagGroupController extends BaseController {

    @Autowired
    private IWePresTagGroupTaskService taskService;

    @Autowired
    private IWeGroupCodeService groupCodeService;

    /**
     * 获取老客标签建群列表数据
     */
    //  @PreAuthorize("@ss.hasPermi('wecom:communitytagGroup:list')")
    @GetMapping(path = "/list")
    public TableDataInfo<List<WePresTagGroupTaskVo>> getList(
            @RequestParam(value = "taskName", required = false) String taskName,
            @RequestParam(value = "sendType", required = false) Integer sendType,
            @RequestParam(value = "createBy", required = false) String createBy,
            @RequestParam(value = "beginTime", required = false) String beginTime,
            @RequestParam(value = "endTime", required = false) String endTime) {
        startPage();
        List<WePresTagGroupTaskVo> wePresTagGroupTaskVoList = taskService.selectTaskListList(taskName, sendType, createBy, beginTime, endTime);
        return getDataTable(wePresTagGroupTaskVoList);
    }

    /**
     * 新建老客标签建群任务
     */
    //   @PreAuthorize("@ss.hasPermi('wecom:communitytagGroup:add')")
    @PostMapping
    public AjaxResult addTask(@RequestBody @Validated WePresTagGroupTaskDto wePresTagGroupTaskDto) {
        // 检测任务名是否可用
        if (!taskService.checkTaskNameUnique(wePresTagGroupTaskDto.getTaskName())) {
            return AjaxResult.error("任务名已存在");
        }
        if (null == groupCodeService.selectWeGroupCodeById(wePresTagGroupTaskDto.getGroupCodeId())) {
            return AjaxResult.error(HttpStatus.NOT_FOUND,"群活码不存在");
        }
        return toAjax(taskService.add(wePresTagGroupTaskDto));
    }

    /**
     * 根据获取任务详细信息
     */
    //  @PreAuthorize("@ss.hasPermi('wecom:communitytagGroup:query')")
    @GetMapping(path = "/{id}")
    public AjaxResult getTask(@PathVariable("id") Long id) {
        WePresTagGroupTaskVo taskVo = taskService.getTaskById(id);
        // 获取标签和使用人员
        List<WeTag> tagList = taskService.getTagListByTaskId(id);
        List<WeEmplVo> emplVoList = taskService.getEmplListByTaskId(id);
        WeGroupCode weGroupCode = groupCodeService.selectWeGroupCodeById(taskVo.getGroupCodeId());
        taskVo.setGroupCodeInfo(weGroupCode);
        taskVo.setTagList(tagList);
        taskVo.setScopeList(emplVoList);
        return AjaxResult.success(taskVo);
    }

    /**
     * 更新任务信息
     */
    //    @PreAuthorize("@ss.hasPermi('wecom:communitytagGroup:edit')")
    @PutMapping(path = "/{id}")
    public AjaxResult updateTask(@PathVariable("id") Long id, @RequestBody @Validated WePresTagGroupTaskDto wePresTagGroupTaskDto) {
        // 检测任务名是否可用
        WePresTagGroupTaskVo original = taskService.getTaskById(id);
        // 判断名称是否发生变化
        boolean newName = wePresTagGroupTaskDto.getTaskName().equals(original.getTaskName());
        // 新名称不能已存在
        if (!taskService.checkTaskNameUnique(wePresTagGroupTaskDto.getTaskName()) && !newName) {
            return AjaxResult.error("任务名已存在");
        }
        if (null == groupCodeService.selectWeGroupCodeById(wePresTagGroupTaskDto.getGroupCodeId())) {
            return AjaxResult.error(HttpStatus.NOT_FOUND,"群活码不存在");
        }
        return toAjax(taskService.updateTask(id, wePresTagGroupTaskDto));
    }

    /**
     * 批量删除老客标签建群任务
     */
    //   @PreAuthorize("@ss.hasPermi('wecom:communitytagGroup:remove')")
    @DeleteMapping(path = "/{ids}")
    public AjaxResult batchRemoveTask(@PathVariable("ids") Long[] ids) {
        return toAjax(taskService.batchRemoveTaskByIds(ids));
    }

    /**
     * 根据老客标签建群id及过滤条件，获取其统计信息
     */
    //  @PreAuthorize("@ss.hasPermi('wecom:communitytagGroup:query')")
    @GetMapping(path = "/stat/{id}")
    public TableDataInfo<List<WePresTagGroupTaskStatVo>> getStatInfo(
            @PathVariable("id") Long id,
            @RequestParam(value = "customerName", required = false) String customerName,
            @RequestParam(value = "isInGroup", required = false) Integer isInGroup,
            @RequestParam(value = "isSent", required = false) Integer isSent
            ) {
        startPage();
        List<WePresTagGroupTaskStatVo> statVoList = taskService.getStatByTaskId(id, customerName, isInGroup, isSent);
        return getDataTable(statVoList);
    }

}
