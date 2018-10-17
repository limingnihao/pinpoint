package com.navercorp.pinpoint.collector.http.handler.service;


import com.navercorp.pinpoint.collector.http.handler.vo.AgentVO;
import com.navercorp.pinpoint.collector.http.handler.vo.SpanVO;

public interface SpanService {

    void insertAgent(AgentVO agentVO);

    void insertUser(SpanVO spanVO) throws Exception;

    void insertHttp(SpanVO spanVO) throws Exception;

    void insertDb(SpanVO spanVO) throws Exception;

    void insertRedis(SpanVO spanVO) throws Exception;

    void insertRpcProvider(SpanVO spanVO) throws Exception;

    void insertRpcClient(SpanVO spanVO);
}
