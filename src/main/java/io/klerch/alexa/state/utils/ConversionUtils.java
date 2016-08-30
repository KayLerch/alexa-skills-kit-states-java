/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConversionUtils {
    /**
     * A json-string of key-value pairs is read out as a map
     * @param json json-string of key-value pairs
     * @return a map with corresponding key-value paris
     */
    public static Map<String, Object> mapJson(final String json) {
        final ObjectMapper mapper = new ObjectMapper();
        if (json == null || json.isEmpty()) new HashMap<>();
        final TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
        try {
            // read jsonString into map
            return mapper.readValue(json, typeRef);
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }
}
