package com.navercorp.pinpoint.collector.manage.controller;

import com.alibaba.fastjson.JSONObject;
import com.navercorp.pinpoint.collector.manage.service.SpanService;
import com.navercorp.pinpoint.collector.manage.vo.AgentVO;
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
    public String create(@RequestBody String json) {
        logger.info(json);
        AgentVO vo = JSONObject.parseObject(json, AgentVO.class);
        if (vo != null) {
            this.spanService.insertAgent(vo);
        }
        return "ok";
    }

}
