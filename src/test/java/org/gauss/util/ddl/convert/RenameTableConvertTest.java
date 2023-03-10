package org.gauss.util.ddl.convert;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gauss.jsonstruct.DDLValueStruct;
import org.gauss.util.TestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * @author saxisuer
 * @Description
 * @date 2022/6/29
 * @email sheng.pu@enmotech.com
 * @COMPANY ENMOTECH
 */
public class RenameTableConvertTest {

    private final ObjectMapper topicMapper = new ObjectMapper();

    private RenameTableConvert renameTableConvert = new RenameTableConvert();

    private DDLValueStruct ddlValueStruct = null;
    private String topicValue;

    @Before
    public void before() throws IOException {
        topicMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        topicValue = TestUtil.readJsonFromFile("RenameTableTopicValue.json");
        ddlValueStruct = topicMapper.readValue(topicValue, DDLValueStruct.class);
    }

    @Test
    public void test() {
        List<String> s = renameTableConvert.convertToOpenGaussDDL(ddlValueStruct);
        System.out.println(s);
        Assert.assertEquals(s.size(), 1);
        Assert.assertEquals("ALTER TABLE \"C##ROMA_LOGMINER\".\"T_DDL_0031\" RENAME TO \"T_DDL_0031_01\"", s.get(0));
    }
}
