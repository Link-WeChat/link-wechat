package com.linkwechat.wecom.domain.dto;

import lombok.Data;

/**
 * @description: 通讯录用户
 * @author: HaoN
 * @create: 2020-08-28 10:45
 **/
@Data
public class WeUserDto {

    //成员头像的mediaid，通过素材管理接口上传图片获得的mediaid
    private String avatar_mediaid;

    //姓名
    private String name;
    //昵称
    private String alias;
    //账号
    private String userid;

    //性别。1表示男性，2表示女性
    private Integer gender;

    //手机号码。企业内必须唯一，mobile/email二者不能同时为空
    private String mobile;

    //邮箱
    private String email;

    //成员所属部门id列表,不超过100个
    private String[] department;

    //职位
    private String position;

    //身份:表示在所在的部门内是否为上级。1表示为上级，0表示非上级。
    private String[] is_leader_in_dept;

    //启用/禁用成员。1表示启用成员，0表示禁用成员
    private Integer enable;

    //座机。32字节以内，由纯数字或’-‘号组成。
    private String telephone;

    //地址
    private String address;



}
