package com.ctrip.framework.apollo.util;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.dto.AcuraDTO;
import com.ctrip.framework.apollo.core.dto.ApolloConfig;
import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.model.AppNamespaceDTO;
import com.ctrip.framework.apollo.model.ItemChangeSets;
import com.ctrip.framework.apollo.model.ItemDTO;
import com.ctrip.framework.apollo.model.NamespaceDTO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
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

    static String portalUrlTestOut = "http://118.25.1.27:8090";

    static String portalUrlTestIn = "http://172.21.10.86:8090";

//    static String portalUrlTestOut = "http://localhost:8090";


//    ApolloOpenApiClient client = ApolloOpenApiClient.newBuilder()
//            .withPortalUrl(portalUrlTestOut)
//            .withToken(token)
//            .build();


    public static AppNamespaceDTO createNamespace(Env env, String appId, String clusterName, String namespaceName) {
        String adminserviceUrl = getAdminserviceUrl(env);
        Map<String, Object> param = new HashMap<>(3);
        param.put("appId", appId);
        param.put("clusterName", clusterName);
        param.put("name", namespaceName);
        param.put("comment", "创建namespace接口测试");
        param.put("dataChangeCreatedBy", "apollo");
        String url = adminserviceUrl + "/apps/" + appId + "/appnamespaces";
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        String result = HttpRequest.post(url)
                .body(gson.toJson(param))
                .execute().body();
        System.out.println(result);
        JSONObject jsonObject = JSONUtil.parseObj(result);
        AppNamespaceDTO appNamespaceDTO = null;
        Object status = jsonObject.get("status");
        // 正常的获取
        if (status == null) {
            appNamespaceDTO = JSONUtil.toBean(JSONUtil.parseObj(result), AppNamespaceDTO.class);
        } else {
            logger.error("获取namespace失败:" + jsonObject.get("message").toString());
        }
        return appNamespaceDTO;
    }

    public static List<ItemDTO> getItems(Env env, String appId, String clusterName, String namespaceName) {
        String adminserviceUrl = getAdminserviceUrl(env);
        String response = HttpUtil.get(adminserviceUrl + "/apps/" + appId + "/clusters/" + clusterName + "/namespaces/" + namespaceName + "/items");
        return JSONUtil.toList(JSONUtil.parseArray(response), ItemDTO.class);
    }


    public static void createItems(Env env, String appId, String clusterName, String namespaceName, ItemChangeSets itemChangeSets) {
        String adminserviceUrl = getAdminserviceUrl(env);
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        String result2 = HttpRequest.post(adminserviceUrl + "/apps/" + appId + "/clusters/" + clusterName + "/namespaces/" + namespaceName + "/itemset")
                .body(gson.toJson(itemChangeSets))
                .execute().body();

        System.out.println(result2);
    }

    public static void publish(Env env, String appId, String clusterName, String namespaceName) {
        String adminserviceUrl = getAdminserviceUrl(env);
        Map<String, Object> param = new HashMap<>(2);
        param.put("name", DateUtil.now() + "-auto-relase");
        param.put("operator", "apollo");
        String response = HttpUtil.post(adminserviceUrl + "/apps/" + appId + "/clusters/" + clusterName + "/namespaces/" + namespaceName + "/releases", param);
        System.out.println(response);
    }


    public static String getNamespaceIdByParam(Env env, String appId, String clusterName, String namespaceName) {
        String adminserviceUrl = getAdminserviceUrl(env);
        String result = HttpUtil.get(adminserviceUrl + "/apps/" + appId + "/clusters/" + clusterName + "/namespaces/" + namespaceName);
        JSONObject jsonObject = JSONUtil.parseObj(result);
        System.out.println(result);
        if (jsonObject.get("id") == null) {
            logger.warn(jsonObject.get("exception").toString());
            return null;
        } else {
            return jsonObject.get("id").toString();
        }
    }

    public static NamespaceDTO getAllNamespace(Env env, String appId, String clusterName, String namespaceName) {
        String adminserviceUrl = getAdminserviceUrl(env);
        String url = adminserviceUrl + "/apps/" + appId + "/clusters/" + clusterName + "/namespaces/" + namespaceName + "/all";
        String result = HttpUtil.get(url);
        JSONObject jsonObject = JSONUtil.parseObj(result);
        NamespaceDTO namespaceDTO = null;
        Object status = jsonObject.get("status");
        // 正常的获取
        if (status == null) {
            namespaceDTO = JSONUtil.toBean(JSONUtil.parseObj(result), NamespaceDTO.class);
        } else {
            logger.error("获取namespace失败:" + jsonObject.get("message").toString());
        }
        return namespaceDTO;
    }

    public static AppNamespaceDTO getNamespace(Env env, String appId, String clusterName, String namespaceName) {
        String adminserviceUrl = getAdminserviceUrl(env);
        String result = HttpUtil.get(adminserviceUrl + "/apps/" + appId + "/clusters/" + clusterName + "/namespaces/" + namespaceName);
        JSONObject jsonObject = JSONUtil.parseObj(result);
        AppNamespaceDTO appNamespaceDTO = null;
        Object status = jsonObject.get("status");
        // 正常的获取
        if (status == null) {
            appNamespaceDTO = JSONUtil.toBean(JSONUtil.parseObj(result), AppNamespaceDTO.class);
        } else {
            logger.error("获取namespace失败:" + jsonObject.get("message").toString());
        }
        return appNamespaceDTO;
    }

    public static void rescueNamespace(Env env, String appId, String clusterName, String namespaceName) {
        String adminserviceUrl = getAdminserviceUrl(env);
        String result = HttpUtil.get(adminserviceUrl + "/apps/" + appId + "/clusters/" + clusterName + "/namespaces/" + namespaceName);
        JSONObject jsonObject = JSONUtil.parseObj(result);
        AppNamespaceDTO appNamespaceDTO = null;
        Object status = jsonObject.get("status");
    }

    private static String getAdminserviceUrl(Env env) {
        String adminserviceUrl;
        if (env.equals(Env.TESTIN)) {
            adminserviceUrl = portalUrlTestIn;
        } else if (env.equals(Env.TESTOUT)) {
            adminserviceUrl = portalUrlTestOut;
        } else {
            adminserviceUrl = portalUrlTestIn;
        }
        return adminserviceUrl;
    }


    private static void assemBlyItemMap(Map<String, String> itemMap, String namespaceId, List<ItemDTO> createItems, ItemDTO itemDTO) {
        if (StrUtil.isNotEmpty(itemDTO.getKey())) {
            itemMap.put(itemDTO.getKey(), itemDTO.getValue());
            itemDTO.setNamespaceId(Long.valueOf(namespaceId));
            createItems.add(itemDTO);
        }
    }


