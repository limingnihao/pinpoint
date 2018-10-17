package com.navercorp.pinpoint.collector.http.handler.service.impl;

import com.navercorp.pinpoint.collector.dao.AgentLifeCycleDao;
import com.navercorp.pinpoint.collector.dao.SqlMetaDataDao;
import com.navercorp.pinpoint.collector.dao.StringMetaDataDao;
import com.navercorp.pinpoint.collector.dao.hbase.HbaseApiMetaDataDao;
import com.navercorp.pinpoint.collector.handler.AgentInfoHandler;
import com.navercorp.pinpoint.collector.handler.AgentStatHandlerV2;
import com.navercorp.pinpoint.collector.handler.SpanHandler;
import com.navercorp.pinpoint.collector.http.handler.service.SpanService;
import com.navercorp.pinpoint.collector.http.handler.vo.AgentVO;
import com.navercorp.pinpoint.collector.http.handler.vo.SpanVO;
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
import java.util.List;
import java.util.stream.Collectors;

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

    public void check(SpanVO vo) throws Exception {
        if (vo.getSpanId() == null || StringUtils.isEmpty(vo.getSpanId()) || "null".equals(vo.getSpanId()) || "".equals(vo.getSpanId())) {
            throw new Exception("error");
        }
        if (vo.getTraceId() == null || StringUtils.isEmpty(vo.getTraceId()) || "null".equals(vo.getTraceId()) || "".equals(vo.getTraceId())) {
            throw new Exception("error");
        }
        if (vo.getAppName() == null || StringUtils.isEmpty(vo.getAppName()) || "null".equals(vo.getAppName()) || "".equals(vo.getAppName())) {
            throw new Exception("error");
        }
        if (vo == null) {
            throw new Exception("error");
        }
    }

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

        this.agentInfoHandler.handleRequest(agent);
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
    public void insertUser(SpanVO vo) throws Exception {
        logger.info("insertUser - " + vo);
        this.check(vo);
        long agentTime = this.acceptedTimeService.getAcceptedTime();
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
        span.setRpc(vo.getHttpUrl());

        span.setStartTime(now);
        span.setAgentStartTime(agentTime);
        span.setElapsed(elapsed);

        span.setServiceType(ServiceType.USER.getCode());
        span.setApplicationServiceType(SpringBootConstants.SERVICE_TYPE.getCode());
        span.setRemoteAddr(vo.getIpAddress());
        span.setAnnotations(new ArrayList());

        TAnnotation status = new TAnnotation();
        status.setKey(AnnotationKey.HTTP_STATUS_CODE.getCode());
        status.setValue(TAnnotationValue.stringValue(vo.getStatus()));
        span.getAnnotations().add(status);

        if (StringUtils.isNotEmpty(vo.getParams())) {
            TAnnotation params = new TAnnotation();
            params.setKey(AnnotationKey.HTTP_PARAM.getCode());
            params.setValue(TAnnotationValue.stringValue(vo.getParams()));
            span.getAnnotations().add(params);
        }
        this.spanHandler.handleSimple(span);
    }

    @Override
    public void insertHttp(SpanVO vo) throws Exception {
        logger.info("insertHttp - " + vo);
        this.check(vo);

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
        span.setSpanEventList(new ArrayList<>());
        if (api != null) {
            span.setApiId(api.getApiId());
        }

        String tempUrl = vo.getHttpUrl().replace("http://", "").replace("https://", "");
        String domain = vo.getHttpUrl();
        List<String> list = Arrays.asList(tempUrl.split("[/? ]")).stream().filter(v -> !v.equals("")).collect(Collectors.toList());
        if (list != null && list.size() > 0) {
            domain = list.get(0);
        }
        String uri = tempUrl.replace(domain, "");
        TSpanEvent event1 = new TSpanEvent();
        event1.setServiceType(JdkHttpConstants.SERVICE_TYPE.getCode());
        event1.setDestinationId(domain);
        event1.setEndPoint(domain);
        event1.setNextSpanId(-1);
        event1.setSequence((short) 0);
        event1.setDepth(1);
        event1.setApiId(0);
        event1.setAnnotations(new ArrayList());
        span.getSpanEventList().add(event1);

        // method
        TAnnotationValue http_domain = new TAnnotationValue();
        http_domain.setIntValue(MethodType.ANNOTATION);
        http_domain.setStringValue(StringUtils.isNotEmpty(vo.getHttpType()) ? vo.getHttpType() + "()" : "com.zhaopin.common3.http.executor.HttpExecutor.execute()");

        TAnnotation http = new TAnnotation();
        http.setKey(AnnotationKey.API.getCode());
        http.setValue(http_domain);

        event1.getAnnotations().add(http);


        // http url
        TAnnotation address = new TAnnotation();
        address.setKey(AnnotationKey.HTTP_URL.getCode());
        address.setValue(TAnnotationValue.stringValue(tempUrl));
        event1.getAnnotations().add(address);


        // 参数
        if (StringUtils.isNotEmpty(vo.getParams()) && !"{}".equals(vo.getParams())) {
            TAnnotation params = new TAnnotation();
            params.setKey(AnnotationKey.HTTP_PARAM.getCode());
            params.setValue(TAnnotationValue.stringValue(vo.getParams()));
            event1.getAnnotations().add(params);
        }

        // 状态码
        TAnnotation status = new TAnnotation();
        status.setKey(AnnotationKey.HTTP_STATUS_CODE.getCode());
        status.setValue(TAnnotationValue.stringValue(vo.getStatus()));
        event1.getAnnotations().add(status);

        this.spanHandler.handleSimple(span);
    }

    @Override
    public void insertDb(SpanVO vo) throws Exception {
        logger.info("insertDb - " + vo);
        this.check(vo);

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
        span.setSpanEventList(new ArrayList());
        span.setElapsed(elapsed);
        if (api != null) {
            span.setApiId(api.getApiId());
        }

        TSpanEvent event1 = new TSpanEvent();
        event1.setServiceType(MySqlConstants.MYSQL_EXECUTE_QUERY.getCode());
        event1.setSequence((short) 0);
        event1.setDestinationId(vo.getDbName());
        event1.setEndPoint(vo.getDbIp());
        event1.setNextSpanId(-1);
        event1.setApiId(0);
        event1.setDepth(1);
        event1.setAnnotations(new ArrayList());
        span.getSpanEventList().add(event1);

        // method
        TAnnotationValue jdbc_value = new TAnnotationValue();
        jdbc_value.setIntValue(MethodType.ANNOTATION);
        jdbc_value.setStringValue(StringUtils.isNotEmpty(vo.getDbMethod()) ? "sql." + vo.getDbMethod() + "()" : "sql.invoke()");

        TAnnotation jdbc_annotation = new TAnnotation();
        jdbc_annotation.setKey(AnnotationKey.API.getCode());
        jdbc_annotation.setValue(jdbc_value);

        event1.getAnnotations().add(jdbc_annotation);

        // sql
        if (vo.getDbSql() != null && !"".equals(vo.getDbSql())) {
            TAnnotation sql = new TAnnotation();
            sql.setKey(AnnotationKey.SQL.getCode());
            sql.setValue(TAnnotationValue.stringValue(vo.getDbSql()));
            event1.getAnnotations().add(sql);
        }

        // url
        if (vo.getDbUrl() != null && !"".equals(vo.getDbUrl())) {
            TAnnotation url = new TAnnotation();
            url.setKey(AnnotationKey.SQL_METADATA.getCode());
            url.setValue(TAnnotationValue.stringValue(vo.getDbUrl()));
            event1.getAnnotations().add(url);
        }

        // 参数
        if (StringUtils.isNotEmpty(vo.getParams())) {
            TAnnotation params = new TAnnotation();
            params.setKey(AnnotationKey.SQL_PARAM.getCode());
            params.setValue(TAnnotationValue.stringValue(vo.getParams()));
            event1.getAnnotations().add(params);
        }

        // 错误的
        if (StringUtils.isNotEmpty(vo.getStatus()) && !"true".equals(vo.getStatus()) && !"0".equals(vo.getStatus())) {
            TStringMetaData metaData = new TStringMetaData();
            metaData.setAgentId(agentId);
            metaData.setAgentStartTime(agentTime);
            metaData.setStringValue("error");
            metaData.setStringId(metaData.getStringValue().hashCode());
            this.stringMetaDataDao.insert(metaData);

            TIntStringValue exception = new TIntStringValue();
            exception.setIntValue(metaData.getStringId());
            exception.setStringValue(vo.getStatus());
            event1.setExceptionInfo(exception);
        }

        this.spanHandler.handleSimple(span);
    }

    @Override
    public void insertRedis(SpanVO vo) throws Exception {
        logger.info("insertRedis - " + vo);
        this.check(vo);

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

        if (api != null) {
            span.setApiId(api.getApiId());
        }

        // 方法
//        TSpanEvent event1 = new TSpanEvent();
//        event1.setServiceType(RedisConstants.REDIS.getCode());
//        event1.setSequence((short) 0);
//        event1.setDestinationId(vo.getRedisMethod());
//        event1.setEndPoint(vo.getRedisMethod());
//        event1.setNextSpanId(-1);
//        event1.setApiId(0);
//        event1.setDepth(1);
//        event1.setAnnotations(new ArrayList());

        TSpanEvent event1 = new TSpanEvent();
        event1.setServiceType(RedisConstants.REDIS.getCode());
        event1.setSequence((short) 0);
        event1.setDestinationId(vo.getRedisIp());
        event1.setEndPoint(vo.getRedisIp());
        event1.setNextSpanId(-1);
        event1.setApiId(0);
        event1.setDepth(1);
        event1.setAnnotations(new ArrayList());
        span.getSpanEventList().add(event1);

        // method
        TAnnotationValue jdbc_value = new TAnnotationValue();
        jdbc_value.setIntValue(MethodType.ANNOTATION);
        jdbc_value.setStringValue(StringUtils.isNotEmpty(vo.getRedisMethod()) ? vo.getRedisMethod() + "()" : "redis.invoke()");

        TAnnotation jdbc_annotation = new TAnnotation();
        jdbc_annotation.setKey(AnnotationKey.API.getCode());
        jdbc_annotation.setValue(jdbc_value);

        event1.getAnnotations().add(jdbc_annotation);

        // 地址
        if (StringUtils.isNotEmpty(vo.getRedisIp())) {
            TAnnotation address = new TAnnotation();
            address.setKey(AnnotationKey.REDIS_IP.getCode());
            address.setValue(TAnnotationValue.stringValue(vo.getRedisIp()));
            event1.getAnnotations().add(address);
        }

        // 参数
        if (StringUtils.isNotEmpty(vo.getParams())) {
            TAnnotation params = new TAnnotation();
            params.setKey(AnnotationKey.REDIS_PARAM.getCode());
            params.setValue(TAnnotationValue.stringValue(vo.getParams()));
            event1.getAnnotations().add(params);
        }

        // 错误的
        if (!"info".equals(vo.getStatus())) {
            TStringMetaData metaData = new TStringMetaData();
            metaData.setAgentId(agentId);
            metaData.setAgentStartTime(agentTime);
            metaData.setStringValue("error");
            metaData.setStringId(metaData.getStringValue().hashCode());
            this.stringMetaDataDao.insert(metaData);

            TIntStringValue exception = new TIntStringValue();
            exception.setIntValue(metaData.getStringId());
            exception.setStringValue(vo.getStatus());
            event1.setExceptionInfo(exception);
        }
        this.spanHandler.handleSimple(span);
    }

    @Override
    public void insertRpcProvider(SpanVO vo) throws Exception {
        logger.info("insertRpcProvider - " + vo);
        this.check(vo);

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

        span.setParentApplicationName(vo.getClientAppName());
        span.setRemoteAddr(vo.getRemoteType() + " " + vo.getIpAddress());
        span.setParentApplicationType(SpringBootConstants.SERVICE_TYPE.getCode());
        span.setAnnotations(new ArrayList());

        if (StringUtils.isNotEmpty(vo.getParams())) {
            TAnnotation params = new TAnnotation();
            params.setKey(81);
            params.setValue(TAnnotationValue.stringValue(vo.getParams()));
            span.getAnnotations().add(params);
        }
        if (StringUtils.isNotEmpty(vo.getStatus()) && !"0".equals(vo.getStatus())) {
            TAnnotation params = new TAnnotation();
            params.setKey(AnnotationKey.EXCEPTION.getCode());
            params.setValue(TAnnotationValue.stringValue(vo.getStatus()));
            span.getAnnotations().add(params);
        }

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
