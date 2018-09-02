/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.web.dao.hbase;

import java.util.List;

import com.navercorp.pinpoint.common.buffer.AutomaticBuffer;
import com.navercorp.pinpoint.common.buffer.Buffer;
import com.navercorp.pinpoint.common.hbase.TableNameProvider;
import com.navercorp.pinpoint.thrift.dto.TApiMetaData;
import com.sematext.hbase.wd.RowKeyDistributorByHashPrefix;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import com.navercorp.pinpoint.common.server.bo.ApiMetaDataBo;
import com.navercorp.pinpoint.common.hbase.HBaseTables;
import com.navercorp.pinpoint.common.hbase.HbaseOperations2;
import com.navercorp.pinpoint.common.hbase.RowMapper;
import com.navercorp.pinpoint.web.dao.ApiMetaDataDao;

/**
 * @author emeroad
 * @author jaehong.kim
 */
@Repository
public class HbaseApiMetaDataDao implements ApiMetaDataDao {
    static final String SPEL_KEY = "#agentId.toString() + '.' + #time.toString() + '.' + #apiId.toString()";
    
    @Autowired
    private HbaseOperations2 hbaseOperations2;

    @Autowired
    private HbaseOperations2 hbaseTemplate;

    @Autowired
    private TableNameProvider tableNameProvider;

    @Autowired
    @Qualifier("apiMetaDataMapper")
    private RowMapper<List<ApiMetaDataBo>> apiMetaDataMapper;

    @Autowired
    @Qualifier("metadataRowKeyDistributor")
    private RowKeyDistributorByHashPrefix rowKeyDistributorByHashPrefix;

    @Override
    @Cacheable(value="apiMetaData", key=SPEL_KEY)
    public List<ApiMetaDataBo> getApiMetaData(String agentId, long time, int apiId) {
        if (agentId == null) {
            throw new NullPointerException("agentId must not be null");
        }

        ApiMetaDataBo apiMetaDataBo = new ApiMetaDataBo(agentId, time, apiId);
        byte[] sqlId = getDistributedKey(apiMetaDataBo.toRowKey());
        Get get = new Get(sqlId);
        get.addFamily(HBaseTables.API_METADATA_CF_API);

        TableName apiMetaDataTableName = tableNameProvider.getTableName(HBaseTables.API_METADATA_STR);
        return hbaseOperations2.get(apiMetaDataTableName, get, apiMetaDataMapper);
    }

    private byte[] getDistributedKey(byte[] rowKey) {
        return rowKeyDistributorByHashPrefix.getDistributedKey(rowKey);
    }

    @Override
    public void insert(TApiMetaData apiMetaData) {
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
        if(apiMetaData.isSetType()) {
            buffer.putInt(apiMetaData.getType());
        } else {
            buffer.putInt(0);
        }

        final byte[] apiMetaDataBytes = buffer.getBuffer();
        put.addColumn(HBaseTables.API_METADATA_CF_API, HBaseTables.API_METADATA_CF_API_QUALI_SIGNATURE, apiMetaDataBytes);

        TableName apiMetaDataTableName = tableNameProvider.getTableName(HBaseTables.API_METADATA_STR);
        hbaseTemplate.put(apiMetaDataTableName, put);
    }
}
