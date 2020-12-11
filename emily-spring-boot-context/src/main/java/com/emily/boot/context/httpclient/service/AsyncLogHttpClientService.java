package com.emily.boot.context.httpclient.service;

import com.emily.boot.context.httpclient.po.AsyncLogHttpClientRequest;
import com.emily.boot.context.httpclient.po.AsyncLogHttpClientResponse;

public interface AsyncLogHttpClientService {
    /**
     * @Description 记录请求信息
     * @Version 1.0
     */
    void traceRequest(AsyncLogHttpClientRequest asyncLogHttpClient);

    /**
     * @Description 记录响应信息
     * @Version 1.0
     */
    void traceResponse(AsyncLogHttpClientResponse asyncLogHttpClient);
}
