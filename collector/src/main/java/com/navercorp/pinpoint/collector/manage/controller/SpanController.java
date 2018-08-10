package com.navercorp.pinpoint.collector.manage.controller;

import com.alibaba.fastjson.JSONObject;
import com.navercorp.pinpoint.collector.handler.SpanHandler;
import com.navercorp.pinpoint.collector.manage.service.SpanService;
import com.navercorp.pinpoint.collector.manage.vo.AgentVO;
import com.navercorp.pinpoint.collector.manage.vo.SpanVO;
import com.navercorp.pinpoint.common.util.TransactionIdUtils;
import com.navercorp.pinpoint.plugin.spring.boot.SpringBootConstants;
import com.navercorp.pinpoint.plugin.thrift.ThriftConstants;
import com.navercorp.pinpoint.thrift.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/span")
public class SpanController {

    @Autowired
    private SpanHandler spanHandler;

    @Autowired
    private SpanService spanService;

    @RequestMapping("insertUser")
    public String insertUser(String agentId, String appName, String url) {
        long now = System.currentTimeMillis();
        byte[] transactionId = TransactionIdUtils.formatBytes(agentId, now, 1);

        TSpan span = new TSpan();
        span.setAgentId(agentId);
        span.setApplicationName(appName);
        span.setTransactionId(transactionId);
        span.setStartTime(now);
        span.setAgentStartTime(now);
        span.setRpc(url);
        span.setSpanId(span.hashCode());
        span.setParentSpanId(-1);
        span.setServiceType(ThriftConstants.THRIFT_SERVER.getCode());
        span.setApplicationServiceType(SpringBootConstants.SERVICE_TYPE.getCode());
        this.spanHandler.handleSimple(span);
        return "ok";
    }

    @RequestMapping("insertHttp")
    public String insertHttp(SpanVO vo) {
        this.spanService.insertHttp(vo);
        return "ok";
    }

    @RequestMapping("insertHttpes")
    public String insertHttp(@RequestBody SpanVO[] list) {
        for (SpanVO vo : list) {
            this.spanService.insertHttp(vo);
        }
        return "ok";
    }

    @RequestMapping("insertDB")
    public String insertDb(SpanVO vo) {
        this.spanService.insertDb(vo);
        return "ok";
    }

    @RequestMapping("insertDBes")
    public String insertDbs(@RequestBody SpanVO[] list) {
        for (SpanVO vo : list) {
            this.spanService.insertDb(vo);
        }
        return "ok";
    }

    @RequestMapping("insertRedis")
    public String insertRedis(SpanVO vo) {
        this.spanService.insertRedis(vo);
        return "ok";
    }

    @RequestMapping("insertRedises")
    public String insertRedises(@RequestBody SpanVO[] list) {
        for (SpanVO vo : list) {
            this.spanService.insertRedis(vo);
        }
        return "ok";
    }

    @RequestMapping("insertRpcProvider")
    public String insertRpcProvider(SpanVO vo) {
        this.spanService.insertRpcProvider(vo);
        return "ok";
    }

    @RequestMapping("insertRpcProvideres")
    public String insertRpcProvideres(@RequestBody SpanVO[] list) {
        for (SpanVO vo : list) {
            this.spanService.insertRpcProvider(vo);
        }
        return "ok";
    }

    @RequestMapping("insertRpcClient")
    public String insertRpcClient(SpanVO vo) {
        this.spanService.insertRpcClient(vo);
        return "ok";
    }

