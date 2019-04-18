package com.ctrip.framework.apollo.util;


import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ctrip.framework.apollo.model.ItemChangeSets;
import com.ctrip.framework.apollo.model.ItemDTO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All rights Reserved, Designed By www.maihaoche.com
 *
 * @Package com.ctrip.framework.apollo.util
 * @author: 文远（wenyuan@maihaoche.com）
 * @date: 2019-04-09 10:28
 * @Copyright: 2017-2020 www.maihaoche.com Inc. All rights reserved.
 * 注意：本内容仅限于卖好车内部传阅，禁止外泄以及用于其他的商业目
 */
public class ApolloAPIUtil {

    private static Logger logger = LoggerFactory.getLogger(ApolloAPIUtil.class);

    static String portalUrl = "http://118.25.1.27:8090";
//    static String portalUrl = "http://localhost:8090";

    static String token = "e16e5cd903fd0c97a116c873b448544b9d086de9";
//
//    ApolloOpenApiClient client = ApolloOpenApiClient.newBuilder()
//            .withPortalUrl(portalUrl)
//            .withToken(token)
//            .build();


    public static String createNamespace(String appId, String clusterName, String namespaceName) {
        Map<String, Object> param = new HashMap<>(3);
        param.put("appId", appId);
        param.put("clusterName", clusterName);
        param.put("name", namespaceName);
        param.put("comment", "创建namespace接口测试");
        param.put("dataChangeCreatedBy", "wenyuan");
        String url = portalUrl + "/apps/"+ appId +"/appnamespaces";
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        String result2 = HttpRequest.post(url)
                .body(gson.toJson(param))
                .execute().body();
        System.out.println(result2);
        JSONObject jsonObject = JSONUtil.parseObj(result2);
        if (jsonObject.get("id") == null) {
            logger.warn(jsonObject.get("exception").toString());
            return null;
        } else {
            return jsonObject.get("id").toString();
        }
    }

    public static List<ItemDTO> getItems(String appId, String clusterName, String namespaceName) {
        String response = HttpUtil.get(portalUrl + "/apps/"+ appId +"/clusters/"+ clusterName +"/namespaces/"+ namespaceName +"/items");
        return JSONUtil.toList(JSONUtil.parseArray(response), ItemDTO.class);
    }

    public static void createItems(String appId, String clusterName, String namespaceName, ItemChangeSets itemChangeSets) {
//        Map<String, Object> param = new HashMap<>(1);
//        param.put("changeSet", itemChangeSets);
//
//        String response = HttpUtil.post(portalUrl + "/apps/"+ appId +"/clusters/"+ clusterName +"/namespaces/"+ namespaceName +"/itemset",
//                param);
//        System.out.println(response);


        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        String result2 = HttpRequest.post(portalUrl + "/apps/"+ appId +"/clusters/"+ clusterName +"/namespaces/"+ namespaceName +"/itemset")
                .body(gson.toJson(itemChangeSets))
                .execute().body();

        System.out.println(result2);
    }

    public static void publish(String appId, String clusterName, String namespaceName) {
        Map<String, Object> param = new HashMap<>(2);
        param.put("name", DateUtil.now()+"-auto-relase");
        param.put("operator", "wenyuan");
        String response = HttpUtil.post(portalUrl + "/apps/"+ appId +"/clusters/"+ clusterName +"/namespaces/"+ namespaceName +"/releases",param);
        System.out.println(response);
    }


    public static void main(String[] args) throws IOException {
//        createNamespace("30325", "k8s_default", "k8s_wenyuan");

//        String response = HttpUtil.get("http://lg.haimaiche.net/app/create.json?appName=acura&namespace=k8s_wenyuan");
//        System.out.println(response);

//        List<ItemDTO> itemDTOS = getItems("30325", "k8s_default", "application");
//        System.out.println(itemDTOS.toString());

        createNamespace("30325", "k8s_default", "jjee");
//        ItemChangeSets itemChangeSets = new ItemChangeSets();
//        List<ItemDTO> createItems = new ArrayList<>();
//        ItemDTO itemDTO = new ItemDTO("server.port", "8080", "aaa", 1);
//        createItems.add(itemDTO);
//        itemChangeSets.setCreateItems(createItems);
//        itemChangeSets.setDataChangeCreatedBy("wenyuan");
//        itemChangeSets.setDataChangeLastModifiedBy("wenyuan");

//        createItems("30325", "k8s_default", "k8s_jason", itemChangeSets);

//        ApolloOpenApiClient client = ApolloOpenApiClient.newBuilder()
//                .withPortalUrl(portalUrl)
//                .withToken(token)
//                .build();
//        OpenItemDTO itemDTO = new OpenItemDTO();
//        itemDTO.setKey("aa");
//        itemDTO.setValue("bbbb");
//        itemDTO.setDataChangeCreatedBy("wenyuan");
//        itemDTO.setDataChangeLastModifiedBy("wenyuan");
//        client.createItem("32325", "testout", "k8s_default", "k8s_jack", itemDTO);
    }

}
