package com.navercorp.pinpoint.collector.manage.controller;

import com.alibaba.fastjson.JSONObject;
import com.navercorp.pinpoint.collector.handler.SpanHandler;
import com.navercorp.pinpoint.collector.manage.service.SpanService;
import com.navercorp.pinpoint.collector.manage.vo.AgentVO;
import com.navercorp.pinpoint.collector.manage.vo.SpanVO;
import com.navercorp.pinpoint.common.server.util.AcceptedTimeService;
import com.navercorp.pinpoint.common.util.TransactionIdUtils;
import com.navercorp.pinpoint.plugin.jdbc.mysql.MySqlConstants;
import com.navercorp.pinpoint.thrift.dto.TSpan;
import com.navercorp.pinpoint.thrift.dto.TSpanEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/span")
public class SpanController {

    @Autowired
    private SpanHandler spanHandler;

    @Autowired
    private SpanService spanService;

    @RequestMapping("insertUser")
    public String insertUser(SpanVO vo) {
        this.spanService.insertUser(vo);
        return "ok";
    }

    @RequestMapping("insertUserJson")
    public String insertUserJson(@RequestBody String json) {
        SpanVO vo = JSONObject.parseObject(json, SpanVO.class);
        if (vo != null) {
            this.spanService.insertUser(vo);
        }
        return "ok";
    }

    @RequestMapping("insertHttp")
    public String insertHttp(SpanVO vo) {
        this.spanService.insertHttp(vo);
        return "ok";
    }

    @RequestMapping("insertHttpJson")
    public String insertHttpJson(@RequestBody String json) {
        SpanVO vo = JSONObject.parseObject(json, SpanVO.class);
        if (vo != null) {
            this.spanService.insertHttp(vo);
        }
        return "ok";
    }

    @RequestMapping("insertDB")
    public String insertDb(SpanVO vo) {
        this.spanService.insertDb(vo);
        return "ok";
    }

    @RequestMapping("insertDBJson")
    public String insertDBJson(@RequestBody String json) {
        SpanVO vo = JSONObject.parseObject(json, SpanVO.class);
        if (vo != null) {
            this.spanService.insertDb(vo);
        }
        return "ok";
    }

    @RequestMapping("insertRedis")
    public String insertRedis(SpanVO vo) {
        this.spanService.insertRedis(vo);
        return "ok";
    }

    @RequestMapping("insertRedisJson")
    public String insertRedisJson(@RequestBody String json) {
        SpanVO vo = JSONObject.parseObject(json, SpanVO.class);
        if (vo != null) {
            this.spanService.insertRedis(vo);
        }
        return "ok";
    }

    @RequestMapping("insertRpcProvider")
    public String insertRpcProvider(SpanVO vo) {
        vo.setRemoteType("rpc");
        this.spanService.insertRpcProvider(vo);
        return "ok";
    }

    @RequestMapping("insertRpcProviderJson")
    public String insertRpcProviderJson(@RequestBody String json) {
        SpanVO vo = JSONObject.parseObject(json, SpanVO.class);
        if (vo != null) {
            vo.setRemoteType("rpc");
            this.spanService.insertRpcProvider(vo);
        }
        return "ok";
    }

    @RequestMapping("insertHttpProvider")
    public String insertHttpProvider(SpanVO vo) {
        vo.setRemoteType("http");
        this.spanService.insertRpcProvider(vo);
        return "ok";
    }

    @RequestMapping("insertHttpProviderJson")
    public String insertHttpProviderJson(@RequestBody String json) {
        SpanVO vo = JSONObject.parseObject(json, SpanVO.class);
        if (vo != null) {
            vo.setRemoteType("http");
            this.spanService.insertRpcProvider(vo);
        }
        return "ok";
    }

    @RequestMapping("insertRpcClient")
    public String insertRpcClient(SpanVO vo) {
        this.spanService.insertRpcClient(vo);
        return "ok";
    }

    @RequestMapping("insertRpcClientJson")
    public String insertRpcClientJson(@RequestBody String json) {
        SpanVO vo = JSONObject.parseObject(json, SpanVO.class);
        if (vo != null) {
            this.spanService.insertRpcClient(vo);
        }
        return "ok";
    }

    @RequestMapping("insertAll")
    public String all(String agentJson, String httpesJson, String redisesJson, String dbesJson, String rpcClientesJson) {
        AgentVO agentVO = JSONObject.parseObject(agentJson, AgentVO.class);
        List<SpanVO> httpList = JSONObject.parseArray(httpesJson, SpanVO.class);
        List<SpanVO> redisList = JSONObject.parseArray(redisesJson, SpanVO.class);
        List<SpanVO> dbList = JSONObject.parseArray(dbesJson, SpanVO.class);
        List<SpanVO> clientList = JSONObject.parseArray(rpcClientesJson, SpanVO.class);

        if (agentVO != null || agentVO.getAppName() != null) {
            this.spanService.insertAgent(agentVO);
        }
        if (httpList != null) {
            for (SpanVO vo : httpList) {
                this.spanService.insertHttp(vo);
            }
        }
        if (redisList != null) {
            for (SpanVO vo : redisList) {
                this.spanService.insertRedis(vo);
            }
        }
        if (dbList != null) {
            for (SpanVO vo : dbList) {
                this.spanService.insertDb(vo);
            }
        }
        if (clientList != null) {
            for (SpanVO vo : clientList) {
                this.spanService.insertRpcClient(vo);
            }
        }

        return "OK";
    }

    @Autowired
    private AcceptedTimeService acceptedTimeService;

