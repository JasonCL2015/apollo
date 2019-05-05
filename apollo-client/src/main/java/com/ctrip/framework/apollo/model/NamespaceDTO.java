package com.ctrip.framework.apollo.model;


public class NamespaceDTO extends BaseDTO{
  private long id;

  private String appId;

  private String clusterName;

  private String namespaceName;

  private boolean isDeleted = false;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getAppId() {
    return appId;
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getNamespaceName() {
    return namespaceName;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public void setNamespaceName(String namespaceName) {
    this.namespaceName = namespaceName;
  }

  public void setDeleted(boolean deleted) {
    isDeleted = deleted;
  }

  public boolean isDeleted() {
    return isDeleted;
  }
}
