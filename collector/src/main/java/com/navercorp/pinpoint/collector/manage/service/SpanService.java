package com.navercorp.pinpoint.collector.manage.service;

import com.navercorp.pinpoint.collector.manage.vo.AgentVO;
import com.navercorp.pinpoint.collector.manage.vo.SpanVO;

public interface SpanService {

    void insertAgent(AgentVO agentVO);

    void insertHttp(SpanVO spanVO);


    void insertDb(SpanVO spanVO);


    void insertRedis(SpanVO spanVO);


    void insertRpcProvider(SpanVO spanVO);

    
    void insertRpcClient(SpanVO spanVO);
}
