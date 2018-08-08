package com.navercorp.pinpoint.collector.manage.service.impl;

import com.navercorp.pinpoint.collector.dao.AgentLifeCycleDao;
import com.navercorp.pinpoint.collector.handler.AgentInfoHandler;
import com.navercorp.pinpoint.collector.handler.AgentStatHandlerV2;
import com.navercorp.pinpoint.collector.handler.SpanHandler;
import com.navercorp.pinpoint.collector.manage.service.SpanService;
import com.navercorp.pinpoint.collector.manage.vo.AgentVO;
import com.navercorp.pinpoint.collector.manage.vo.SpanVO;
import com.navercorp.pinpoint.common.server.bo.AgentLifeCycleBo;
import com.navercorp.pinpoint.common.server.util.AgentLifeCycleState;
import com.navercorp.pinpoint.common.util.TransactionIdUtils;
import com.navercorp.pinpoint.plugin.jdbc.mysql.MySqlConstants;
import com.navercorp.pinpoint.plugin.jdk.http.JdkHttpConstants;
import com.navercorp.pinpoint.plugin.redis.RedisConstants;
import com.navercorp.pinpoint.plugin.spring.boot.SpringBootConstants;
import com.navercorp.pinpoint.plugin.thrift.ThriftConstants;
import com.navercorp.pinpoint.thrift.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;

