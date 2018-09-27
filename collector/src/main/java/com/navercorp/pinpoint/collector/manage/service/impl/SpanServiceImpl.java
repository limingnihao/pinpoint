package com.navercorp.pinpoint.collector.manage.service.impl;

import com.navercorp.pinpoint.collector.dao.AgentLifeCycleDao;
import com.navercorp.pinpoint.collector.dao.SqlMetaDataDao;
import com.navercorp.pinpoint.collector.dao.StringMetaDataDao;
import com.navercorp.pinpoint.collector.dao.hbase.HbaseApiMetaDataDao;
import com.navercorp.pinpoint.collector.handler.AgentInfoHandler;
import com.navercorp.pinpoint.collector.handler.AgentStatHandlerV2;
import com.navercorp.pinpoint.collector.handler.SpanHandler;
import com.navercorp.pinpoint.collector.manage.service.SpanService;
import com.navercorp.pinpoint.collector.manage.vo.AgentVO;
import com.navercorp.pinpoint.collector.manage.vo.SpanVO;
import com.navercorp.pinpoint.common.server.bo.AgentLifeCycleBo;
import com.navercorp.pinpoint.common.server.util.AcceptedTimeService;
import com.navercorp.pinpoint.common.server.util.AgentLifeCycleState;
import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.common.trace.MethodType;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.util.TransactionIdUtils;
import com.navercorp.pinpoint.plugin.jdbc.mysql.MySqlConstants;
import com.navercorp.pinpoint.plugin.jdk.http.JdkHttpConstants;
import com.navercorp.pinpoint.plugin.redis.RedisConstants;
import com.navercorp.pinpoint.plugin.spring.boot.SpringBootConstants;
import com.navercorp.pinpoint.plugin.thrift.ThriftConstants;
import com.navercorp.pinpoint.plugin.tomcat.TomcatConstants;
import com.navercorp.pinpoint.thrift.dto.*;
import org.apache.commons.lang3.StringUtils;
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
    private HbaseApiMetaDataDao hbaseApiMetaDataDao;

    @Autowired
    private SpanHandler spanHandler;

    @Autowired
    private AcceptedTimeService acceptedTimeService;

    @Autowired
    private StringMetaDataDao stringMetaDataDao;

    @Autowired
    private SqlMetaDataDao sqlMetaDataDao;

    @Override
    public void insertAgent(AgentVO agentVO) {
        logger.info("create - " + agentVO);
        String agentId = getAgentId(agentVO.getAppName(), agentVO.getIpAddress());
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

        this.acceptedTimeService.accept();
    }

    @Override
    public void insertUser(SpanVO vo) {
        long agentTime = this.acceptedTimeService.getAcceptedTime();
        logger.info("insertUser - " + vo + ", agentTime=" + agentTime);
        String agentId = getAgentId(vo.getAppName(), vo.getIpAddress());
        long now = parseLong(vo.getStartTime(), System.currentTimeMillis());
        int elapsed = parseInt(vo.getElapsed(), 0);

        String service = vo.getService() + "." + vo.getMethod() + "()";
        TApiMetaData api = new TApiMetaData();
        api.setAgentId(agentId);
        api.setAgentStartTime(agentTime);
        api.setApiId(service.hashCode());
        api.setApiInfo(service);
        this.hbaseApiMetaDataDao.insert(api);

//        byte[] transactionId = TransactionIdUtils.formatBytes(agentId, now, 1);
        byte[] transactionId = TransactionIdUtils.formatBytes(vo.getTraceId(), 0, 0);
        long parentSpanId = -1;
        long spanId = vo.getSpanId().hashCode();

        TSpan span = new TSpan();
        span.setAgentId(agentId);
        span.setApplicationName(vo.getAppName());
        span.setTransactionId(transactionId);
        span.setSpanId(spanId);
        span.setParentSpanId(parentSpanId);
        span.setApiId(api.getApiId());
        span.setEndPoint(vo.getIpAddress());

        span.setStartTime(now);
        span.setAgentStartTime(agentTime);
        span.setElapsed(elapsed);

        span.setRpc(vo.getUrl());
        span.setServiceType(ServiceType.USER.getCode());
        span.setApplicationServiceType(SpringBootConstants.SERVICE_TYPE.getCode());
        span.setRemoteAddr("http " + vo.getIpAddress());
        this.spanHandler.handleSimple(span);

    }

    @Override
    public void insertHttp(SpanVO vo) {
        logger.info("insertHttp - " + vo);

        long agentTime = this.acceptedTimeService.getAcceptedTime();
        String agentId = getAgentId(vo.getAppName(), vo.getIpAddress());
        long now = parseLong(vo.getStartTime(), System.currentTimeMillis());
        int elapsed = parseInt(vo.getElapsed(), 0);

        //        byte[] transactionId = TransactionIdUtils.formatBytes(agentId, now, 1);
        byte[] transactionId = TransactionIdUtils.formatBytes(vo.getTraceId(), 0, 0);

        long parentSpanId = vo.getParentSpanId().hashCode();
        long spanId = vo.getSpanId().hashCode();

        TApiMetaData api = null;
        String apiInfo = null;
        if (StringUtils.isNotEmpty(vo.getService()) && StringUtils.isNotEmpty(vo.getMethod())) {
            apiInfo = vo.getService() + "." + vo.getMethod() + "()";
        } else {
            apiInfo = "com.zhaopin.common3.http.HttpUtils.query()";
        }
        api = new TApiMetaData();
        api.setAgentId(agentId);
        api.setAgentStartTime(agentTime);
        api.setApiId(apiInfo.hashCode());
        api.setApiInfo(apiInfo);
        this.hbaseApiMetaDataDao.insert(api);

        TSpan span = new TSpan();
        span.setAgentId(agentId);
        span.setApplicationName(vo.getAppName());
        span.setTransactionId(transactionId);
        span.setSpanId(spanId <= 0 ? vo.hashCode() : spanId);
        span.setParentSpanId(parentSpanId);
        span.setRpc(vo.getService() + "." + vo.getMethod());

        span.setStartTime(now);
        span.setAgentStartTime(agentTime);
        span.setElapsed(elapsed);
        span.setServiceType(ThriftConstants.THRIFT_SERVER.getCode());
        span.setApplicationServiceType(SpringBootConstants.SERVICE_TYPE.getCode());
        span.setSpanEventList(new ArrayList<TSpanEvent>());

        span.setTotal(parseLong(vo.getTotal()));
        if (api != null) {
            span.setApiId(api.getApiId());
        }

        // api
//        TApiMetaData api2 = new TApiMetaData();
//        api2.setAgentId(agentId);
//        api2.setAgentStartTime(agentTime);
//        api2.setApiInfo("HTTP.proxy()");
//        api2.setApiId(api2.getApiInfo().hashCode());
//        this.hbaseApiMetaDataDao.insert(api2);

        // http
        TAnnotation tAnnotation = new TAnnotation();
        tAnnotation.setKey(AnnotationKey.API.getCode());
        tAnnotation.setValue(TAnnotationValue.stringValue(vo.getDomain() + "" + vo.getUrl()));

        TSpanEvent event1 = new TSpanEvent();
        event1.setServiceType(JdkHttpConstants.SERVICE_TYPE.getCode());
        event1.setDestinationId(vo.getDomain());
        event1.setEndPoint(vo.getDomain());
        event1.setNextSpanId(-1);
        event1.setSequence((short) 0);
        event1.setDepth(1);
        event1.setApiId(0);
        event1.setAnnotations(new ArrayList<TAnnotation>());
        event1.getAnnotations().add(tAnnotation);
//
//        TSpanEvent event2 = new TSpanEvent();
//        event2.setServiceType(MySqlConstants.MYSQL_EXECUTE_QUERY.getCode());
//        event2.setSequence((short) 1);
//        event1.setDestinationId(vo.getDomain());
//        event1.setEndPoint(vo.getDomain());
//        event2.setNextSpanId(-1);
//        event2.setApiId(api2.getApiId());
//        event2.setDepth(1);
//        event2.setAnnotations(new ArrayList<>());

        // 0是没错误的
        if ("2xx".equals(vo.getStatus())) {
        } else {
            TIntStringValue exception = new TIntStringValue();
            exception.setStringValue(vo.getStatus());
            event1.setExceptionInfo(exception);
        }

        span.getSpanEventList().add(event1);

        this.spanHandler.handleSimple(span);
    }

    @Override
    public void insertDb(SpanVO vo) {
        logger.info("insertDb - " + vo);
        long agentTime = this.acceptedTimeService.getAcceptedTime();
        String agentId = getAgentId(vo.getAppName(), vo.getIpAddress());
        long now = parseLong(vo.getStartTime(), System.currentTimeMillis());
        int elapsed = parseInt(vo.getElapsed(), 0);

//        byte[] transactionId = TransactionIdUtils.formatBytes(agentId, now, 1);
        byte[] transactionId = TransactionIdUtils.formatBytes(vo.getTraceId(), 0, 0);

        long parentSpanId = vo.getParentSpanId().hashCode();
        long spanId = vo.getSpanId().hashCode();

        TApiMetaData api = null;
        if (StringUtils.isNotEmpty(vo.getService()) && StringUtils.isNotEmpty(vo.getMethod())) {
            String apiInfo = vo.getService() + "." + vo.getMethod() + "()";
            api = new TApiMetaData();
            api.setAgentId(agentId);
            api.setAgentStartTime(agentTime);
            api.setApiId(apiInfo.hashCode());
            api.setApiInfo(apiInfo);
            this.hbaseApiMetaDataDao.insert(api);
        }

        TSpan span = new TSpan();
        span.setAgentId(agentId);
        span.setApplicationName(vo.getAppName());
        span.setTransactionId(transactionId);
        span.setSpanId(spanId <= 0 ? vo.hashCode() : spanId);
        span.setParentSpanId(parentSpanId);
        span.setRpc(vo.getService() + "." + vo.getMethod());

        span.setStartTime(now);
        span.setAgentStartTime(agentTime);
        span.setServiceType(ThriftConstants.THRIFT_SERVER.getCode());
        span.setApplicationServiceType(SpringBootConstants.SERVICE_TYPE.getCode());
        span.setSpanEventList(new ArrayList<TSpanEvent>());
        span.setElapsed(elapsed);
        span.setTotal(parseLong(vo.getTotal()));
        if (api != null) {
            span.setApiId(api.getApiId());
        }

        TSpanEvent event1 = new TSpanEvent();
        event1.setServiceType(MySqlConstants.MYSQL.getCode());
        event1.setSequence((short) 0);
        event1.setDestinationId(vo.getDbName());
        event1.setEndPoint(vo.getDbIp());
        event1.setNextSpanId(-1);
        event1.setApiId(0);
        event1.setDepth(1);
//        event1.setStartElapsed(100);
//        event1.setEndElapsed(200);
        event1.setAnnotations(new ArrayList<>());

        // api
        TApiMetaData api2 = new TApiMetaData();
        api2.setAgentId(agentId);
        api2.setAgentStartTime(agentTime);
        api2.setApiInfo("Jdbc.jdbcProxy()");
        api2.setApiId(api2.getApiInfo().hashCode());
        this.hbaseApiMetaDataDao.insert(api2);

        TSpanEvent event2 = new TSpanEvent();
        event2.setServiceType(MySqlConstants.MYSQL_EXECUTE_QUERY.getCode());
        event2.setSequence((short) 1);
        event2.setDestinationId(vo.getDbName());
        event2.setEndPoint(vo.getDbIp());
        event2.setNextSpanId(-1);
        event2.setApiId(api2.getApiId());
//        event2.setEndElapsed(200);
        event2.setDepth(1);
        event2.setAnnotations(new ArrayList<>());

        // jdbc
        TAnnotationValue jdbc_value = new TAnnotationValue();
        jdbc_value.setIntValue(MethodType.ANNOTATION);
        jdbc_value.setStringValue(vo.getDbUrl());

        TAnnotation jdbc_annotation = new TAnnotation();
        jdbc_annotation.setKey(AnnotationKey.API.getCode());
        jdbc_annotation.setValue(jdbc_value);
        event1.getAnnotations().add(jdbc_annotation);

        // sql
        if (vo.getDbSql() != null && !"".equals(vo.getDbSql())) {
            TSqlMetaData sql_MetaDataBo = new TSqlMetaData();
            sql_MetaDataBo.setAgentId(agentId);
            sql_MetaDataBo.setAgentStartTime(agentTime);
            sql_MetaDataBo.setSql(vo.getDbSql());
            sql_MetaDataBo.setSqlId(sql_MetaDataBo.getSql().hashCode());
            this.sqlMetaDataDao.insert(sql_MetaDataBo);

            TIntStringStringValue sql_intString = new TIntStringStringValue();
            sql_intString.setIntValue(sql_MetaDataBo.getSqlId());
            sql_intString.setStringValue1(sql_MetaDataBo.getSql());

            TAnnotationValue sql_value = new TAnnotationValue();
//            sql_value.setIntValue(MethodType.ANNOTATION);
            sql_value.setIntStringStringValue(sql_intString);

            TAnnotation sql_annotation = new TAnnotation();
            sql_annotation.setKey(AnnotationKey.SQL_ID.getCode());
            sql_annotation.setValue(sql_value);
            event2.getAnnotations().add(sql_annotation);
        }


        // 0是没错误的
        if ("true".equals(vo.getStatus()) || "0".equals(vo.getStatus())) {
        } else {
            TStringMetaData metaData = new TStringMetaData();
            metaData.setAgentId(agentId);
            metaData.setAgentStartTime(agentTime);
            metaData.setStringValue("jdbc");
            metaData.setStringId(metaData.getStringValue().hashCode());
            this.stringMetaDataDao.insert(metaData);

            TIntStringValue exception = new TIntStringValue();
            exception.setIntValue(metaData.getStringId());
            exception.setStringValue(vo.getStatus());
            event2.setExceptionInfo(exception);
        }
        if (StringUtils.isNotEmpty(vo.getDbName())) {
            span.getSpanEventList().add(event1);
        }
        span.getSpanEventList().add(event2);
        this.spanHandler.handleSimple(span);
    }

    @Override
    public void insertRedis(SpanVO vo) {
        logger.info("insertRedis - " + vo);

        long agentTime = this.acceptedTimeService.getAcceptedTime();
        String agentId = getAgentId(vo.getAppName(), vo.getIpAddress());
        long now = parseLong(vo.getStartTime(), System.currentTimeMillis());
        int elapsed = parseInt(vo.getElapsed(), 0);

        //        byte[] transactionId = TransactionIdUtils.formatBytes(agentId, now, 1);
        byte[] transactionId = TransactionIdUtils.formatBytes(vo.getTraceId(), 0, 0);

        long parentSpanId = vo.getParentSpanId().hashCode();
        long spanId = vo.getSpanId().hashCode();

        TApiMetaData api = null;
        if (StringUtils.isNotEmpty(vo.getService()) && StringUtils.isNotEmpty(vo.getMethod())) {
            String apiInfo = vo.getService() + "." + vo.getMethod() + "()";
            api = new TApiMetaData();
            api.setAgentId(agentId);
            api.setAgentStartTime(agentTime);
            api.setApiId(apiInfo.hashCode());
            api.setApiInfo(apiInfo);
            this.hbaseApiMetaDataDao.insert(api);
        }

        TSpan span = new TSpan();
        span.setAgentId(agentId);
        span.setApplicationName(vo.getAppName());
        span.setTransactionId(transactionId);
        span.setSpanId(spanId <= 0 ? vo.hashCode() : spanId);
        span.setParentSpanId(parentSpanId);
        span.setRpc(vo.getService() + "." + vo.getMethod());

        span.setStartTime(now);
        span.setAgentStartTime(agentTime);
        span.setElapsed(elapsed);
        span.setServiceType(ThriftConstants.THRIFT_SERVER.getCode());
        span.setApplicationServiceType(SpringBootConstants.SERVICE_TYPE.getCode());
        span.setSpanEventList(new ArrayList<TSpanEvent>());

        span.setTotal(parseLong(vo.getTotal()));
        if (api != null) {
            span.setApiId(api.getApiId());
        }

        // redis connect
        TAnnotationValue conn_value = new TAnnotationValue();
        conn_value.setStringValue(vo.getRedisAddress());

        TAnnotation conn_annotation = new TAnnotation();
        conn_annotation.setKey(AnnotationKey.API.getCode());
        conn_annotation.setValue(conn_value);


        // redis connect
        TAnnotationValue method_value = new TAnnotationValue();
        method_value.setStringValue(vo.getRedisMethod());

        TAnnotation method_annotation = new TAnnotation();
        method_annotation.setKey(AnnotationKey.API.getCode());
        method_annotation.setValue(method_value);


        TSpanEvent event1 = new TSpanEvent();
        event1.setServiceType(RedisConstants.REDIS.getCode());
        event1.setSequence((short) 0);
        event1.setDestinationId(vo.getDbName());
        event1.setEndPoint(vo.getRedisAddress());
        event1.setDestinationId(vo.getRedisAddress());
        event1.setNextSpanId(-1);
        event1.setApiId(0);
        event1.setDepth(1);
        event1.setAnnotations(new ArrayList<>());
        event1.getAnnotations().add(conn_annotation);
        event1.getAnnotations().add(method_annotation);


        // 0是没错误的
        if ("info".equals(vo.getStatus())) {
        } else {
            TStringMetaData metaData = new TStringMetaData();
            metaData.setAgentId(agentId);
            metaData.setAgentStartTime(agentTime);
            metaData.setStringValue("redis");
            metaData.setStringId(metaData.getStringValue().hashCode());
            this.stringMetaDataDao.insert(metaData);

            TIntStringValue exception = new TIntStringValue();
            exception.setIntValue(metaData.getStringId());
            exception.setStringValue(vo.getStatus());
            event1.setExceptionInfo(exception);
        }
        span.getSpanEventList().add(event1);
        this.spanHandler.handleSimple(span);
    }

    @Override
    public void insertRpcProvider(SpanVO vo) {
        logger.info("insertRpcProvider - " + vo);
        long agentTime = this.acceptedTimeService.getAcceptedTime();

        String agentId = getAgentId(vo.getAppName(), vo.getIpAddress());

        long now = parseLong(vo.getStartTime(), System.currentTimeMillis());
        int elapsed = parseInt(vo.getElapsed(), 0);

        String url = vo.getService() + "." + vo.getMethod() + "()";
        TApiMetaData api = new TApiMetaData();
        api.setAgentId(agentId);
        api.setAgentStartTime(agentTime);
        api.setApiId(url.hashCode());
        api.setApiInfo(url);
        this.hbaseApiMetaDataDao.insert(api);

//        byte[] transactionId = TransactionIdUtils.formatBytes(agentId, now, 1);
        byte[] transactionId = TransactionIdUtils.formatBytes(vo.getTraceId(), 0, 0);
        long parentSpanId = "0".equals(vo.getParentSpanId()) ? -1 : vo.getParentSpanId().hashCode();
        long spanId = vo.getSpanId().hashCode();

        TSpan span = new TSpan();
        span.setAgentId(agentId);
        span.setApplicationName(vo.getAppName());
        span.setTransactionId(transactionId);
        span.setSpanId(spanId);
        span.setParentSpanId(parentSpanId);
        span.setApiId(api.getApiId());

        span.setStartTime(now);
        span.setAgentStartTime(agentTime);
        span.setElapsed(elapsed);
        span.setEndPoint(vo.getIpAddress());
        span.setAcceptorHost(vo.getIpAddress());
        span.setRpc(url);
        span.setServiceType(ThriftConstants.THRIFT_SERVER.getCode());
        span.setApplicationServiceType(SpringBootConstants.SERVICE_TYPE.getCode());

        // 0是没错误的
        span.setErr(0);
        span.setTotal(parseLong(vo.getTotal()));

        span.setParentApplicationName(vo.getClientAppName());
        span.setRemoteAddr(vo.getRemoteType() + " " + vo.getIpAddress());
        span.setParentApplicationType(SpringBootConstants.SERVICE_TYPE.getCode());
        this.spanHandler.handleSimple(span);
    }

    @Override
    public void insertRpcClient(SpanVO vo) {
        logger.info("insertRpcClient - " + vo);

        long agentTime = this.acceptedTimeService.getAcceptedTime();

        String agentId = getAgentId(vo.getAppName(), vo.getIpAddress());
        long now = System.currentTimeMillis();

        String url = vo.getService() + "." + vo.getMethod() + "()";
        TApiMetaData api = new TApiMetaData();
        api.setAgentId(agentId);
        api.setAgentStartTime(agentTime);
        api.setApiId(url.hashCode());
        api.setApiInfo(url);
        this.hbaseApiMetaDataDao.insert(api);

//        byte[] transactionId = TransactionIdUtils.formatBytes(agentId, now, 1);
        byte[] transactionId = TransactionIdUtils.formatBytes(agentId, 0, vo.getTraceId().hashCode());
        long parentSpanId = "0".equals(vo.getParentSpanId()) ? -1 : vo.getParentSpanId().hashCode();
        long spanId = vo.getSpanId().hashCode();

        TSpan span = new TSpan();
        span.setAgentId(agentId);
        span.setApplicationName(vo.getAppName());

        span.setTransactionId(transactionId);
        span.setSpanId(spanId);
        span.setParentSpanId(parentSpanId);

        span.setStartTime(now);
        span.setAgentStartTime(this.acceptedTimeService.getAcceptedTime());
        span.setEndPoint(vo.getIpAddress());
        span.setAcceptorHost(vo.getIpAddress());
        if (api != null) {
            span.setApiId(api.getApiId());
        }

        // 0是没错误的
//        if ("0".equals(vo.getStatus())) {
//            span.setErr(0);
//        } else {
//            span.setErr(1);
//        }
        span.setTotal(parseLong(vo.getTotal()));
        span.setRpc(vo.getUrl());
//        span.setRpc(service + ":" + method);
        span.setServiceType(TomcatConstants.TOMCAT.getCode());
        span.setApplicationServiceType(SpringBootConstants.SERVICE_TYPE.getCode());//1210
        span.setSpanEventList(new ArrayList<TSpanEvent>());


        String method = vo.getService() + "." + vo.getMethod();
        TApiMetaData api2 = new TApiMetaData();
        api2.setAgentId(agentId);
        api2.setAgentStartTime(this.acceptedTimeService.getAcceptedTime());
        api2.setApiId(method.hashCode());
        api2.setApiInfo(method + "()");
        this.hbaseApiMetaDataDao.insert(api2);

        TSpanEvent event1 = new TSpanEvent();
        event1.setServiceType(ThriftConstants.THRIFT_CLIENT.getCode());
//        event1.setServiceType(DubboConstants.DUBBO_CONSUMER_SERVICE_TYPE.getCode());
        event1.setRpc(method);
        event1.setSequence((short) 0);
        event1.setDestinationId(vo.getProviderIp());
        event1.setEndPoint(vo.getProviderIp());
        event1.setApiId(api2.getApiId());

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
        return parseLong(val, -1);
    }

    public static long parseLong(String val, long def) {
        try {
            return Long.parseLong(val);
        } catch (Exception e) {
            return def;
        }
    }

    public static int parseInt(String val, int def) {
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return def;
        }
    }

    public static String getAgentId(String name, String ip) {
        return name + "^" + ip;
    }
}
