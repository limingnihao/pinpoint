package com.navercorp.pinpoint.collector.http.handler.vo;

public class SpanVO {
    private String traceId;
    private String spanId;
    private String parentSpanId = "-1";
    private String nextSpanId;

    private String ipAddress = "";
    private String appName = "";

    private String params = "";
    private String status = "0";

    private String elapsed = "";
    private String startTime = "";//毫秒

    // http
    private String httpUrl = "";
    private String httpType = "";

    // db
    private String dbIp = "";
    private String dbName = "";
    private String dbUrl = "";
    private String dbSql = "";
    private String dbMethod = "";

    // redis
    private String redisIp = "";
    private String redisMethod = "";

    // rpc
    private String service = "";
    private String method = "";

    // rpc provider
    private String clientAppName = "";
    private String clientIp = "";

    // rpc client
    private String providerAppName = "";
    private String providerIp = "";

    private String remoteType = "";

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public String getParentSpanId() {
        return parentSpanId;
    }

    public void setParentSpanId(String parentSpanId) {
        this.parentSpanId = parentSpanId;
    }

    public String getNextSpanId() {
        return nextSpanId;
    }

    public void setNextSpanId(String nextSpanId) {
        this.nextSpanId = nextSpanId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getElapsed() {
        return elapsed;
    }

    public void setElapsed(String elapsed) {
        this.elapsed = elapsed;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getHttpUrl() {
        return httpUrl;
    }

    public void setHttpUrl(String httpUrl) {
        this.httpUrl = httpUrl;
    }

    public String getHttpType() {
        return httpType;
    }

    public void setHttpType(String httpType) {
        this.httpType = httpType;
    }

    public String getDbIp() {
        return dbIp;
    }

    public void setDbIp(String dbIp) {
        this.dbIp = dbIp;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getDbSql() {
        return dbSql;
    }

    public void setDbSql(String dbSql) {
        this.dbSql = dbSql;
    }

    public String getDbMethod() {
        return dbMethod;
    }

    public void setDbMethod(String dbMethod) {
        this.dbMethod = dbMethod;
    }

    public String getRedisIp() {
        return redisIp;
    }

    public void setRedisIp(String redisIp) {
        this.redisIp = redisIp;
    }

    public String getRedisMethod() {
        return redisMethod;
    }

    public void setRedisMethod(String redisMethod) {
        this.redisMethod = redisMethod;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getClientAppName() {
        return clientAppName;
    }

    public void setClientAppName(String clientAppName) {
        this.clientAppName = clientAppName;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getProviderAppName() {
        return providerAppName;
    }

    public void setProviderAppName(String providerAppName) {
        this.providerAppName = providerAppName;
    }

    public String getProviderIp() {
        return providerIp;
    }

    public void setProviderIp(String providerIp) {
        this.providerIp = providerIp;
    }

    public String getRemoteType() {
        return remoteType;
    }

    public void setRemoteType(String remoteType) {
        this.remoteType = remoteType;
    }

    @Override
    public String toString() {
        return "SpanVO{" +
                "traceId='" + traceId + '\'' +
                ", spanId='" + spanId + '\'' +
                ", parentSpanId='" + parentSpanId + '\'' +
                ", nextSpanId='" + nextSpanId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", appName='" + appName + '\'' +
                ", params='" + params + '\'' +
                ", status='" + status + '\'' +
                ", elapsed='" + elapsed + '\'' +
                ", startTime='" + startTime + '\'' +
                ", httpUrl='" + httpUrl + '\'' +
                ", httpType='" + httpType + '\'' +
                ", dbIp='" + dbIp + '\'' +
                ", dbName='" + dbName + '\'' +
                ", dbUrl='" + dbUrl + '\'' +
                ", dbSql='" + dbSql + '\'' +
                ", dbMethod='" + dbMethod + '\'' +
                ", redisIp='" + redisIp + '\'' +
                ", redisMethod='" + redisMethod + '\'' +
                ", service='" + service + '\'' +
                ", method='" + method + '\'' +
                ", clientAppName='" + clientAppName + '\'' +
                ", clientIp='" + clientIp + '\'' +
                ", providerAppName='" + providerAppName + '\'' +
                ", providerIp='" + providerIp + '\'' +
                ", remoteType='" + remoteType + '\'' +
                '}';
    }
}
