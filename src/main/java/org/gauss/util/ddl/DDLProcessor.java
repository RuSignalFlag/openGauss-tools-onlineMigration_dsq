/*
 * Copyright (c) 2021 Huawei Technologies Co.,Ltd.
 */

package org.gauss.util.ddl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.gauss.jsonstruct.DDLValueStruct;
import org.gauss.jsonstruct.SourceStruct;
import org.gauss.util.JDBCExecutor;
import org.gauss.util.OpenGaussConstant;
import org.gauss.util.Processor;
import org.gauss.util.ddl.convert.DDLConvertHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DDLProcessor extends Processor {
    private static final Logger LOGGER = LoggerFactory.getLogger(DDLProcessor.class);
    private final ObjectMapper topicMapper = new ObjectMapper();
    private final JDBCExecutor ddlExecutor = new JDBCExecutor();
    private final DDLCacheController ddlCacheController = DDLCacheController.getInstance();

    public DDLProcessor() {
        topicMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void process(ConsumerRecord<String, String> record) {
        DDLValueStruct value = null;
        try {
            value = topicMapper.readValue(record.value(), DDLValueStruct.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        SourceStruct source = value.getPayload().getSource();
        String snapshot = source.getSnapshot();
        if (snapshot.equals("true") || snapshot.equals("last")) {
            return;
        }
        String ddl = value.getPayload().getDdl();
        LOGGER.info("get DDL SQL: {}", ddl);

        String openGaussDDL = DDLConvertHandler.getDDlConvert(value.getPayload()).convertToOpenGaussDDL(value);
        // when get ddl sql ,if sql contains rename table ,drop table, drop column , must compare current dml scn and ddl scn
        if (null != openGaussDDL && needCache(value.getPayload())) {
            LOGGER.info("add ddl sql: {} to cache,ddl_scn: {}", openGaussDDL, source.getCommit_scn());
            ddlCacheController.addDdl(Long.valueOf(source.getCommit_scn()), openGaussDDL);
        } else {
            ddlExecutor.executeDDL(openGaussDDL);
        }
    }


    public boolean needCache(DDLValueStruct.PayloadStruct payload) {
        String ddl = payload.getDdl();
        if (StringUtils.containsIgnoreCase(ddl, OpenGaussConstant.TABLE_RENAME)) {
            return true;
        } else if (payload.getTableChanges()
                          .stream()
                          .anyMatch((tableChangeStruct -> tableChangeStruct.getType().equals(OpenGaussConstant.TABLE_ALTER)))) {
            return true;
        } else if (StringUtils.containsIgnoreCase(payload.getTableChanges().get(0).getType(), OpenGaussConstant.TABLE_PRIMARY_KEY_DROP)) {
            return true;
        }
        return false;
    }
}
