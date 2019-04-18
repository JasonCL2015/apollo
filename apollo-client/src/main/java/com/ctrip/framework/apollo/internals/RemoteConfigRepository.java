package com.ctrip.framework.apollo.internals;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ctrip.framework.apollo.Apollo;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.dto.AcuraDTO;
import com.ctrip.framework.apollo.core.dto.ApolloConfig;
import com.ctrip.framework.apollo.core.dto.ApolloNotificationMessages;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.core.schedule.ExponentialSchedulePolicy;
import com.ctrip.framework.apollo.core.schedule.SchedulePolicy;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.exceptions.ApolloConfigStatusCodeException;
import com.ctrip.framework.apollo.model.ItemChangeSets;
import com.ctrip.framework.apollo.model.ItemDTO;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import com.ctrip.framework.apollo.util.ApolloAPIUtil;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.apollo.util.ExceptionUtil;
import com.ctrip.framework.apollo.util.http.HttpRequest;
import com.ctrip.framework.apollo.util.http.HttpResponse;
import com.ctrip.framework.apollo.util.http.HttpUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

//import com.ctrip.framework.apollo.openapi.client.ApolloOpenApiClient;
//import com.ctrip.framework.apollo.openapi.dto.NamespaceReleaseDTO;
//import com.ctrip.framework.apollo.openapi.dto.OpenAppNamespaceDTO;
//import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
//import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
//import com.google.gson.reflect.TypeToken;
//import org.apache.http.client.utils.DateUtils;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class RemoteConfigRepository extends AbstractConfigRepository {
    private static final Logger logger = LoggerFactory.getLogger(RemoteConfigRepository.class);
    private static final Joiner STRING_JOINER = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR);
    private static final Joiner.MapJoiner MAP_JOINER = Joiner.on("&").withKeyValueSeparator("=");
    private static final Escaper pathEscaper = UrlEscapers.urlPathSegmentEscaper();
    private static final Escaper queryParamEscaper = UrlEscapers.urlFormParameterEscaper();

    private final ConfigServiceLocator m_serviceLocator;
    private final HttpUtil m_httpUtil;
    private final ConfigUtil m_configUtil;
    private final RemoteConfigLongPollService remoteConfigLongPollService;
    private volatile AtomicReference<ApolloConfig> m_configCache;
    private final String m_namespace;
    private final static ScheduledExecutorService m_executorService;
    private final AtomicReference<ServiceDTO> m_longPollServiceDto;
    private final AtomicReference<ApolloNotificationMessages> m_remoteMessages;
    private final RateLimiter m_loadConfigRateLimiter;
    private final AtomicBoolean m_configNeedForceRefresh;
    private final SchedulePolicy m_loadConfigFailSchedulePolicy;
    private final Gson gson;

    static {
        m_executorService = Executors.newScheduledThreadPool(1, ApolloThreadFactory.create("RemoteConfigRepository", true));
    }

    /**
     * Constructor.
     *
     * @param namespace the namespace
     */
    public RemoteConfigRepository(String namespace) {
        m_namespace = namespace;
        m_configCache = new AtomicReference<>();
        m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        m_httpUtil = ApolloInjector.getInstance(HttpUtil.class);
        m_serviceLocator = ApolloInjector.getInstance(ConfigServiceLocator.class);
        remoteConfigLongPollService = ApolloInjector.getInstance(RemoteConfigLongPollService.class);
        m_longPollServiceDto = new AtomicReference<>();
        m_remoteMessages = new AtomicReference<>();
        m_loadConfigRateLimiter = RateLimiter.create(m_configUtil.getLoadConfigQPS());
        m_configNeedForceRefresh = new AtomicBoolean(true);
        m_loadConfigFailSchedulePolicy = new ExponentialSchedulePolicy(m_configUtil.getOnErrorRetryInterval(),
                m_configUtil.getOnErrorRetryInterval() * 8);
        gson = new Gson();
        this.trySync();
        this.schedulePeriodicRefresh();
        this.scheduleLongPollingRefresh();
    }

    @Override
    public Properties getConfig() {
        if (m_configCache.get() == null) {
            this.sync();
        }
        return transformApolloConfigToProperties(m_configCache.get());
    }

    @Override
    public void setUpstreamRepository(ConfigRepository upstreamConfigRepository) {
        // remote config doesn't need upstream
    }

    @Override
    public ConfigSourceType getSourceType() {
        return ConfigSourceType.REMOTE;
    }

    private void schedulePeriodicRefresh() {
        logger.debug("Schedule periodic refresh with interval: {} {}", m_configUtil.getRefreshInterval(),
                m_configUtil.getRefreshIntervalTimeUnit());
        m_executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Tracer.logEvent("Apollo.ConfigService", String.format("periodicRefresh: %s", m_namespace));
                logger.debug("refresh config for namespace: {}", m_namespace);
                trySync();
                Tracer.logEvent("Apollo.Client.Version", Apollo.VERSION);
            }
        }, m_configUtil.getRefreshInterval(), m_configUtil.getRefreshInterval(), m_configUtil.getRefreshIntervalTimeUnit());
    }

    @Override
    protected synchronized void sync() {
        Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "syncRemoteConfig");

        try {
            ApolloConfig previous = m_configCache.get();
            ApolloConfig current = loadApolloConfig();

            // reference equals means HTTP 304
            if (previous != current) {
                logger.debug("Remote Config refreshed!");
                m_configCache.set(current);
                this.fireRepositoryChange(m_namespace, this.getConfig());
            }

            if (current != null) {
                Tracer.logEvent(String.format("Apollo.Client.Configs.%s", current.getNamespaceName()), current.getReleaseKey());
            }

            transaction.setStatus(Transaction.SUCCESS);
        } catch (Throwable ex) {
            transaction.setStatus(ex);
            throw ex;
        } finally {
            transaction.complete();
        }
    }

    /**
     * <p>
     * 将获得的配置文件转换成properties输出，这里对加密字段做解密处理
     * </p>
     *
     * @param apolloConfig
     * @return Properties
     * @author 文远（wenyuan@maihaoche.com）
     * @date 2018/9/28 上午10:55
     * @since V1.1.0-SNAPSHOT
     */
    private Properties transformApolloConfigToProperties(ApolloConfig apolloConfig) {
        Properties result = new Properties();
        result.putAll(apolloConfig.getConfigurations());
        return result;
    }

    private ApolloConfig loadApolloConfig() {
        if (!m_loadConfigRateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
            // wait at most 5 seconds
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
            }
        }
        String appId = m_configUtil.getAppId();
        String cluster = m_configUtil.getCluster();

        String dataCenter = m_configUtil.getDataCenter();
        Tracer.logEvent("Apollo.Client.ConfigMeta", STRING_JOINER.join(appId, cluster, m_namespace));
        int maxRetries = m_configNeedForceRefresh.get() ? 2 : 1;
        long onErrorSleepTime = 0; // 0 means no sleep
        Throwable exception = null;

        List<ServiceDTO> configServices = getConfigServices();
        String url = null;
        for (int i = 0; i < maxRetries; i++) {
            List<ServiceDTO> randomConfigServices = Lists.newLinkedList(configServices);
            Collections.shuffle(randomConfigServices);
            // Access the server which notifies the client first
            if (m_longPollServiceDto.get() != null) {
                randomConfigServices.add(0, m_longPollServiceDto.getAndSet(null));
            }

            for (ServiceDTO configService : randomConfigServices) {
                if (onErrorSleepTime > 0) {
                    logger.warn("Load config failed, will retry in {} {}. appId: {}, cluster: {}, namespaces: {}",
                            onErrorSleepTime, m_configUtil.getOnErrorRetryIntervalTimeUnit(), appId, cluster, m_namespace);

                    try {
                        m_configUtil.getOnErrorRetryIntervalTimeUnit().sleep(onErrorSleepTime);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
                /**
                 * 1,判断m_configUtil.getK8sNamespace()是否为空，如果为空，不改变原有逻辑，如果不为空，则进行下一步
                 * 2,根据appId和k8s_namespace获取配置，如果配置不存在，则用appId和k8s_default进行获取配置模板，
                 * 3,处理k8s_default中的配置项占位符，进行保存至新的apollo cluster,命名为k8s_namespace
                 */
                if (!Strings.isNullOrEmpty(m_configUtil.getK8sNamespace())) {
                    List<ItemDTO> itemDTOList = null;
                    try {
                        itemDTOList = ApolloAPIUtil.getItems(m_configUtil.getApolloEnv(), appId, ConfigConsts.K8S_CLUSTER_DEFAULT,
                                ConfigConsts.K8S_NAMESPACE_PRE + m_configUtil.getK8sNamespace());
                    } catch (Exception e) {
                        logger.error(e.toString());
                    }
                    if (CollUtil.isNotEmpty(itemDTOList)) {
                        //加载配置内容,返回apolloConfig
                        ApolloConfig apolloConfig = new ApolloConfig(appId,
                                ConfigConsts.K8S_CLUSTER_DEFAULT, ConfigConsts.K8S_NAMESPACE_PRE + m_configUtil.getK8sNamespace(), null);

                        Map<String, String> itemMap = new HashMap<>(itemDTOList.size());
                        for (int j = 0; j < itemDTOList.size(); j++) {
                            ItemDTO itemDTO = itemDTOList.get(j);
                            itemMap.put(itemDTO.getKey(), itemDTO.getValue());
                        }
                        apolloConfig.setConfigurations(itemMap);
                        m_configNeedForceRefresh.set(false);
                        m_loadConfigFailSchedulePolicy.success();
                        return apolloConfig;
                    } else {
                        //获取k8s_default中namespace=application的模板
                        try {
                            itemDTOList = ApolloAPIUtil.getItems(m_configUtil.getApolloEnv(), appId, ConfigConsts.K8S_CLUSTER_DEFAULT,
                                    ConfigConsts.NAMESPACE_APPLICATION);
                        } catch (Exception e) {
                            logger.error(e.toString());
                        }
                        if (CollUtil.isEmpty(itemDTOList)) {
                            String message = String.format("Load Apollo Config failed - appId: %s, cluster: %s, namespace: %s, url: %s",
                                    appId,
                                    cluster, m_namespace, url);
                            throw new ApolloConfigException(message, exception);
                        }

                        ApolloConfig apolloConfig = new ApolloConfig(appId,
                                ConfigConsts.K8S_CLUSTER_DEFAULT, ConfigConsts.K8S_NAMESPACE_PRE + m_configUtil.getK8sNamespace(), null);

                        //创建新的namespace
                        String appNamespaceId = ApolloAPIUtil.createNamespace(m_configUtil.getApolloEnv(), appId,
                                ConfigConsts.K8S_CLUSTER_DEFAULT,
                                ConfigConsts.K8S_NAMESPACE_PRE + m_configUtil.getK8sNamespace());

                        Map<String, String> itemMap = new HashMap<>();

                        String namespaceId = ApolloAPIUtil.getNamespaceIdByParam(m_configUtil.getApolloEnv(), appId,
                                ConfigConsts.K8S_CLUSTER_DEFAULT,
                                ConfigConsts.K8S_NAMESPACE_PRE + m_configUtil.getK8sNamespace());
                        if (appNamespaceId != null && namespaceId != null) {
                            ItemChangeSets itemChangeSets = new ItemChangeSets();
                            List<ItemDTO> createItems = new ArrayList<>();
                            itemChangeSets.setDataChangeLastModifiedBy("wenyuan");

                            AcuraDTO acuraDTO = null;
                            for (int j = 0; j < itemDTOList.size(); j++) {
                                ItemDTO itemDTO = itemDTOList.get(j);
                                if (itemDTO.getValue().contains(ConfigConsts.PALCEHOLDER_NAMESPACE)) {
                                    itemDTO.setValue(itemDTO.getValue().replace(ConfigConsts.PALCEHOLDER_NAMESPACE,
                                            m_configUtil.getK8sNamespace()));
                                    assemBlyItemMap(itemMap, namespaceId, createItems, itemDTO);
                                    continue;
                                }
                                if (itemDTO.getValue().contains(ConfigConsts.PLACEHOLDER_ACURA_APPID)) {
                                    if (acuraDTO == null) {
                                        acuraDTO = getAcuraDTO(m_configUtil.getAppName(),m_configUtil.getK8sNamespace());
                                    }
                                    itemDTO.setValue(acuraDTO.getId());
                                    assemBlyItemMap(itemMap, namespaceId, createItems, itemDTO);
                                    continue;
                                }
                                if (itemDTO.getValue().contains(ConfigConsts.PLACEHOLDER_ACURA_APPKEY)) {
                                    if (acuraDTO == null) {
                                        acuraDTO = getAcuraDTO(m_configUtil.getAppName(),m_configUtil.getK8sNamespace());
                                    }
                                    itemDTO.setValue(acuraDTO.getKey());
                                    assemBlyItemMap(itemMap, namespaceId, createItems, itemDTO);
                                }
                            }
                            itemChangeSets.setCreateItems(createItems);
                            ApolloAPIUtil.createItems(m_configUtil.getApolloEnv(), appId, ConfigConsts.K8S_CLUSTER_DEFAULT,
                                    ConfigConsts.K8S_NAMESPACE_PRE + m_configUtil.getK8sNamespace(), itemChangeSets);
                            ApolloAPIUtil.publish(m_configUtil.getApolloEnv(), appId, ConfigConsts.K8S_CLUSTER_DEFAULT,
                                    ConfigConsts.K8S_NAMESPACE_PRE + m_configUtil.getK8sNamespace());

                            apolloConfig.setConfigurations(itemMap);
                            m_configNeedForceRefresh.set(false);
                            m_loadConfigFailSchedulePolicy.success();
                            return apolloConfig;
                        }
                    }
                }

                url = assembleQueryConfigUrl(configService.getHomepageUrl(), appId, cluster, m_namespace, dataCenter,
                        m_remoteMessages.get(), m_configCache.get());

                logger.info("Loading config from {}", url);
                HttpRequest request = new HttpRequest(url);

                Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "queryConfig");
                transaction.addData("Url", url);
                try {

                    HttpResponse<ApolloConfig> response = m_httpUtil.doGet(request, ApolloConfig.class);
                    m_configNeedForceRefresh.set(false);
                    m_loadConfigFailSchedulePolicy.success();

                    transaction.addData("StatusCode", response.getStatusCode());
                    transaction.setStatus(Transaction.SUCCESS);

                    if (response.getStatusCode() == 304) {
                        logger.info("Config server responds with 304 HTTP status code.");
                        return m_configCache.get();
                    }

                    ApolloConfig result = response.getBody();

                    logger.info("Loaded config for {}: {}", m_namespace, result);

                    return result;
                } catch (ApolloConfigStatusCodeException ex) {
                    ApolloConfigStatusCodeException statusCodeException = ex;
                    // config not found
                    if (ex.getStatusCode() == 404) {
                        String message = String
                                .format("Could not find config for namespace - appId: %s, cluster: %s, namespace: %s, "
                                        + "please check whether the configs are released in Apollo!", appId, cluster, m_namespace);
                        statusCodeException = new ApolloConfigStatusCodeException(ex.getStatusCode(), message);
                    }
                    Tracer.logEvent("ApolloConfigException", ExceptionUtil.getDetailMessage(statusCodeException));
                    transaction.setStatus(statusCodeException);
                    exception = statusCodeException;
                } catch (Throwable ex) {
                    Tracer.logEvent("ApolloConfigException", ExceptionUtil.getDetailMessage(ex));
                    transaction.setStatus(ex);
                    exception = ex;
                } finally {
                    transaction.complete();
                }

                // if force refresh, do normal sleep, if normal config load, do exponential
                // sleep
                onErrorSleepTime = m_configNeedForceRefresh.get() ? m_configUtil.getOnErrorRetryInterval()
                        : m_loadConfigFailSchedulePolicy.fail();
            }

        }
        String message = String.format("Load Apollo Config failed - appId: %s, cluster: %s, namespace: %s, url: %s", appId,
                cluster, m_namespace, url);
        throw new ApolloConfigException(message, exception);
    }

    private void assemBlyItemMap(Map<String, String> itemMap, String namespaceId, List<ItemDTO> createItems, ItemDTO itemDTO) {
        if (StrUtil.isNotEmpty(itemDTO.getKey())) {
            itemMap.put(itemDTO.getKey(), itemDTO.getValue());
            itemDTO.setNamespaceId(Long.valueOf(namespaceId));
            createItems.add(itemDTO);
        }
    }


    private static AcuraDTO getAcuraDTO(String appName,String namespace) {
        String aucraUrl = "http://lg.haimaiche.net/app/create.json?appName="+appName+"&namespace="+namespace;
        String result = cn.hutool.http.HttpUtil.get(aucraUrl);
        JSONObject object = JSONUtil.parseObj(result);
        if (StrUtil.equals(object.get("code").toString(), "200")) {
            AcuraDTO acuraDTO = object.get("data", AcuraDTO.class);
            return acuraDTO;
        } else {
            logger.error("调用讴歌失败:" + object.get("message").toString());
            return new AcuraDTO();
        }
    }

    String assembleQueryConfigUrl(String uri, String appId, String cluster, String namespace, String dataCenter,
                                  ApolloNotificationMessages remoteMessages, ApolloConfig previousConfig) {

        String path = "configs/%s/%s/%s";
        List<String> pathParams = Lists.newArrayList(pathEscaper.escape(appId), pathEscaper.escape(cluster),
                pathEscaper.escape(namespace));
        Map<String, String> queryParams = Maps.newHashMap();

        if (previousConfig != null) {
            queryParams.put("releaseKey", queryParamEscaper.escape(previousConfig.getReleaseKey()));
        }

        if (!Strings.isNullOrEmpty(dataCenter)) {
            queryParams.put("dataCenter", queryParamEscaper.escape(dataCenter));
        }

        String localIp = m_configUtil.getLocalIp();
        if (!Strings.isNullOrEmpty(localIp)) {
            queryParams.put("ip", queryParamEscaper.escape(localIp));
        }

        if (remoteMessages != null) {
            queryParams.put("messages", queryParamEscaper.escape(gson.toJson(remoteMessages)));
        }

        String pathExpanded = String.format(path, pathParams.toArray());

        if (!queryParams.isEmpty()) {
            pathExpanded += "?" + MAP_JOINER.join(queryParams);
        }
        if (!uri.endsWith("/")) {
            uri += "/";
        }
        return uri + pathExpanded;
    }

    private void scheduleLongPollingRefresh() {
        remoteConfigLongPollService.submit(m_namespace, this);
    }

    public void onLongPollNotified(ServiceDTO longPollNotifiedServiceDto, ApolloNotificationMessages remoteMessages) {
        m_longPollServiceDto.set(longPollNotifiedServiceDto);
        m_remoteMessages.set(remoteMessages);
        m_executorService.submit(new Runnable() {
            @Override
            public void run() {
                m_configNeedForceRefresh.set(true);
                trySync();
            }
        });
    }

    private List<ServiceDTO> getConfigServices() {
        List<ServiceDTO> services = m_serviceLocator.getConfigServices();
        if (services.size() == 0) {
            throw new ApolloConfigException("No available config service");
        }

        return services;
    }

    public static void main(String[] args) throws Exception {
        // String pwd = "{apollo}3da73386e4e0827f771c205a2e70d0f0";
        // System.out.println(pwd.contains(PASSWORDPRE));
        // String password = pwd.replace(PASSWORDPRE, "");
        // System.out.println(password);
        // System.out.println(new String(AESUtil.decryptAES(password)));

//    Map<String, String> configurations = new HashMap<>();
//    configurations.put("a", "a");
//    ApolloConfig apolloConfig = new ApolloConfig();
//    apolloConfig.setConfigurations(configurations);

        // transformApolloConfigToProperties(apolloConfig);

//        APIResult<AcuraDTO> apiResult = getAcuraDTO();

//        String result = cn.hutool.http.HttpUtil.get("http://lg.haimaiche.net/app/create.json?appName=malibu&namespace=k8s_wenyuan");
//        JSONObject object = JSONUtil.parseObj(result);
//        AcuraDTO acuraDTO = object.get("data", AcuraDTO.class);
//        System.out.println(acuraDTO.toString());
    }
}