//    private static AcuraDTO getAcuraDTO(String appName, String namespace) {
//        String aucraUrl = "https://acura-" + namespace + ".maihaoche.net/app/create.json?appName=" + appName + "&namespace=" + namespace + "&token=Vp9yz78eFsqhfF";
//        String result = cn.hutool.http.HttpUtil.get(aucraUrl);
//        JSONObject object = JSONUtil.parseObj(result);
//        if (StrUtil.equals(object.get("code").toString(), "200")) {
//            AcuraDTO acuraDTO = object.get("data", AcuraDTO.class);
//            return acuraDTO;
//        } else {
//            logger.error("调用讴歌失败:" + object.get("message").toString());
//            return new AcuraDTO();
//        }
//    }

//    private static void copyItem(Env env, String appId, String clusterName, String namespaceName, String appName, AppNamespaceDTO appNamespaceDTO) {
//        Map<String, String> itemMap = new HashMap<>();
//        // 只有在创建成功了之后才去复制item
//        String namespaceId = String.valueOf(appNamespaceDTO.getId());
//        //获取默认的application下的 itemList
//        List<ItemDTO> itemDTOList = getItems(env, appId, clusterName, "application");
//        ItemChangeSets itemChangeSets = new ItemChangeSets();
//        List<ItemDTO> createItems = new ArrayList<>();
//        itemChangeSets.setDataChangeLastModifiedBy("apollo");
//        AcuraDTO acuraDTO = null;
//        for (int j = 0; j < itemDTOList.size(); j++) {
//            ItemDTO itemDTO = itemDTOList.get(j);
//            if (itemDTO.getValue().contains(ConfigConsts.PALCEHOLDER_NAMESPACE)) {
//                itemDTO.setValue(itemDTO.getValue().replace(ConfigConsts.PALCEHOLDER_NAMESPACE,
//                        namespaceName));
//                assemBlyItemMap(itemMap, namespaceId, createItems, itemDTO);
//            } else if (itemDTO.getValue().contains(ConfigConsts.PLACEHOLDER_ACURA_APPID)) {
//                if (acuraDTO == null) {
//                    acuraDTO = getAcuraDTO(appName, namespaceName);
//                }
//                itemDTO.setValue(acuraDTO.getId());
//                assemBlyItemMap(itemMap, namespaceId, createItems, itemDTO);
//            } else if (itemDTO.getValue().contains(ConfigConsts.PLACEHOLDER_ACURA_APPKEY)) {
//                if (acuraDTO == null) {
//                    acuraDTO = getAcuraDTO(appName, namespaceName);
//                }
//                itemDTO.setValue(acuraDTO.getKey());
//                assemBlyItemMap(itemMap, namespaceId, createItems, itemDTO);
//            } else {
//                assemBlyItemMap(itemMap, namespaceId, createItems, itemDTO);
//            }
//        }
//        itemChangeSets.setCreateItems(createItems);
//        ApolloAPIUtil.createItems(env, appId, ConfigConsts.K8S_CLUSTER_DEFAULT,
//                ConfigConsts.K8S_NAMESPACE_PRE + namespaceName, itemChangeSets);
//        ApolloAPIUtil.publish(env, appId, ConfigConsts.K8S_CLUSTER_DEFAULT,
//                ConfigConsts.K8S_NAMESPACE_PRE + namespaceName);
//    }

    public static void main(String[] args) throws IOException {
        Env env = Env.fromString("TESTIN");
        String appId = "40110";
        String clusterName = "k8s_default";
        String namespaceName = "sell";
        String appName = "chrysler";
        List<ItemDTO> itemDTOList = null;
        NamespaceDTO namespaceDTO = getAllNamespace(env, appId, clusterName, ConfigConsts.K8S_NAMESPACE_PRE + namespaceName);
        System.out.print(namespaceDTO);
//        //判定namespace是否已经删除还是不存在
//        NamespaceDTO namespaceDTO = getAllNamespace(env, appId, clusterName, ConfigConsts.K8S_NAMESPACE_PRE + namespaceName);
//        if (namespaceDTO == null) {
//            //如果namespace不存在，创建namespace和appNamespace
//            AppNamespaceDTO appNamespaceDTO = ApolloAPIUtil.createNamespace(env, appId,
//                    ConfigConsts.K8S_CLUSTER_DEFAULT, ConfigConsts.K8S_NAMESPACE_PRE + namespaceName);
//            // 只有在创建成功了之后才去复制item
//            if (appNamespaceDTO != null) {
//                copyItem(env, appId, clusterName, namespaceName, appName, appNamespaceDTO);
//            }
//        } else if (namespaceDTO.isDeleted()) {
//            //如果namespace是被逻辑删除的
//
//
//        } else {
//            //如果namespace存在
//            try {
//                itemDTOList = ApolloAPIUtil.getItems(env, appId, ConfigConsts.K8S_CLUSTER_DEFAULT,
//                        ConfigConsts.K8S_NAMESPACE_PRE + namespaceName);
//            } catch (Exception e) {
//                logger.error(e.toString());
//            }
//            //如果ItemList不为空
//            if (CollUtil.isNotEmpty(itemDTOList)) {
//                //加载配置内容,执行原有逻辑
//
//            } else {
//                //如果ItemList为空
//                AppNamespaceDTO appNamespaceDTO = getNamespace(env, appId, clusterName, ConfigConsts.K8S_NAMESPACE_PRE + namespaceName);
//                // 只有在创建成功了之后才去复制item
//                if (appNamespaceDTO != null) {
//                    copyItem(env, appId, clusterName, namespaceName, appName, appNamespaceDTO);
//                }
//            }
//        }
    }
}
