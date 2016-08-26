package me.lerch.alexa.state.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

public class ConversionUtilsTest {
    private void testMapJsonSingleValue(final String key, final Object value) {
        final String json1 = "{\"" + key + "\":\"" + value + "\"}";
        final Map<String, Object> map1 = ConversionUtils.mapJson(json1);
        Assert.assertNotNull(map1);
        Assert.assertTrue(map1.keySet().size() == 1);
        Assert.assertTrue(map1.containsKey(key));
        Assert.assertEquals(map1.get(key), value.toString());
    }

    @Test
    public void mapJsonSingleValue() throws Exception {
        testMapJsonSingleValue("key", true);
        testMapJsonSingleValue("key", "value");
        testMapJsonSingleValue("key", "%&!#+,'");
        testMapJsonSingleValue("key", 123.234);
        testMapJsonSingleValue("key", 23);
        testMapJsonSingleValue("key", Arrays.asList("value1", "value2", "value3"));
    }

    @Test
    public void mapJsonNoValue() throws Exception {
        Assert.assertTrue(ConversionUtils.mapJson("{}").isEmpty());
        Assert.assertTrue(ConversionUtils.mapJson("{}").isEmpty());
        Assert.assertTrue(ConversionUtils.mapJson("{}").isEmpty());
    }
}