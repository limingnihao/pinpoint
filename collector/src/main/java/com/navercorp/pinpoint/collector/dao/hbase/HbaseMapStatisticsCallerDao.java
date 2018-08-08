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

package com.navercorp.pinpoint.collector.dao.hbase;

import com.navercorp.pinpoint.collector.dao.MapStatisticsCallerDao;
import com.navercorp.pinpoint.collector.dao.hbase.statistics.*;
import com.navercorp.pinpoint.common.hbase.HbaseOperations2;
import com.navercorp.pinpoint.common.hbase.TableNameProvider;
import com.navercorp.pinpoint.common.server.util.AcceptedTimeService;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.util.ApplicationMapStatisticsUtils;
import com.navercorp.pinpoint.common.util.TimeSlot;
import com.sematext.hbase.wd.RowKeyDistributorByHashPrefix;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Scan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.navercorp.pinpoint.common.hbase.HBaseTables.MAP_STATISTICS_CALLEE_VER2_CF_COUNTER;
import static com.navercorp.pinpoint.common.hbase.HBaseTables.MAP_STATISTICS_CALLEE_VER2_STR;

/**
 * Update statistics of caller node
 *
 * @author netspider
 * @author emeroad
 * @author HyunGil Jeong
 */
@Repository
public class HbaseMapStatisticsCallerDao implements MapStatisticsCallerDao {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private HbaseOperations2 hbaseTemplate;

    @Autowired
    private TableNameProvider tableNameProvider;

    @Autowired
    private AcceptedTimeService acceptedTimeService;

    @Autowired
    private TimeSlot timeSlot;

    @Autowired
    @Qualifier("callerBulkIncrementer")
    private BulkIncrementer bulkIncrementer;

    @Autowired
    @Qualifier("statisticsCallerRowKeyDistributor")
    private RowKeyDistributorByHashPrefix rowKeyDistributorByHashPrefix;

    private final boolean useBulk;

    public HbaseMapStatisticsCallerDao() {
        this(true);
    }

    public HbaseMapStatisticsCallerDao(boolean useBulk) {
        this.useBulk = useBulk;
    }

    @Override
    public void update(String callerApplicationName, ServiceType callerServiceType, String callerAgentid, String calleeApplicationName, ServiceType calleeServiceType, String calleeHost, int elapsed, boolean isError, long total) {
        if (callerApplicationName == null) {
            throw new NullPointerException("callerApplicationName must not be null");
        }
        if (calleeApplicationName == null) {
            throw new NullPointerException("calleeApplicationName must not be null");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("[Caller] {} ({}) {} -> {} ({})[{}]", callerApplicationName, callerServiceType, callerAgentid,
                    calleeApplicationName, calleeServiceType, calleeHost);
        }

        // there may be no endpoint in case of httpclient
        calleeHost = StringUtils.defaultString(calleeHost);

        // make row key. rowkey is me
        final long acceptedTime = acceptedTimeService.getAcceptedTime();
        final long rowTimeSlot = timeSlot.getTimeSlot(acceptedTime);
        final RowKey callerRowKey = new CallRowKey(callerApplicationName, callerServiceType.getCode(), rowTimeSlot);

        final short calleeSlotNumber = ApplicationMapStatisticsUtils.getSlotNumber(calleeServiceType, elapsed, isError);
        final ColumnName calleeColumnName = new CalleeColumnName(callerAgentid, calleeServiceType.getCode(), calleeApplicationName, calleeHost, calleeSlotNumber);
        if (useBulk) {
            TableName mapStatisticsCalleeTableName = tableNameProvider.getTableName(MAP_STATISTICS_CALLEE_VER2_STR);
            // shiming.li update
            bulkIncrementer.increment(mapStatisticsCalleeTableName, callerRowKey, calleeColumnName, total);
        } else {
            final byte[] rowKey = getDistributedKey(callerRowKey.getRowKey());
            // column name is the name of caller app.
            byte[] columnName = calleeColumnName.getColumnName();
            // increment(rowKey, columnName, 1L);
            // shiming.li update
            increment(rowKey, columnName, total);
        }
    }

    private void increment(byte[] rowKey, byte[] columnName, long increment) {
        if (rowKey == null) {
            throw new NullPointerException("rowKey must not be null");
        }
        if (columnName == null) {
            throw new NullPointerException("columnName must not be null");
        }
        TableName mapStatisticsCalleeTableName = tableNameProvider.getTableName(MAP_STATISTICS_CALLEE_VER2_STR);
        hbaseTemplate.incrementColumnValue(mapStatisticsCalleeTableName, rowKey, MAP_STATISTICS_CALLEE_VER2_CF_COUNTER, columnName, increment);
    }

    // shiming.li update 弃用
    //@Override
    public void flushAll2() {
        if (!useBulk) {
            throw new IllegalStateException();
        }
        // update statistics by rowkey and column for now. need to update it by rowkey later.
        Map<TableName, List<Increment>> incrementMap = bulkIncrementer.getIncrements(rowKeyDistributorByHashPrefix);

        for (Map.Entry<TableName, List<Increment>> e : incrementMap.entrySet()) {
            TableName tableName = e.getKey();
            List<Increment> increments = e.getValue();
            if (logger.isDebugEnabled()) {
                logger.debug("flush {} to [{}] Increment:{}", this.getClass().getSimpleName(), tableName.getNameAsString(), increments.size());
            }
            hbaseTemplate.increment(tableName, increments);
        }
    }

    @Override
    public void flushAll() {
        Map<RowInfo, Long> remove = bulkIncrementer.remove();
        for (Map.Entry<RowInfo, Long> entry : remove.entrySet()) {
            final RowInfo rowInfo = entry.getKey();
            long callCount = entry.getValue();
            RowKey rowKey = rowInfo.getRowKey();
            ColumnName columnName = rowInfo.getColumnName();
            byte[] row = rowKeyDistributorByHashPrefix.getDistributedKey(rowKey.getRowKey());
            // 查询
            Scan scan = new Scan();
            scan.addColumn(this.bulkIncrementer.getFamily(), columnName.getColumnName());
            List<Long> counts = hbaseTemplate.find(rowInfo.getTableName(), scan, (result, rowNum) -> {
                long value = bytesToLong(result.getValue(bulkIncrementer.getFamily(), columnName.getColumnName()));
//                logger.info("flushAll - 查询 - " + rowKey + ", " + columnName + ", rowNum=" + rowNum + ", value=" + value);
                return value;
            });
            long sum = 0;
            for (Long count : counts) {
                sum += count;
            }
            logger.info("caller flushAll - && 查询, " + rowKey + ", " + columnName + ", sum=" + sum + ", callCount=" + callCount);
            if (sum < callCount) {
                long count = callCount - sum;
                logger.info("caller flushAll - && - 增加, " + rowKey + ", " + columnName + ", count=" + count);
                // 增加
                this.hbaseTemplate.incrementColumnValue(rowInfo.getTableName(), row, this.bulkIncrementer.getFamily(), columnName.getColumnName(), count);
            }
        }
    }

    private byte[] getDistributedKey(byte[] rowKey) {
        return rowKeyDistributorByHashPrefix.getDistributedKey(rowKey);
    }

    public static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip
        long longNumber = buffer.getLong();
        buffer.clear();
        return longNumber;
    }
}