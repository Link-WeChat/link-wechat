package com.linkwechat.wecom.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.linkwechat.common.config.CosConfig;
import com.linkwechat.common.constant.WeConstans;
import com.linkwechat.common.enums.TaskFissionType;
import com.linkwechat.common.exception.wecom.WeComException;
import com.linkwechat.common.utils.DateUtils;
import com.linkwechat.common.utils.QREncode;
import com.linkwechat.common.utils.SecurityUtils;
import com.linkwechat.common.utils.StringUtils;
import com.linkwechat.common.utils.file.FileUploadUtils;
import com.linkwechat.common.utils.img.NetFileUtils;
import com.linkwechat.wecom.client.WeExternalContactClient;
import com.linkwechat.wecom.domain.*;
import com.linkwechat.wecom.domain.dto.WeChatUserDTO;
import com.linkwechat.wecom.domain.dto.WeExternalContactDto;
import com.linkwechat.wecom.domain.dto.WeTaskFissionPosterDTO;
import com.linkwechat.wecom.domain.dto.message.CustomerMessagePushDto;
import com.linkwechat.wecom.domain.dto.message.LinkMessageDto;
import com.linkwechat.wecom.domain.vo.WeTaskFissionDailyDataVO;
import com.linkwechat.wecom.domain.vo.WeTaskFissionProgressVO;
import com.linkwechat.wecom.domain.vo.WeTaskFissionStatisticVO;
import com.linkwechat.wecom.mapper.WeTaskFissionMapper;
import com.linkwechat.wecom.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ?????????Service???????????????
 *
 * @author leejoker
 * @date 2021-01-20
 */
@Slf4j
@Service
public class WeTaskFissionServiceImpl implements IWeTaskFissionService {
    @Autowired
    private WeTaskFissionMapper weTaskFissionMapper;
    @Autowired
    private IWeTaskFissionStaffService weTaskFissionStaffService;
    @Autowired
    private IWeCustomerMessagePushService weCustomerMessagePushService;
    @Autowired
    private IWeTaskFissionRecordService weTaskFissionRecordService;
    @Autowired
    private IWeUserService weUserService;
    @Autowired
    private WeExternalContactClient weExternalContactClient;
    @Autowired
    private IWePosterService wePosterService;
    @Autowired
    private IWeGroupCodeService weGroupCodeService;
    @Autowired
    private IWeMaterialService weMaterialService;
    @Autowired
    private IWeCustomerService weCustomerService;
    @Autowired
    private IWeFlowerCustomerRelService weFlowerCustomerRelService;
    @Autowired
    private IWeTaskFissionCompleteRecordService weTaskFissionCompleteRecordService;
    @Autowired
    private CosConfig cosConfig;
    @Value("${H5.url}")
    private String pageUrl;

    /**
     * ???????????????
     *
     * @param id ?????????ID
     * @return ?????????
     */
    @Override
    public WeTaskFission selectWeTaskFissionById(Long id) {
        WeTaskFission taskFission = weTaskFissionMapper.selectWeTaskFissionById(id);
        List<WeTaskFissionStaff> staffList = weTaskFissionStaffService.selectWeTaskFissionStaffByTaskId(id);
        taskFission.setTaskFissionStaffs(staffList);
        return taskFission;
    }

