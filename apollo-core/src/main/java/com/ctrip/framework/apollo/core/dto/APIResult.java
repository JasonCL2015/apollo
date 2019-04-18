package com.ctrip.framework.apollo.core.dto;

import java.io.Serializable;

/**
 * All rights Reserved, Designed By www.maihaoche.com
 *
 * @Package com.ctrip.framework.apollo.core.dto
 * @author: 文远（wenyuan@maihaoche.com）
 * @date: 2019-04-12 15:13
 * @Copyright: 2017-2020 www.maihaoche.com Inc. All rights reserved.
 * 注意：本内容仅限于卖好车内部传阅，禁止外泄以及用于其他的商业目
 */
public class APIResult<T> implements Serializable {

    private static final long serialVersionUID = 1922539991804940811L;

    private boolean success = true;

    private String code = "200";

    private String message = "";

    private T data;

    private Long timestamp = System.currentTimeMillis();

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
