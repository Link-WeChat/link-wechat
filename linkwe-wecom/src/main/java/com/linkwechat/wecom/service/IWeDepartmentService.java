package com.linkwechat.wecom.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linkwechat.wecom.domain.WeDepartment;
import org.apache.ibatis.annotations.Param;

/**
 * 企业微信组织架构相关Service接口
 * 
 * @author ruoyi
 * @date 2020-09-01
 */
public interface IWeDepartmentService extends IService<WeDepartment>
{
    /**
     * 查询企业微信组织架构相关
     * 
     * @param id 企业微信组织架构相关ID
     * @return 企业微信组织架构相关
     */
    public WeDepartment selectWeDepartmentById(Long id);

    /**
     * 查询企业微信组织架构相关列表
     * 
     * @return 企业微信组织架构相关集合
     */
    public List<WeDepartment> selectWeDepartmentList();

    /**
     * 新增企业微信组织架构相关
     * 
     * @param weDepartment 企业微信组织架构相关
     * @return 结果
     */
    public void insertWeDepartment(WeDepartment weDepartment);

    /**
     * 修改企业微信组织架构相关
     * 
     * @param weDepartment 企业微信组织架构相关
     * @return 结果
     */
    public void updateWeDepartment(WeDepartment weDepartment);



    /**
     *  批量保存
     * @param weDepartments
     * @return
     */
    public int batchInsertWeDepartment(List<WeDepartment> weDepartments);


    /**
     * 删除部门表所有数据
     * @return
     */
    public int deleteAllWeDepartment();


    /**
     * 同步部门
     */
    public List<WeDepartment> synchWeDepartment();
}
