package com.linkwechat.wecom.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 社区运营 - 群SOP DTO
 */
@Data
public class WeGroupSopDto {
    /**
     * 规则名
     */
    @NotNull(message = "规则名不可为空")
    private String ruleName;

    /**
     * 群聊id列表
     */
    @NotNull(message = "未选择群聊")
    private List<String> chatIdList;

    /**
     * 内容标题
     */
    @NotNull(message = "标题不可为空")
    private String title;

    /**
     * 消息内容
     */
    @NotNull(message = "内容不可为空")
    private String content;

    /**
     * 素材列表
     */
    private List<Long> materialIdList;

    /**
     * 上传的图片的URL列表
     */
    private List<String> picList;

    /**
     * 开始执行时间
     */
    @NotNull(message = "开始执行时间为空")
    private String startExeTime;

    /**
     * 结束执行时间
     */
    @NotNull(message = "结束时间为空")
    private String stopExeTime;
}
