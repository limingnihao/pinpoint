package com.navercorp.pinpoint.collector.http.handler.controller;

import com.alibaba.fastjson.JSONObject;
import com.navercorp.pinpoint.collector.handler.SpanHandler;
import com.navercorp.pinpoint.collector.http.handler.service.SpanService;
import com.navercorp.pinpoint.collector.http.handler.vo.AgentVO;
import com.navercorp.pinpoint.collector.http.handler.vo.SpanVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/span")
public class SpanController {

    @Autowired
    private SpanService spanService;

    @RequestMapping("insertUser")
    public String insertUser(SpanVO vo) {
        try {
            this.spanService.insertUser(vo);
            return "ok";
        } catch (Exception e) {
            return "error";
        }
    }

    @RequestMapping("insertUserJson")
    public String insertUserJson(@RequestBody String json) {
        SpanVO vo = JSONObject.parseObject(json, SpanVO.class);
        try {
            this.spanService.insertUser(vo);
            return "ok";
        } catch (Exception e) {
            return "error";
        }
    }

    @RequestMapping("insertHttp")
    public String insertHttp(SpanVO vo) {
        try {
            this.spanService.insertHttp(vo);
            return "ok";
        } catch (Exception e) {
            return "error";
        }
    }

    @RequestMapping("insertHttpJson")
    public String insertHttpJson(@RequestBody String json) {
        SpanVO vo = JSONObject.parseObject(json, SpanVO.class);
        try {
            this.spanService.insertHttp(vo);
            return "ok";
        } catch (Exception e) {
            return "error";
        }
    }

    @RequestMapping("insertDB")
    public String insertDb(SpanVO vo) {
        try {
            this.spanService.insertDb(vo);
            return "ok";
        } catch (Exception e) {
            return "error";
        }
    }

    @RequestMapping("insertDBJson")
    public String insertDBJson(@RequestBody String json) {
        SpanVO vo = JSONObject.parseObject(json, SpanVO.class);
        try {
            this.spanService.insertDb(vo);
            return "ok";
        } catch (Exception e) {
            return "error";
        }
    }

    @RequestMapping("insertRedis")
    public String insertRedis(SpanVO vo) {
        try {
            this.spanService.insertRedis(vo);
            return "ok";
        } catch (Exception e) {
            return "error";
        }
    }

    @RequestMapping("insertRedisJson")
    public String insertRedisJson(@RequestBody String json) {
        SpanVO vo = JSONObject.parseObject(json, SpanVO.class);
        try {
            this.spanService.insertRedis(vo);
            return "ok";
        } catch (Exception e) {
            return "error";
        }
    }

    @RequestMapping("insertRpcProvider")
    public String insertRpcProvider(SpanVO vo) {
        vo.setRemoteType("rpc");
        try {
            this.spanService.insertRpcProvider(vo);
            return "ok";
        } catch (Exception e) {
            return "error";
        }
    }

    @RequestMapping("insertRpcProviderJson")
    public String insertRpcProviderJson(@RequestBody String json) {
        SpanVO vo = JSONObject.parseObject(json, SpanVO.class);
            vo.setRemoteType("rpc");
        try {
            this.spanService.insertRpcProvider(vo);
            return "ok";
        } catch (Exception e) {
            return "error";
        }
    }

    @RequestMapping("insertHttpProvider")
    public String insertHttpProvider(SpanVO vo) {
        vo.setRemoteType("http");
        try {
            this.spanService.insertRpcProvider(vo);
            return "ok";
        } catch (Exception e) {
            return "error";
        }
    }

    @RequestMapping("insertHttpProviderJson")
    public String insertHttpProviderJson(@RequestBody String json) {
        SpanVO vo = JSONObject.parseObject(json, SpanVO.class);
            vo.setRemoteType("http");
        try {
            this.spanService.insertRpcProvider(vo);
            return "ok";
        } catch (Exception e) {
            return "error";
        }
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
        try{
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
        }catch(Exception e){
            return "error";
        }
        return "OK";
    }


}
