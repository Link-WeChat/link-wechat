package com.tencent.wework;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.linkwechat.common.core.domain.elastic.ElasticSearchEntity;
import com.linkwechat.common.utils.wecom.RSAUtil;
import lombok.extern.slf4j.Slf4j;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sxw
 * @description
 * @date 2020/12/2 16:01
 **/
@Slf4j
public class FinanceUtils {
    /**
     *  NewSdk返回的sdk指针
     */
    private static volatile long sdk = 0;
    /**
     * 超时时间，单位秒
     */
    private final static long timeout = 5 * 60;
    /**
     *一次拉取的消息条数，最大值1000条，超过1000条会返回错误
     */
    private final static long LIMIT = 1000;

    /**
     * 初始化
     * @param corpId 企业id
     * @param secret 会话存档密钥
     */
    public static void initSDK (String corpId, String secret) {
        if (sdk == 0) {
            sdk = Finance.NewSdk();
            Finance.Init(sdk,corpId,secret);
        }
    }

    private final static String privateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIEpAIBAAKCAQEAjGwIQIMGDJonMQdX3OJzZN0YVoYbORK1FU/8vhwdI9ixxSGW\n" +
            "nrAQc6r8k3g1QTXzmKAxdVlAa1OGZCpl5X5A6C9JwDhFpuN+SKJxfxtHNv1oBk0r\n" +
            "LnDftsdlibZFQn3fiMomxKyU8eujWBe31+pLQXaTSdSHcNhb+NrBa7AtNUr/c7bK\n" +
            "nnFUZHDpXFB4Z0BhK1cRh2NN7ppLNnIJXpduGrwGtLdx++LsBaX2vjO4qW/DKP2Z\n" +
            "3gZON1tGoofS1IPhCg0naoRN3lL7ctFAyxOr2ualGOc6hpvBGfjnBKU42aoaKQEi\n" +
            "7tVJHChonZXMRcm4mdPu5371IzPeR3NYlP3ESQIDAQABAoIBAEux8MDV4GzqrDXB\n" +
            "v5tSduHoTTZXFFWvv+29MIFKpmqsZSfe81KXlkbD5WOZaRu9+ZJMyFx5wEAaxlc4\n" +
            "g7UyWcrPsvHrY6CTAYAmEQhzq+/4qhq7fouKTQ46boHNv4gYUmdqkXtzjNbcLzFN\n" +
            "l8zMf8TKJpul9VzfRLThN2LVlYkkETcCukzZrK+aSCi6E/dL+noL1ndZYmWF5kbB\n" +
            "lk881x5CdbusYBtoxfh6ie0EXfCyK4duX3OdvXRcSpTdqiqTcrtt9oinh4ZHV930\n" +
            "BCTPkGGz5UL3C243W3dlzuonNXqwl++FCSyRMVONYSgb4NRJn4fvehjGtXyAv3Sz\n" +
            "nSAVoAECgYEA9dhYk4DT2Hclaqo/0ugQvUp7oIPN9qFgrZ6nU94SHkdrBqhFrCmz\n" +
            "32GeYGSsQJQx3T9Kuip7oGX9AOKn3f30NrcuAe9VzzWAkCKQgQgmtMVySAPWCVr9\n" +
            "4MH77hc3ijxr6iGd70BQ7EDyVQGRvZqOLl1wu7yZ9vhbVHA87KpgHf0CgYEAkjjn\n" +
            "mNudh+rs8ibrGu+md8bXJdxHtIbncXF5VsV7wi4HlHfKnWqy+z2If1akm2iacnsi\n" +
            "XDVqyYNExdtoxmbPc/R9ZJ8jP0FLPoWACcjP03r9H+5eiXrmniZbElGshnwKHFFh\n" +
            "ZlY1xPAXFgEjh10DDG1wK6/9eB13o+NdP7c7yz0CgYEA3HujNOadF84DaJ43j1dB\n" +
            "bAKYzWRoG4CvzAY12ymW5kD244mWssjT4MsT1y9yrJg8AEj7I+tl5HyTY2/jTVys\n" +
            "4UV3pZSYSLWbD6lwH8jHrehGjf+ivdE1T5Wp8+YukXOO9PQhSKlN6hR1QXee1YT/\n" +
            "buWubTDAhJZIknn2qepDOaECgYA/OnITLCwTYOey0ldpn2V5d7cC/RzYmFkuOeZB\n" +
            "OkYIsoS+k4o+xau9bl8+yQWG2hWnGU3DV6nkl2m+sOC+oihkuL0cLKA+MwrJb0rt\n" +
            "cDNF/HHGQ/oHMujUTDunUT/sK2jn09ztNqwri7I/5qApYXP3BL+zdHeDYCKi8Kca\n" +
            "7S3i9QKBgQCgKgQf8u+oRojMH14iJssAyNEhZgmJ85s/BeiUmMuW5K8iZZ5pnVuq\n" +
            "8nc/IJGVPHRTObdGCd1L4d5UERiX46Z1BjYv/nuK5DScDcB3mq6K9VXJLNE+MxwW\n" +
            "wOVT+6FjmBRucGXczAkWXCH7++y6gYzE+OuRIdv5sVkE24gNG7sawg==\n" +
            "-----END RSA PRIVATE KEY-----";