    /**
     * ?????????????????????
     *
     * @param weTaskFission ?????????
     * @return ?????????
     */
    @Override
    public List<WeTaskFission> selectWeTaskFissionList(WeTaskFission weTaskFission) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat sdf_day = new SimpleDateFormat("yyyy-MM-dd");
        if (weTaskFission.getStartTime() != null && weTaskFission.getOverTime() != null) {
            weTaskFission.setStartTime(sdf.parse(sdf_day.format(weTaskFission.getStartTime()) + " 00:00:00"));
            weTaskFission.setOverTime(sdf.parse(sdf_day.format(weTaskFission.getOverTime()) + " 23:59:59"));
        }
        return weTaskFissionMapper.selectWeTaskFissionList(weTaskFission);
    }

    /**
     * ???????????????
     *
     * @param weTaskFission ?????????
     * @return ??????
     */
    @Override
    @Transactional
    public Long insertWeTaskFission(WeTaskFission weTaskFission) {
        weTaskFission.setCreateBy(SecurityUtils.getUsername());
        weTaskFission.setCreateTime(DateUtils.getNowDate());
        groupQrcodeHandler(weTaskFission);
        int insertResult = weTaskFissionMapper.insertWeTaskFission(weTaskFission);
        if (insertResult > 0) {
            if (CollectionUtils.isNotEmpty(weTaskFission.getTaskFissionStaffs())) {
                if (weTaskFission.getId() != null) {
                    weTaskFission.getTaskFissionStaffs().forEach(staff -> {
                        staff.setTaskFissionId(weTaskFission.getId());
                    });
                    weTaskFissionStaffService.insertWeTaskFissionStaffList(weTaskFission.getTaskFissionStaffs());
                }
            }
        }
        return weTaskFission.getId();
    }

    /**
     * ???????????????
     *
     * @param weTaskFission ?????????
     * @return ??????
     */
    @Override
    @Transactional
    public Long updateWeTaskFission(WeTaskFission weTaskFission) {
        weTaskFission.setUpdateTime(DateUtils.getNowDate());
        weTaskFission.setUpdateBy(SecurityUtils.getUsername());
        groupQrcodeHandler(weTaskFission);
        int updateResult = weTaskFissionMapper.updateWeTaskFission(weTaskFission);
        if (updateResult > 0) {
            if (CollectionUtils.isNotEmpty(weTaskFission.getTaskFissionStaffs())) {
                List<WeTaskFissionStaff> staffList = weTaskFissionStaffService.selectWeTaskFissionStaffByTaskId(weTaskFission.getId());
                if (CollectionUtils.isNotEmpty(staffList)) {
                    weTaskFissionStaffService.deleteWeTaskFissionStaffByIds(staffList.stream().map(WeTaskFissionStaff::getId).toArray(Long[]::new));
                }
                weTaskFission.getTaskFissionStaffs().forEach(staff -> {
                    staff.setTaskFissionId(weTaskFission.getId());
                });
                weTaskFissionStaffService.insertWeTaskFissionStaffList(weTaskFission.getTaskFissionStaffs());
            }
        }
        return weTaskFission.getId();
    }

    /**
     * ?????????????????????
     *
     * @param ids ????????????????????????ID
     * @return ??????
     */
    @Override
    public int deleteWeTaskFissionByIds(Long[] ids) {
        return weTaskFissionMapper.deleteWeTaskFissionByIds(ids);
    }

    /**
     * ?????????????????????
     *
     * @param id ?????????ID
     * @return ??????
     */
    @Override
    public int deleteWeTaskFissionById(Long id) {
        return weTaskFissionMapper.deleteWeTaskFissionById(id);
    }


    @Override
    public void sendWeTaskFission(Long id) {
        WeTaskFission weTaskFission = selectWeTaskFissionById(id);
        //????????????
        String postersPath = weTaskFission.getPostersUrl();
        //????????????id
        String fissStaffId = weTaskFission.getFissionTargetId();
        //H5????????????????????????
        StringBuilder pageUrlBuilder = new StringBuilder(pageUrl);
        pageUrlBuilder.append("?")
                .append("agentId=").append("1000010")
                .append("&")
                .append("fissionId=").append(id)
                .append("&")
                .append("userId=").append(fissStaffId)
                .append("&")
                .append("posterId=").append(weTaskFission.getPostersId());

        LinkMessageDto linkMessageDto = new LinkMessageDto();
        linkMessageDto.setPicurl(postersPath);
        linkMessageDto.setDesc(weTaskFission.getFissInfo());
        linkMessageDto.setTitle(weTaskFission.getTaskName());
        linkMessageDto.setUrl(pageUrlBuilder.toString());

        CustomerMessagePushDto customerMessagePushDto = new CustomerMessagePushDto();
        customerMessagePushDto.setLinkMessage(linkMessageDto);
        customerMessagePushDto.setPushType("0");
        customerMessagePushDto.setPushRange("1");
        customerMessagePushDto.setMessageType("2");
        customerMessagePushDto.setTag(weTaskFission.getCustomerTagId());
        //??????????????????
        List<WeTaskFissionStaff> weTaskFissionStaffList = weTaskFissionStaffService.selectWeTaskFissionStaffByTaskId(id);
        if (CollectionUtil.isNotEmpty(weTaskFissionStaffList)) {
            //????????????id
            String departmentIds = weTaskFissionStaffList.stream().filter(weTaskFissionStaff ->
                    WeConstans.USE_SCOP_BUSINESSID_TYPE_ORG.equals(weTaskFissionStaff.getStaffType()))
                    .map(WeTaskFissionStaff::getStaffId).collect(Collectors.joining(","));
            //????????????id
            String userIds = weTaskFissionStaffList.stream().filter(weTaskFissionStaff ->
                    WeConstans.USE_SCOP_BUSINESSID_TYPE_USER.equals(weTaskFissionStaff.getStaffType()))
                    .map(WeTaskFissionStaff::getStaffId).collect(Collectors.joining(","));
            customerMessagePushDto.setStaffId(userIds);
            customerMessagePushDto.setDepartment(departmentIds);
        }
        try {
            weCustomerMessagePushService.addWeCustomerMessagePush(customerMessagePushDto);
        } catch (JsonProcessingException | ParseException e) {
            e.printStackTrace();
            log.error("???????????????????????????????????????????????????params:{},ex:{}", JSONObject.toJSONString(customerMessagePushDto), e);
        }


    }

    @Override
    @Transactional
    public String fissionPosterGenerate(WeTaskFissionPosterDTO weTaskFissionPosterDTO) {
        WeCustomer weCustomer = weCustomerService.getOne(new LambdaQueryWrapper<WeCustomer>()
                .eq(WeCustomer::getExternalUserid, weTaskFissionPosterDTO.getEid()));
        if (weCustomer != null) {
            //?????????????????????????????????
            WeTaskFissionRecord record = getTaskFissionRecordId(weTaskFissionPosterDTO.getTaskFissionId(), weCustomer.getExternalUserid(), weCustomer.getName());
            String posterUrl = record.getPoster();
            if (StringUtils.isBlank(posterUrl)) {
                String qrcode = getPosterQRCode(weTaskFissionPosterDTO.getFissionTargetId(), record, weCustomer);
                if (StringUtils.isBlank(qrcode)) {
                    throw new WeComException("????????????????????????");
                }
                WePoster poster = wePosterService.selectOne(weTaskFissionPosterDTO.getPosterId());
                poster.getPosterSubassemblyList().stream().filter(Objects::nonNull)
                        .filter(wePosterSubassembly -> wePosterSubassembly.getType() == 3).forEach(wePosterSubassembly -> {
                    wePosterSubassembly.setImgPath(qrcode);
                });
                posterUrl = wePosterService.generateSimpleImg(poster);
                if (StringUtils.isBlank(posterUrl)) {
                    throw new WeComException("?????????????????????");
                }
                record.setQrCode(qrcode);
                record.setPoster(posterUrl);
                weTaskFissionRecordService.updateWeTaskFissionRecord(record);
            }
            return posterUrl;
        } else {
            throw new WeComException("?????????????????????");
        }
    }

    @Override
    public void completeFissionRecord(Long taskFissionId, Long taskFissionRecordId, WeChatUserDTO weChatUserDTO) {
        WeTaskFissionCompleteRecord wfcr = new WeTaskFissionCompleteRecord();
        wfcr.setTaskFissionId(taskFissionId);
        wfcr.setFissionRecordId(taskFissionRecordId);
        String userId = StringUtils.isBlank(weChatUserDTO.getUserid()) ? weChatUserDTO.getUnionid() : weChatUserDTO.getUserid();
        wfcr.setCustomerId(userId);
        wfcr.setCustomerName(weChatUserDTO.getName());
        List<WeTaskFissionCompleteRecord> list = weTaskFissionCompleteRecordService.selectWeTaskFissionCompleteRecordList(wfcr);
        if (CollectionUtils.isEmpty(list)) {
            wfcr.setCreateTime(new Date());
            weTaskFissionCompleteRecordService.insertWeTaskFissionCompleteRecord(wfcr);
        }
    }

    @Override
    public List<WeCustomer> getCustomerListById(String eid, String fissionId) {
        WeTaskFissionRecord weTaskFissionRecord;
        if (StringUtils.isEmpty(eid)) {
            List<WeTaskFissionRecord> weTaskFissionRecords = weTaskFissionRecordService
                    .list(new LambdaQueryWrapper<WeTaskFissionRecord>().eq(WeTaskFissionRecord::getTaskFissionId, fissionId));
            return Optional.ofNullable(weTaskFissionRecords).orElseGet(ArrayList::new).stream()
                    .map(record -> weCustomerService.selectWeCustomerById(record.getCustomerId()))
                    .filter(Objects::nonNull).collect(Collectors.toList());
        } else {
            WeCustomer weCustomer = weCustomerService.getOne(new LambdaQueryWrapper<WeCustomer>().eq(WeCustomer::getExternalUserid, eid));
            String externalUseriId = Optional.ofNullable(weCustomer).map(WeCustomer::getExternalUserid)
                    .orElseThrow(() -> new WeComException("?????????????????????"));
            weTaskFissionRecord = weTaskFissionRecordService
                    .selectWeTaskFissionRecordByIdAndCustomerId(Long.valueOf(fissionId), externalUseriId);
            Optional.ofNullable(weTaskFissionRecord).map(WeTaskFissionRecord::getId)
                    .orElseThrow(() -> new WeComException("???????????????????????????"));
            List<WeFlowerCustomerRel> list = weFlowerCustomerRelService.list(new LambdaQueryWrapper<WeFlowerCustomerRel>()
                    .eq(WeFlowerCustomerRel::getState, WeConstans.FISSION_PREFIX + weTaskFissionRecord.getId()));
            List<String> eidList = Optional.ofNullable(list).orElseGet(ArrayList::new).stream()
                    .map(WeFlowerCustomerRel::getExternalUserid).collect(Collectors.toList());
            return weCustomerService.listByIds(eidList);
        }
    }

    @Override
    public WeTaskFissionStatisticVO taskFissionStatistic(Long taskFissionId, Date startTime, Date endTime) {
        WeTaskFissionStatisticVO vo = new WeTaskFissionStatisticVO();
        WeTaskFission taskFission = weTaskFissionMapper.selectWeTaskFissionById(taskFissionId);
        if (taskFission == null) {
            throw new WeComException("?????????????????????");
        }
        vo.setTaskFissionId(taskFissionId);
        vo.setTaskName(taskFission.getTaskName());
        vo.setStartTime(startTime);
        vo.setEndTime(endTime);
        Map<String, List<WeTaskFissionCompleteRecord>> completeRecordsMap = weTaskFissionCompleteRecordService.statisticCompleteRecords(taskFissionId, startTime, endTime).parallelStream().filter(Objects::nonNull).collect(Collectors.groupingBy(item -> DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, item.getCreateTime())));
        Map<String, List<WeTaskFissionRecord>> recordsMap = weTaskFissionRecordService.statisticRecords(taskFissionId, startTime, endTime).parallelStream().filter(Objects::nonNull).collect(Collectors.groupingBy(item -> DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, item.getCreateTime())));
        List<WeTaskFissionDailyDataVO> dailyDataList = Lists.newArrayList();
        DateUtils.findDates(startTime, endTime).parallelStream().map(d -> DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, d))
                .forEach(date -> {
                    WeTaskFissionDailyDataVO v = new WeTaskFissionDailyDataVO();
                    v.setDay(date);
                    List<WeTaskFissionCompleteRecord> completeList = completeRecordsMap.get(date);
                    List<WeTaskFissionRecord> recordsList = recordsMap.get(date);
                    if (CollectionUtils.isNotEmpty(completeList)) {
                        v.setIncrease(completeList.size());
                    }
                    if (CollectionUtils.isNotEmpty(recordsList)) {
                        v.setAttend(recordsList.size());
                        int completeSize = (int) recordsList.stream().filter(r -> r.getCompleteTime() != null).count();
                        v.setComplete(completeSize);
                    }
                    dailyDataList.add(v);
                });
        vo.setData(dailyDataList);
        return vo;
    }

    @Override
    public WeTaskFissionProgressVO getCustomerTaskProgress(WeTaskFission taskFission, String eid) {
        long complete = 0L;
        long total = taskFission.getFissNum();
        List<WeCustomer> list = getCustomerListById(eid, String.valueOf(taskFission.getId()));
        if (CollectionUtils.isNotEmpty(list)) {
            complete = list.size();
        } else {
            list = new ArrayList<>();
        }
        return WeTaskFissionProgressVO.builder().total(total).completed(complete).customers(list).build();
    }

    /*************************************** private functions **************************************/

    private String getPosterQRCode(String fissionTargetId, WeTaskFissionRecord record, WeCustomer weCustomer) {
        String qrCode = record.getQrCode();
        if (StringUtils.isBlank(qrCode)) {
            Long taskFissionId = record.getTaskFissionId();
            WeTaskFission taskFission = weTaskFissionMapper.selectWeTaskFissionById(taskFissionId);
            Integer taskFissionType = taskFission.getFissionType();
            if (TaskFissionType.USER_FISSION.getCode().equals(taskFissionType)) {
                qrCode = getUserFissionQrcode(fissionTargetId, record);
            } else if (TaskFissionType.GROUP_FISSION.getCode().equals(taskFissionType)) {
                qrCode = getGroupFissionQrcode(taskFissionId, record, weCustomer);
            } else {
                throw new WeComException("?????????????????????");
            }
        }
        return qrCode;
    }

    private String getUserFissionQrcode(String fissionTargetId, WeTaskFissionRecord record) {
        //???????????????
        String qrcode = null;
        WeExternalContactDto.WeContactWay contactWay = posterContactWay(fissionTargetId, record.getId());
        WeExternalContactDto dto = weExternalContactClient.addContactWay(contactWay);
        if (dto != null) {
            qrcode = dto.getQr_code();
        }
        return qrcode;
    }

    private String getGroupFissionQrcode(Long taskFissionId, WeTaskFissionRecord record, WeCustomer weCustomer) {
        String qrCode = null;
        if (weCustomer != null) {
            String avatar = weCustomer.getAvatar();
            WeMaterial file = weMaterialService.findWeMaterialById(Long.parseLong(avatar));
            String avatarUrl = StringUtils.isEmpty(avatar) ? file.getMaterialUrl() : avatar;
            String content = "/wecom/fission/complete/" + taskFissionId + "/records/" + record.getId();
            BufferedImage bufferedImage = QREncode.crateQRCode(content, avatarUrl);
            if (bufferedImage != null) {
                try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                    ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
                    NetFileUtils.StreamMultipartFile streamMultipartFile = new NetFileUtils.StreamMultipartFile(System.currentTimeMillis() + ".jpg", byteArrayOutputStream.toByteArray());
                    qrCode = FileUploadUtils.upload2Cos(streamMultipartFile, cosConfig);
                } catch (Exception e) {
                    log.warn("???????????????????????????, record={}, customer={}, exception={}", record, weCustomer, ExceptionUtils.getStackTrace(e));
                    throw new WeComException("?????????????????????");
                }
            }
            return qrCode;
        } else {
            throw new WeComException("?????????????????????,?????????????????????");
        }
    }

    private WeExternalContactDto.WeContactWay posterContactWay(String fissionTargetId, Long recordId) {
        WeExternalContactDto.WeContactWay wcw = new WeExternalContactDto.WeContactWay();
        wcw.setScene(2);
        wcw.setType(1);
        wcw.setUser(new String[]{fissionTargetId});
        wcw.setState(WeConstans.FISSION_PREFIX + recordId);
        return wcw;
    }

    private WeTaskFissionRecord getTaskFissionRecordId(Long taskFissionId, String customerId, String customerName) {
        WeTaskFissionRecord record = WeTaskFissionRecord.builder()
                .taskFissionId(taskFissionId)
                .customerId(customerId)
                .customerName(customerName).build();
        List<WeTaskFissionRecord> searchExists = weTaskFissionRecordService.selectWeTaskFissionRecordList(record);
        WeTaskFissionRecord recordInfo;
        if (CollectionUtils.isNotEmpty(searchExists)) {
            recordInfo = searchExists.get(0);
        } else {
            int insertRows = weTaskFissionRecordService.insertWeTaskFissionRecord(record);
            if (insertRows > 0) {
                recordInfo = record;
            } else {
                throw new WeComException("?????????????????????????????????????????????");
            }
        }
        return recordInfo;
    }

    private void groupQrcodeHandler(WeTaskFission weTaskFission) {
        String groupQrcodeId = weTaskFission.getFissionTargetId();
        if (weTaskFission.getFissionType() != null && weTaskFission.getFissionType().equals(TaskFissionType.GROUP_FISSION.getCode())
                && StringUtils.isNotBlank(groupQrcodeId) && StringUtils.isBlank(weTaskFission.getFissQrcode())) {
            WeGroupCode groupCode = weGroupCodeService.selectWeGroupCodeById(Long.parseLong(groupQrcodeId));
            if (groupCode != null) {
                String qrcodeUrl = groupCode.getCodeUrl();
                weTaskFission.setFissQrcode(qrcodeUrl);
            }
        }
    }
}