@Service
public class SpanServiceImpl implements SpanService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AgentInfoHandler agentInfoHandler;

    @Autowired
    private AgentLifeCycleDao agentLifeCycleDao;

    @Autowired
    private AgentStatHandlerV2 agentStatHandlerV2;

    @Autowired
    private SpanHandler spanHandler;

    @Override
    public void insertAgent(AgentVO agentVO) {
        logger.info("create - " + agentVO);
        String agentId = agentVO.getAppName() + "-" + agentVO.getIpAddress();
        long start = System.currentTimeMillis();
        TAgentInfo agent = new TAgentInfo();
        agent.setApplicationName(agentVO.getAppName());
        agent.setAgentId(agentId);
        agent.setIp(agentVO.getIpAddress());
        agent.setHostname(agentVO.getHostname());
//        agent.setServiceType(Short.valueOf("1210"));
        agent.setServiceType(SpringBootConstants.SERVICE_TYPE.getCode());
        agent.setAgentVersion("1.0.0.zp");
        agent.setVmVersion(agentVO.getVersion());
        agent.setStartTimestamp(start);

        TJvmInfo jvmInfo = new TJvmInfo();
        jvmInfo.setVmVersion(agentVO.getVersion());
        jvmInfo.setGcType(getGcType(agentVO.getGcType()));
        agent.setJvmInfo(jvmInfo);

        this.agentInfoHandler.handleSimple(agent);
        long id = (long) (Math.random() * 1000000000) + 1000000000;
        AgentLifeCycleBo agentLifeCycleBo = new AgentLifeCycleBo(agentId, start, start + 10000, id, AgentLifeCycleState.RUNNING);
        this.agentLifeCycleDao.insert(agentLifeCycleBo);

        TJvmGc jvmGc = new TJvmGc();
        jvmGc.setType(getGcType(agentVO.getGcType()));
        jvmGc.setJvmMemoryHeapUsed(parseLong(agentVO.getHeapUsed()));
        jvmGc.setJvmMemoryHeapMax(parseLong(agentVO.getHeapMax()));
        jvmGc.setJvmMemoryNonHeapUsed(parseLong(agentVO.getNonHeapUsed()));
        jvmGc.setJvmMemoryNonHeapMax(parseLong(agentVO.getNonHeapMax()));
        jvmGc.setJvmGcOldCount(parseLong(agentVO.getGcOldCount()));
        jvmGc.setJvmGcOldTime(parseLong(agentVO.getGcOldTime()));

        // cpuload
        TCpuLoad cpuLoad = new TCpuLoad();
        cpuLoad.setJvmCpuLoad(parseLong(agentVO.getJvmCpuLoad()));
        cpuLoad.setSystemCpuLoad(parseLong(agentVO.getSystemCpuLoad()));

        // transaction

        // activeTrace
        TActiveTrace activeTrace = new TActiveTrace();
        // dataSourceList

        TAgentStat agentStat = new TAgentStat();
        agentStat.setAgentId(agentId);
        agentStat.setStartTimestamp(start - 15000);
        agentStat.setTimestamp(start);
        agentStat.setCollectInterval(15000);
        agentStat.setTransaction(new TTransaction());
        agentStat.setCpuLoad(cpuLoad);
        agentStat.setGc(jvmGc);
        agentStat.setActiveTrace(activeTrace);

        TAgentStatBatch agentStatBatch = new TAgentStatBatch();
        agentStatBatch.setAgentId(agentId);
        agentStatBatch.setStartTimestamp(start - 15000);
        agentStatBatch.setAgentStats(Arrays.asList(agentStat));
        this.agentStatHandlerV2.handleSimple(agentStatBatch);
    }

    @Override
    public void insertHttp(SpanVO vo) {
        String agentId = vo.getAppName() + "-" + vo.getIpAddress();
        long now = System.currentTimeMillis();
        byte[] transactionId = TransactionIdUtils.formatBytes(agentId, now, 1);

        TSpan span = new TSpan();
        span.setAgentId(agentId);
        span.setApplicationName(vo.getAppName());
        span.setTransactionId(transactionId);
        span.setStartTime(now);
        span.setAgentStartTime(now);
        span.setRpc(vo.getUrl());
        span.setSpanId(span.hashCode());
        span.setParentSpanId(-1);
        span.setServiceType(ThriftConstants.THRIFT_SERVER.getCode());
        span.setApplicationServiceType(SpringBootConstants.SERVICE_TYPE.getCode());

        span.setTotal(parseLong(vo.getTotal()));

        span.setSpanEventList(new ArrayList<TSpanEvent>());

        TSpanEvent event1 = new TSpanEvent();
        event1.setServiceType(JdkHttpConstants.SERVICE_TYPE.getCode());
        event1.setDestinationId(vo.getDomain());
        event1.setSequence((short) 1);
        // 0是没错误的
        if ("2xx".equals(vo.getStatus())) {
        } else {
            TIntStringValue exception = new TIntStringValue();
            exception.setStringValue(vo.getStatus());
            event1.setExceptionInfo(exception);
        }

        TAnnotation tAnnotation = new TAnnotation();
        tAnnotation.setKey(event1.hashCode());
        tAnnotation.setValue(TAnnotationValue.stringValue("http://" + vo.getDomain()));
        event1.setAnnotations(new ArrayList<TAnnotation>());
        event1.getAnnotations().add(tAnnotation);

        span.getSpanEventList().add(event1);
        this.spanHandler.handleSimple(span);
    }

    @Override
    public void insertDb(SpanVO vo) {
        String agentId = vo.getAppName() + "-" + vo.getIpAddress();
        long now = System.currentTimeMillis();
        byte[] transactionId = TransactionIdUtils.formatBytes(agentId, now, 1);

        TSpan span = new TSpan();
        span.setAgentId(agentId);
        span.setApplicationName(vo.getAppName());
        span.setTransactionId(transactionId);
        span.setStartTime(now);
        span.setAgentStartTime(now);
        span.setSpanId(span.hashCode());
        span.setParentSpanId(-1);
        span.setServiceType(ThriftConstants.THRIFT_SERVER.getCode());
        span.setApplicationServiceType(SpringBootConstants.SERVICE_TYPE.getCode());
        span.setSpanEventList(new ArrayList<TSpanEvent>());

        // 0是没错误的
//        if ("true".equals(vo.getStatus())) {
//            span.setErr(0);
//        } else {
//            span.setErr(1);
//        }
        span.setTotal(parseLong(vo.getTotal()));

        TSpanEvent event1 = new TSpanEvent();
        event1.setServiceType(MySqlConstants.MYSQL.getCode());
        event1.setSequence((short) 1);
        event1.setDestinationId(vo.getDbName());
        event1.setEndPoint(vo.getDbIp());

        TSpanEvent event2 = new TSpanEvent();
        event2.setServiceType(MySqlConstants.MYSQL_EXECUTE_QUERY.getCode());
        event2.setSequence((short) 2);
        event2.setDestinationId(vo.getDbName());
        event2.setEndPoint(vo.getDbIp());

        // 0是没错误的
        if ("true".equals(vo.getStatus())) {
        } else {
            TIntStringValue exception = new TIntStringValue();
            exception.setStringValue(vo.getStatus());
            event2.setExceptionInfo(exception);
        }

        span.getSpanEventList().add(event1);
        span.getSpanEventList().add(event2);
        this.spanHandler.handleSimple(span);
    }

    @Override
    public void insertRedis(SpanVO vo) {
        String agentId = vo.getAppName() + "-" + vo.getIpAddress();

        long now = System.currentTimeMillis();
        byte[] transactionId = TransactionIdUtils.formatBytes(agentId, now, 1);

        TSpan span = new TSpan();
        span.setAgentId(agentId);
        span.setApplicationName(vo.getAppName());
        span.setTransactionId(transactionId);
        span.setStartTime(now);
        span.setAgentStartTime(now);
        span.setSpanId(span.hashCode());
        span.setParentSpanId(-1);
        span.setServiceType(ThriftConstants.THRIFT_SERVER.getCode());
        span.setApplicationServiceType(SpringBootConstants.SERVICE_TYPE.getCode());
        span.setSpanEventList(new ArrayList<TSpanEvent>());

        // 0是没错误的
//        if ("NORMAL".equals(vo.getStatus())) {
//            span.setErr(0);
//        } else {
//            span.setErr(1);
//        }
        span.setTotal(parseLong(vo.getTotal()));

        TSpanEvent event1 = new TSpanEvent();
        event1.setServiceType(RedisConstants.REDIS.getCode());
        event1.setSequence((short) 1);
        event1.setDestinationId("redis");
        event1.setEndPoint(vo.getRedisIp());
        // 0是没错误的
        if ("NORMAL".equals(vo.getStatus())) {
        } else {
            TIntStringValue exception = new TIntStringValue();
            exception.setStringValue(vo.getStatus());
            event1.setExceptionInfo(exception);
        }
        span.getSpanEventList().add(event1);
        this.spanHandler.handleSimple(span);
    }

    @Override
    public void insertRpcProvider(SpanVO spanVO) {
        String agentId = spanVO.getAppName() + "-" + spanVO.getIpAddress();
        long now = System.currentTimeMillis();
        byte[] transactionId = TransactionIdUtils.formatBytes(agentId, now, 1);

        TSpan span = new TSpan();
        span.setAgentId(agentId);
        span.setApplicationName(spanVO.getAppName());
        span.setTransactionId(transactionId);
        span.setStartTime(now);
        span.setAgentStartTime(now);
        span.setSpanId(span.hashCode());
        span.setParentSpanId(-1);
        span.setEndPoint(spanVO.getIpAddress());
        span.setAcceptorHost(spanVO.getIpAddress());
        span.setRpc(spanVO.getService() + ":" + spanVO.getMethod());
        span.setServiceType(ThriftConstants.THRIFT_SERVER.getCode());
        span.setApplicationServiceType(SpringBootConstants.SERVICE_TYPE.getCode());

        // 0是没错误的
        span.setErr(0);
        span.setTotal(parseLong(spanVO.getTotal()));

        span.setParentApplicationName(spanVO.getClientAppName());
        span.setRemoteAddr(spanVO.getClientIp());
        span.setParentApplicationType(SpringBootConstants.SERVICE_TYPE.getCode());
        this.spanHandler.handleSimple(span);
    }

    @Override
    public void insertRpcClient(SpanVO vo) {
        String agentId = vo.getAppName() + "-" + vo.getIpAddress();
        long now = System.currentTimeMillis();
        byte[] transactionId = TransactionIdUtils.formatBytes(agentId, now, 1);

        TSpan span = new TSpan();
        span.setAgentId(agentId);
        span.setApplicationName(vo.getAppName());
        span.setTransactionId(transactionId);
        span.setStartTime(now);
        span.setAgentStartTime(now);
        span.setSpanId(span.hashCode());
        span.setParentSpanId(-1);
        span.setEndPoint(vo.getIpAddress());
        span.setAcceptorHost(vo.getIpAddress());

        // 0是没错误的
//        if ("0".equals(vo.getStatus())) {
//            span.setErr(0);
//        } else {
//            span.setErr(1);
//        }
        span.setTotal(parseLong(vo.getTotal()));

//        span.setRpc(service + ":" + method);
        span.setServiceType(ThriftConstants.THRIFT_CLIENT.getCode());
        span.setApplicationServiceType(SpringBootConstants.SERVICE_TYPE.getCode());
        span.setSpanEventList(new ArrayList<TSpanEvent>());

        TSpanEvent event1 = new TSpanEvent();
        event1.setServiceType(SpringBootConstants.SERVICE_TYPE.getCode());
        event1.setRpc(vo.getService() + ":" + vo.getMethod());
        event1.setSequence((short) 1);
        event1.setDestinationId(vo.getProviderAppName());
        event1.setEndPoint(vo.getProviderIp());
        // 0是没错误的
        if ("0".equals(vo.getStatus())) {
        } else {
            TIntStringValue exception = new TIntStringValue();
            exception.setStringValue(vo.getStatus());
            event1.setExceptionInfo(exception);
        }
        span.getSpanEventList().add(event1);
        this.spanHandler.handleSimple(span);
    }

    public static TJvmGcType getGcType(String gcType) {
        TJvmGcType jvmGcType;
        if ("ParNew".equals(gcType) || "ConcurrentMarkSweep".equals(gcType)) {
            jvmGcType = TJvmGcType.CMS;
        } else if ("PS Scavenge".equals(gcType) || "PS MarkSweep".equals(gcType)) {
            jvmGcType = TJvmGcType.PARALLEL;
        } else if ("MarkSweepCompact".equals(gcType) || "Copy".equals(gcType)) {
            jvmGcType = TJvmGcType.SERIAL;
        } else if ("G1 Young Generation".equals(gcType) || "G1 Old Generation".equals(gcType)) {
            jvmGcType = TJvmGcType.G1;
        } else {
            jvmGcType = TJvmGcType.UNKNOWN;
        }
        return jvmGcType;
    }

    public static long parseLong(String val) {
        try {
            return (long) Double.parseDouble(val);
        } catch (Exception e) {
            return 0;
        }
    }
}
