package com.yskts.netty.load.client;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public class Result {
    private long start;
    private long sendLatency;
    private long responseLatency;
    private long totalLatency;
    private FullHttpRequest httpRequest;
    private FullHttpResponse httpResponse;

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getSendLatency() {
        return sendLatency;
    }

    public void setSendLatency(long sendLatency) {
        this.sendLatency = sendLatency;
    }

    public long getResponseLatency() {
        return responseLatency;
    }

    public void setResponseLatency(long responseLatency) {
        this.responseLatency = responseLatency;
    }

    public long getTotalLatency() {
        return totalLatency;
    }

    public void setTotalLatency(long totalLatency) {
        this.totalLatency = totalLatency;
    }

    public FullHttpRequest getHttpRequest() {
        return httpRequest;
    }

    public void setHttpRequest(FullHttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public FullHttpResponse getHttpResponse() {
        return httpResponse;
    }

    public void setHttpResponse(FullHttpResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    @Override
    public String toString() {
        return "Result{" +
                "start=" + start +
                ", sendLatency=" + sendLatency +
                ", responseLatency=" + responseLatency +
                ", totalLatency=" + totalLatency +
                ", httpRequest=" + httpRequest +
                ", httpResponse=" + httpResponse +
                '}';
    }
}
