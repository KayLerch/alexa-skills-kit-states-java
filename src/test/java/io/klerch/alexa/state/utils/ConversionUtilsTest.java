package io.klerch.alexa.state.utils;

import org.junit.Assert;
import org.junit.Test;
import java.util.Map;

public class ConversionUtilsTest {
    private void testMapJsonSingleNonStringValue(final String json, final String key, final Object value) {
        final Map<String, Object> map1 = ConversionUtils.mapJson(json);
        Assert.assertNotNull(map1);
        Assert.assertTrue(map1.keySet().size() == 1);
        Assert.assertTrue(map1.containsKey(key));
        Assert.assertEquals(map1.get(key), value);
    }

    private void testMapJsonSingleNonStringValue(final String key, final Object value) {
        final String json1 = "{\"" + key + "\":" + value + "}";
        testMapJsonSingleNonStringValue(json1, key, value);
    }

    private void testMapJsonSingleStringValue(final String key, final String value) {
        final String json1 = "{\"" + key + "\": \"" + value + "\"}";
        testMapJsonSingleNonStringValue(json1, key, value);
    }

    @Test
    public void mapJsonSingleNonStringValue() throws Exception {
        testMapJsonSingleNonStringValue("key", true);
        testMapJsonSingleNonStringValue("key", 123.234);
        testMapJsonSingleNonStringValue("key", 23);
    }

    @Test
    public void mapJsonSingleNStringValue() throws Exception {
        testMapJsonSingleStringValue("key", "value");
        testMapJsonSingleStringValue("key", "%&!#+,'");
    }

    @Test
    public void mapJsonNoValue() throws Exception {
        Assert.assertTrue(ConversionUtils.mapJson("{}").isEmpty());
    }
}