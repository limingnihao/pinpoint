package com.navercorp.pinpoint.common.server.bo.serializer.trace.v2;

import com.navercorp.pinpoint.common.server.bo.serializer.RowKeyDecoder;
import com.navercorp.pinpoint.common.util.BytesUtils;
import com.navercorp.pinpoint.common.util.TransactionId;
import org.springframework.stereotype.Component;

/**
 * @author Woonduk Kang(emeroad)
 */
@Component
public class TraceRowKeyDecoderV2 implements RowKeyDecoder<TransactionId> {

    public static final int MAX_LEN = TraceRowKeyEncoderV2.MAX_LEN;
    public static final int DISTRIBUTE_HASH_SIZE = TraceRowKeyEncoderV2.DISTRIBUTE_HASH_SIZE;

    private final int distributeHashSize;


    public TraceRowKeyDecoderV2() {
        this(DISTRIBUTE_HASH_SIZE);
    }

    public TraceRowKeyDecoderV2(int distributeHashSize) {
        this.distributeHashSize = distributeHashSize;
    }


    @Override
    public TransactionId decodeRowKey(byte[] rowkey) {
        if (rowkey == null) {
            throw new NullPointerException("rowkey must not be null");
        }

        return readTransactionId(rowkey, distributeHashSize);
    }

    private TransactionId readTransactionId(byte[] rowKey, int offset) {

        String agentId = BytesUtils.toStringAndRightTrim(rowKey, offset, MAX_LEN);
        long agentStartTime = BytesUtils.bytesToLong(rowKey, offset + MAX_LEN);
        long transactionSequence = BytesUtils.bytesToLong(rowKey, offset + BytesUtils.LONG_BYTE_LENGTH + MAX_LEN);

        return new TransactionId(agentId, agentStartTime, transactionSequence);
    }

    // for test
    public TransactionId readTransactionId(byte[] rowKey) {
        return readTransactionId(rowKey, 0);
    }
}
