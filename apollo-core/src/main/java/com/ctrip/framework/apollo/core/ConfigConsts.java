package com.ctrip.framework.apollo.core;

public interface ConfigConsts {
  String NAMESPACE_APPLICATION = "application";
  String CLUSTER_NAME_DEFAULT = "default";
  String CLUSTER_NAMESPACE_SEPARATOR = "+";
  String APOLLO_CLUSTER_KEY = "apollo.cluster";
  String APOLLO_META_KEY = "apollo.meta";
  String CONFIG_FILE_CONTENT_KEY = "content";
  String NO_APPID_PLACEHOLDER = "ApolloNoAppIdPlaceHolder";
  String K8S_NAMESPACE = "k8s.namespace";
  String K8S_NAMESPACE_DEFAULT = "k8s_default";
  String K8S_NAMESPACE_PRE = "k8s_";
  long NOTIFICATION_ID_PLACEHOLDER = -1;

  /**
   * apollo接口地址
   */
  String PORTAL_URL = "https://apollo.haimaiche.net";
  /**
   * apollo API TOKEN
   */
  String API_TOKEN = "e16e5cd903fd0c97a116c873b448544b9d086de9";

  String PALCEHOLDER_NAMESPACE = "{namespace}";
  String PLACEHOLDER_ACURA_APPID = "{acura.app.id}";
  String PLACEHOLDER_ACURA_APPKEY = "{acura.app.key}";

}