    /**
     *  拉取聊天记录
     * @param seq 消息的seq值，标识消息的序号
     * @param proxy 代理
     * @param passwd 密码
     */
    public static List<ElasticSearchEntity> getChatData(long seq, String proxy, String passwd){
        List<ElasticSearchEntity> resList = new ArrayList<>();
        long slice = Finance.NewSlice();
        int ret = Finance.GetChatData(sdk, seq, LIMIT, proxy, passwd, timeout, slice);
        if (ret != 0) {
            log.info("getChatData ret " + ret);
            return null;
        }
        String content = Finance.GetContentFromSlice(slice);
        JSONArray chatdataArr = JSONObject.parseObject(content).getJSONArray("chatdata");
        if (CollectionUtil.isNotEmpty(chatdataArr)){
            chatdataArr.stream().map(data -> (JSONObject) data).forEach(data ->{
                try {
                    ElasticSearchEntity elasticSearchEntity = new ElasticSearchEntity();
                    long LocalSEQ = data.getLong("seq");
                    JSONObject jsonObject = decryptChatRecord(sdk, data.getString("encrypt_random_key"),
                            data.getString("encrypt_chat_msg"), privateKey);
                    jsonObject.put("seq",LocalSEQ);
                    elasticSearchEntity.setData(jsonObject);
                    elasticSearchEntity.setId(jsonObject.getString("msgid"));
                    resList.add(elasticSearchEntity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        Finance.FreeSlice(slice);
        return resList;
    }


    public static void main(String[] args) {
        String secret = "_Ruv_TD_GzE4wJLvhMv4MeYkjM81_IJFDPcsUiss9fw";
        initSDK("ww24262ce93851488f",secret);
        getChatData(0,"","");
    }


    /**
     * @param sdk 初始化时候获取到的值
     * @param ncrypt_random_key 企业微信返回的随机密钥
     * @param encrypt_chat_msg 企业微信返回的单条记录的密文消息
     * @param privateKey 企业微信管理后台设置的私钥,!!!版本记得对应上!!!
     * @return JSONObject 返回不同格式的聊天数据,格式有二十来种
     * 详情请看官网 https://open.work.weixin.qq.com/api/doc/90000/90135/91774#%E6%B6%88%E6%81%AF%E6%A0%BC%E5%BC%8F
     */
    private static JSONObject decryptChatRecord(Long sdk, String ncrypt_random_key, String encrypt_chat_msg, String privateKey){
        Long msg = null;
        try {
            //获取私钥
            PrivateKey privateKeyObj  = RSAUtil.getPrivateKey(privateKey);
            String str  = RSAUtil.decryptRSA(ncrypt_random_key, privateKeyObj);
            //初始化参数slice
            msg = Finance.NewSlice();

            //解密
            Finance.DecryptData(sdk, str, encrypt_chat_msg, msg);
            String jsonDataStr = Finance.GetContentFromSlice(msg);

            log.info("解析数据:------------" + jsonDataStr);
            JSONObject realJsonData = (JSONObject) JSONObject.parseObject(jsonDataStr);

            return realJsonData;
        } catch (Exception e) {
            log.error("解析密文失败");
            return null;
        } finally {
            if(msg != null){
                //释放参数slice
                Finance.FreeSlice(msg);
            }
        }
    }
}