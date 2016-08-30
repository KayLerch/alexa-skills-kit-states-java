/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.utils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public class ReflectionUtils {
    /**
     * Returns the method of a given object which is the getter-method of a given field
     * whose name is expected to be like getFieldname. Returns null if method not found.
     * @param o The object containing the getter-method.
     * @param fieldName The field whose getter-method is desired
     * @return the reflected method. Is null if method was not found in the given object
     */
    public static Method getGetter(final Object o, final String fieldName) {
        Optional<Method> method = getMethodWithPrefix(o, fieldName, "get");
        return method.orElse(null);
    }

    /**
     * Returns the method of a given object which is the setter-method of a given field
     * whose name is expected to be like setFieldname. Returns null if method not found.
     * @param o The object containing the setter-method.
     * @param fieldName The field whose setter-method is desired
     * @return the reflected method. Is null if method was not found in the given object
     */
    public static Method getSetter(final Object o, final String fieldName) {
        Optional<Method> method = getMethodWithPrefix(o, fieldName, "set");
        return method.orElse(null);
    }

    /**
     * Returns any method whose name starts with given prefix and ends with given fieldname where
     * first letter of the fieldname will be uppercased.
     * @param o The object containing the desired method.
     * @param fieldName The fieldname contained in the name of the desired method.
     * @param prefix A prefix string of the method-name
     * @return The reflected method. Is empty if method was not found.
     */
    private static Optional<Method> getMethodWithPrefix(final Object o, final String fieldName, final String prefix) {
        final String methodName = prefix + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        return Arrays.stream(o.getClass().getMethods()).filter(method -> method.getName().equals(methodName)).findFirst();
    }
}