    @RequestMapping("test")
    public String test() {
        long spanId_1 = (long) (Math.random() * 100000000000L);
        long spanId_2 = (long) (Math.random() * 100000000000L);
        long now = System.currentTimeMillis();
        int elapsed = (int) (Math.random() * 1000);
        String traceId = UUID.randomUUID().toString();

        acceptedTimeService.accept(now);

        byte[] t1 = TransactionIdUtils.formatBytes("dubbo-client", now, traceId.hashCode());
        byte[] t2 = TransactionIdUtils.formatBytes("dubbo-client", now, traceId.hashCode());

        //TransactionIdUtils.formatBytes("dubbo-client", 1534385520914L, 1)
//        byte [] t1 = UUID.randomUUID().toString().getBytes();
        testClient(now, 1534408534930L, spanId_1, spanId_2, t1, elapsed);


        testProvider(now, 1534408534930L, spanId_2, spanId_1, t1);
        return "ok";
    }


    @RequestMapping("testClient")
    public String testClient(long now, long agentStartTime, long spanId, long spanId2, byte[] transactionId, int elapsed) {
        String agentId = "dubbo-client";
        String appName = "dubboClient";
        String rpc = "/region/getTotal";
        String endPoint = "localhost:21100";
        int apiId = 7;
        short serviceType = 1010;
        short applicationServiceType = 1210;

        TSpan span = new TSpan();
        span.setAgentId(agentId);
        span.setApplicationName(appName);

        span.setStartTime(now);
        span.setAgentStartTime(agentStartTime);
        span.setSpanId(spanId);
        span.setParentSpanId(-1);
        span.setRpc(rpc);
        span.setTransactionId(transactionId);
        span.setSpanEventList(new ArrayList<TSpanEvent>());
        span.setElapsed(elapsed);
        span.setApiId(apiId);
        span.setEndPoint(endPoint);
        span.setRemoteAddr("");
        span.setServiceType(serviceType);
        span.setApplicationServiceType(applicationServiceType);

        TSpanEvent event0 = new TSpanEvent();
        event0.setServiceType(Short.valueOf("1011"));
        event0.setSequence((short) 0);
        event0.setDepth(1);
        event0.setApiId(6);
        event0.setStartElapsed(0);
        event0.setEndElapsed(0);

        TSpanEvent event1 = new TSpanEvent();
        event1.setServiceType(Short.valueOf("5051"));
        event1.setSequence((short) 1);
        event1.setDepth(2);
        event1.setApiId(2);

        TSpanEvent event2 = new TSpanEvent();
        event2.setServiceType(Short.valueOf("5071"));
        event2.setSequence((short) 2);
        event2.setDepth(3);
        event2.setApiId(-4);

        TSpanEvent event3 = new TSpanEvent();
        event3.setServiceType(Short.valueOf("9110"));
        event3.setSequence((short) 3);
        event3.setDepth(4);
        event3.setApiId(8);
        event3.setEndPoint("10.2.2.172:20880");
        event3.setDestinationId("10.2.2.172:20880");
        event3.setNextSpanId(spanId2);

        span.getSpanEventList().add(event0);
        span.getSpanEventList().add(event1);
        span.getSpanEventList().add(event2);
        span.getSpanEventList().add(event3);
        this.spanHandler.handleSimple(span);
        return "true";
    }

    @RequestMapping("testProvider")
    public String testProvider(long now, long agentStartTime, long spanId, long parentSpanId, byte[] transactionId) {
        String agentId = "dubbo-provider";
        String appName = "dubboProvider";
        String rpc = "RPCRegionService:getTotal";
        String endPoint = "192.168.1.69:20880";
        int apiId = 15;
        short serviceType = 1110;
        short applicationServiceType = 1210;
        String parentApplicationName = "dubboClient";
        short parentApplicationType = 1210;


        TSpan span = new TSpan();
        span.setAgentId(agentId);
        span.setApplicationName(appName);

        span.setStartTime(now);
        span.setAgentStartTime(agentStartTime);
        span.setSpanId(spanId);
        span.setParentSpanId(parentSpanId);
        span.setParentApplicationType(parentApplicationType);
        span.setParentApplicationName(parentApplicationName);

        span.setRpc(rpc);
        span.setTransactionId(transactionId);
        span.setSpanEventList(new ArrayList<TSpanEvent>());

        span.setApiId(apiId);
        span.setEndPoint(endPoint);
        span.setRemoteAddr("");
        span.setServiceType(serviceType);
        span.setApplicationServiceType(applicationServiceType);

        TSpanEvent event0 = new TSpanEvent();
        event0.setServiceType(Short.valueOf("5510"));
        event0.setSequence((short) 0);
        event0.setDepth(1);
        event0.setApiId(6);

        TSpanEvent event1 = new TSpanEvent();
        event1.setServiceType(Short.valueOf("6052"));
        event1.setSequence((short) 1);
        event1.setDepth(2);
        event1.setApiId(3);

        TSpanEvent event2 = new TSpanEvent();
        event2.setServiceType(MySqlConstants.MYSQL.getCode());
        event2.setSequence((short) 1);
        event2.setDestinationId("DHCC_APPLICATION");
        event2.setEndPoint("127.0.0.1");
        event2.setDepth(3);

        TSpanEvent event3 = new TSpanEvent();
        event3.setServiceType(MySqlConstants.MYSQL_EXECUTE_QUERY.getCode());
        event3.setSequence((short) 2);
        event3.setDestinationId("DHCC_APPLICATION");
        event3.setEndPoint("127.0.0.1");
        event3.setDepth(4);

        span.getSpanEventList().add(event0);
        span.getSpanEventList().add(event1);
        span.getSpanEventList().add(event2);
//        span.getSpanEventList().add(event3);
        this.spanHandler.handleSimple(span);
        return "true";
    }
}
