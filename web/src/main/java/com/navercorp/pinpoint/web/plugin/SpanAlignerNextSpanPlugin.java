package com.navercorp.pinpoint.web.plugin;

import com.navercorp.pinpoint.common.buffer.AutomaticBuffer;
import com.navercorp.pinpoint.common.buffer.Buffer;
import com.navercorp.pinpoint.common.hbase.HBaseTables;
import com.navercorp.pinpoint.common.hbase.HbaseOperations2;
import com.navercorp.pinpoint.common.hbase.TableNameProvider;
import com.navercorp.pinpoint.common.server.bo.ApiMetaDataBo;
import com.navercorp.pinpoint.common.server.bo.SpanBo;
import com.navercorp.pinpoint.common.server.bo.SpanEventBo;
import com.navercorp.pinpoint.plugin.jdbc.mysql.MySqlConstants;
import com.navercorp.pinpoint.plugin.jdk.http.JdkHttpConstants;
import com.navercorp.pinpoint.plugin.redis.RedisConstants;
import com.navercorp.pinpoint.thrift.dto.TApiMetaData;
import com.sematext.hbase.wd.RowKeyDistributorByHashPrefix;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpanAlignerNextSpanPlugin {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private HbaseOperations2 hbaseTemplate;

    @Autowired
    private TableNameProvider tableNameProvider;

    @Autowired
    @Qualifier("metadataRowKeyDistributor")
    private RowKeyDistributorByHashPrefix rowKeyDistributorByHashPrefix;

    public List<SpanBo> process(List<SpanBo> inputList) {
        List<SpanBo> resultList = new ArrayList<>();
        for (SpanBo b : inputList) {
            System.out.println(b.getApplicationId() + " - " + b.getElapsed());
            int index = 0;
            for (int i = 0; i < resultList.size(); i++) {
                SpanBo r = resultList.get(i);
                if (b.getStartTime() < r.getStartTime()) {
                    index = i;
                }
            }
            if (index > 0) {
                resultList.add(index, b);
            } else {
                resultList.add(b);
            }
        }
        // init sorted node list
        Map<String, SpanBo> spanMap = new HashMap<>();
        for (SpanBo span : resultList) {
            spanMap.put(span.getSpanId() + "", span);
        }
        for (SpanBo span : resultList) {
            // 找到自己的parent，并且将parent的event的next设置成自己spanId
            SpanBo parentSpan = spanMap.get(span.getParentSpanId() + "");
            if (parentSpan != null) {
                boolean isCreateNext = true;
                for (SpanEventBo event : parentSpan.getSpanEventBoList()) {
                    if (event.getNextSpanId() == span.getSpanId()) {
                        isCreateNext = false;
                    }
                }
                if (isCreateNext) {
                    short serviceType = 0;
                    if (span.getSpanEventBoList() != null && span.getSpanEventBoList().size() > 0) {
                        serviceType = span.getSpanEventBoList().get(0).getServiceType();
                    } else {
                        serviceType = span.getServiceType();
                    }
                    System.out.println("serviceType=" + serviceType);
                    String service = "com.zhaopin.thrift.Proxy.proxy()";
                    if (span.getRemoteAddr() != null && span.getRemoteAddr().startsWith("http")) {
                        service = "com.zhaopin.thrift.Proxy.restfulProxy()";
                    } else if (span.getRemoteAddr() != null && span.getRemoteAddr().startsWith("rpc")) {
                        service = "com.zhaopin.thrift.Proxy.rpcProxy()";
                    } else if (serviceType == JdkHttpConstants.SERVICE_TYPE.getCode()) {
                        service = "com.zhaopin.thrift.Proxy.httpProxy()";
                    } else if (serviceType == MySqlConstants.MYSQL_EXECUTE_QUERY.getCode()) {
                        service = "com.zhaopin.thrift.Proxy.dbProxy()";
                    } else if (serviceType == RedisConstants.REDIS.getCode()) {
                        service = "com.zhaopin.thrift.Proxy.redisProxy()";
                    }

                    //
                    span.setParentApplicationId(parentSpan.getApplicationId());
                    span.setParentApplicationServiceType(parentSpan.getApplicationServiceType());
                    // 创建api
                    TApiMetaData api = new TApiMetaData();
                    api.setAgentId(parentSpan.getAgentId());
                    api.setAgentStartTime(parentSpan.getAgentStartTime());
                    api.setApiId(service.hashCode());
                    api.setApiInfo(service);
                    this.insertApi(api);

                    // 创建连接的event
                    SpanEventBo event = new SpanEventBo();
                    event.setServiceType(parentSpan.getServiceType());
                    event.setApiId(service.hashCode());
                    event.setDestinationId(parentSpan.getEndPoint());
                    event.setEndPoint(parentSpan.getEndPoint());

                    event.setSequence(Short.parseShort(parentSpan.getSpanEventBoList().size() + ""));
                    event.setDepth(1);
                    event.setNextSpanId(span.getSpanId());

                    parentSpan.getSpanEventBoList().add(event);
                }
            }
        }
        return resultList;
    }


    public void insertApi(TApiMetaData apiMetaData) {
        if (logger.isDebugEnabled()) {
            logger.debug("insert:{}", apiMetaData);
        }


        ApiMetaDataBo apiMetaDataBo = new ApiMetaDataBo(apiMetaData.getAgentId(), apiMetaData.getAgentStartTime(), apiMetaData.getApiId());
        byte[] rowKey = getDistributedKey(apiMetaDataBo.toRowKey());

        final Put put = new Put(rowKey);

        final Buffer buffer = new AutomaticBuffer(64);
        String api = apiMetaData.getApiInfo();
        buffer.putPrefixedString(api);
        if (apiMetaData.isSetLine()) {
            buffer.putInt(apiMetaData.getLine());
        } else {
            buffer.putInt(-1);
        }
        if (apiMetaData.isSetType()) {
            buffer.putInt(apiMetaData.getType());
        } else {
            buffer.putInt(0);
        }

        final byte[] apiMetaDataBytes = buffer.getBuffer();
        put.addColumn(HBaseTables.API_METADATA_CF_API, HBaseTables.API_METADATA_CF_API_QUALI_SIGNATURE, apiMetaDataBytes);

        TableName apiMetaDataTableName = tableNameProvider.getTableName(HBaseTables.API_METADATA_STR);
        hbaseTemplate.put(apiMetaDataTableName, put);
    }

    private byte[] getDistributedKey(byte[] rowKey) {
        return rowKeyDistributorByHashPrefix.getDistributedKey(rowKey);
    }

}
