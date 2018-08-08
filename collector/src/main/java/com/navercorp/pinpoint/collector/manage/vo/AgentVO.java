package com.navercorp.pinpoint.collector.manage.vo;

public class AgentVO {
    private String appName;
    private String ipAddress;
    private String hostname;
    private String version;
    private String gcType;
    private String heapUsed;
    private String heapMax;
    private String nonHeapUsed;
    private String nonHeapMax;
    private String gcOldCount;
    private String gcOldTime;
    private String JvmCpuLoad;
    private String systemCpuLoad;

    @Override
    public String toString() {
        return "AgentVO{" +
                "appName='" + appName + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", hostname='" + hostname + '\'' +
                ", version='" + version + '\'' +
                ", gcType='" + gcType + '\'' +
                ", heapUsed='" + heapUsed + '\'' +
                ", heapMax='" + heapMax + '\'' +
                ", nonHeapUsed='" + nonHeapUsed + '\'' +
                ", nonHeapMax='" + nonHeapMax + '\'' +
                ", gcOldCount='" + gcOldCount + '\'' +
                ", gcOldTime='" + gcOldTime + '\'' +
                ", JvmCpuLoad='" + JvmCpuLoad + '\'' +
                ", systemCpuLoad='" + systemCpuLoad + '\'' +
                '}';
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGcType() {
        return gcType;
    }

    public void setGcType(String gcType) {
        this.gcType = gcType;
    }

    public String getHeapUsed() {
        return heapUsed;
    }

    public void setHeapUsed(String heapUsed) {
        this.heapUsed = heapUsed;
    }

    public String getHeapMax() {
        return heapMax;
    }

    public void setHeapMax(String heapMax) {
        this.heapMax = heapMax;
    }

    public String getNonHeapUsed() {
        return nonHeapUsed;
    }

    public void setNonHeapUsed(String nonHeapUsed) {
        this.nonHeapUsed = nonHeapUsed;
    }

    public String getNonHeapMax() {
        return nonHeapMax;
    }

    public void setNonHeapMax(String nonHeapMax) {
        this.nonHeapMax = nonHeapMax;
    }

    public String getGcOldCount() {
        return gcOldCount;
    }

    public void setGcOldCount(String gcOldCount) {
        this.gcOldCount = gcOldCount;
    }

    public String getGcOldTime() {
        return gcOldTime;
    }

    public void setGcOldTime(String gcOldTime) {
        this.gcOldTime = gcOldTime;
    }

    public String getJvmCpuLoad() {
        return JvmCpuLoad;
    }

    public void setJvmCpuLoad(String jvmCpuLoad) {
        JvmCpuLoad = jvmCpuLoad;
    }

    public String getSystemCpuLoad() {
        return systemCpuLoad;
    }

    public void setSystemCpuLoad(String systemCpuLoad) {
        this.systemCpuLoad = systemCpuLoad;
    }
}