    @RequestMapping("insertRpcClientes")
    public String insertRpcClientes(@RequestBody SpanVO[] list) {
        for (SpanVO vo : list) {
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

    static String agentId_1 = "_svn-10.2.1.131";
    static String appName_1 = "_svn";

    @RequestMapping("test")
    public String test() {
        long now = System.currentTimeMillis();
        byte[] transactionId = TransactionIdUtils.formatBytes(agentId_1, now, 1);

        TSpan span = new TSpan();
        span.setAgentId(agentId_1);
        span.setApplicationName(appName_1);

        span.setStartTime(now);
        span.setAgentStartTime(now);
        span.setSpanId(span.hashCode());
        span.setParentSpanId(-1);
        span.setRpc("/test/");
        span.setTransactionId(transactionId);
        span.setSpanEventList(new ArrayList<TSpanEvent>());

        span.setElapsed(8);
        span.setApiId(212);
        span.setEndPoint("localhost:9090");
        span.setRemoteAddr("127.0.0.1");
        span.setServiceType(Short.valueOf("1010"));
        span.setApplicationServiceType(Short.valueOf("1210"));

        TSpanEvent event0 = new TSpanEvent();
        event0.setServiceType(Short.valueOf("1011"));
        event0.setDepth(1);
        event0.setSequence((short) 0);
        event0.setStartElapsed(0);
        event0.setEndElapsed(8);
        event0.setApiId(211);

        TSpanEvent event1 = new TSpanEvent();
        event1.setServiceType(Short.valueOf("5051"));
        event1.setDepth(2);
        event1.setSequence((short) 1);
        event1.setStartElapsed(0);
        event1.setEndElapsed(8);
        event1.setApiId(-1);

        TSpanEvent event2 = new TSpanEvent();
        event2.setServiceType(Short.valueOf("5071"));
        event2.setDepth(3);
        event2.setSequence((short) 2);
        event2.setStartElapsed(0);
        event2.setEndElapsed(8);
        event2.setApiId(-201);

        TSpanEvent event3 = new TSpanEvent();
        event3.setServiceType(Short.valueOf("5510"));
        event3.setDepth(4);
        event3.setSequence((short) 3);
        event3.setStartElapsed(0);
        event3.setEndElapsed(8);
        event3.setApiId(203);
        TAnnotation tAnnotation3_1 = new TAnnotation();
        tAnnotation3_1.setKey(-30);
        tAnnotation3_1.setValue(TAnnotationValue.intValue(-30));
        event3.setAnnotations(new ArrayList<TAnnotation>());
        event3.getAnnotations().add(tAnnotation3_1);

        TSpanEvent event4 = new TSpanEvent();
        event4.setServiceType(Short.valueOf("6052"));
        event4.setDepth(5);
        event4.setSequence((short) 4);
        event4.setStartElapsed(0);
        event4.setEndElapsed(1);
        event4.setApiId(-202);
//
        TSpanEvent event5 = new TSpanEvent();
        event5.setServiceType(Short.valueOf("2100"));
        event5.setDepth(-1);
        event5.setSequence((short) 5);
        event5.setStartElapsed(0);
        event5.setEndElapsed(4);
        event5.setApiId(-225);
        event5.setEndPoint("127.0.0.1:3306");
        event5.setDestinationId("dhcc_application");

        TAnnotation tAnnotation5_1 = new TAnnotation();
        tAnnotation5_1.setKey(20);
        tAnnotation5_1.setValue(TAnnotationValue.intStringStringValue(new TIntStringStringValue(20)));
        event5.setAnnotations(new ArrayList<TAnnotation>());
        event5.getAnnotations().add(tAnnotation5_1);

        TSpanEvent event6 = new TSpanEvent();
        event6.setServiceType(Short.valueOf("2101"));
        event6.setDepth(-1);
        event6.setSequence((short) 6);
        event6.setStartElapsed(1);
        event6.setEndElapsed(7);
        event6.setApiId(-229);
        event6.setEndPoint("127.0.0.1:3306");
        event6.setDestinationId("dhcc_application");

        TAnnotation tAnnotation6_1 = new TAnnotation();
        tAnnotation6_1.setKey(20);
        tAnnotation6_1.setValue(TAnnotationValue.intStringStringValue(new TIntStringStringValue(20)));
        event6.setAnnotations(new ArrayList<TAnnotation>());
        event6.getAnnotations().add(tAnnotation6_1);


        TSpanEvent event7 = new TSpanEvent();
        event7.setServiceType(Short.valueOf("6052"));
        event7.setDepth(-1);
        event7.setSequence((short) 7);
        event7.setStartElapsed(0);
        event7.setEndElapsed(8);
        event7.setApiId(-233);

        span.getSpanEventList().add(event0);
        span.getSpanEventList().add(event1);
        span.getSpanEventList().add(event2);
        span.getSpanEventList().add(event3);
        span.getSpanEventList().add(event4);
        span.getSpanEventList().add(event5);
        span.getSpanEventList().add(event6);
        span.getSpanEventList().add(event7);

        this.spanHandler.handleSimple(span);

        return "true";
    }

}
