package com.navercorp.pinpoint.collector.http.handler.controller;

import com.alibaba.fastjson.JSONObject;
import com.navercorp.pinpoint.collector.http.handler.service.SpanService;
import com.navercorp.pinpoint.collector.http.handler.vo.AgentVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent")
public class AgentController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SpanService spanService;

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public String create(AgentVO vo) {
        logger.info("create - " + vo.toString());
        this.spanService.insertAgent(vo);
        return "ok";
    }

    @RequestMapping(value = "/createJson", method = RequestMethod.POST)
    public String createJson(@RequestBody String json) {
        logger.info("createJson - " + json);
        AgentVO vo = JSONObject.parseObject(json, AgentVO.class);
        if (vo != null) {
            this.spanService.insertAgent(vo);
        }
        return "ok";
    }

}
