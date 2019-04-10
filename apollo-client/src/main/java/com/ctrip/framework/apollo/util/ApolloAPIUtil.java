package com.ctrip.framework.apollo.util;

import com.ctrip.framework.apollo.openapi.client.ApolloOpenApiClient;

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

    String portalUrl = "https://apollo.haimaiche.net";

    String token = "e16e5cd903fd0c97a116c873b448544b9d086de9";

    ApolloOpenApiClient client = ApolloOpenApiClient.newBuilder()
            .withPortalUrl(portalUrl)
            .withToken(token)
            .build();


}
