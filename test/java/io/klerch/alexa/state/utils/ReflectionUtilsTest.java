package io.klerch.alexa.state.utils;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class ReflectionUtilsTest {

    public String getSample() {
        return "";
    }

    public void setSample() {
    }

    @Test
    public void getGetterOnExisting() throws Exception {
        final Method method = ReflectionUtils.getGetter(this, "sample");
        Assert.assertNotNull(method);
        Assert.assertEquals(method.getName(), "getSample");
    }

    @Test
    public void getGetterOnNotExisting() throws Exception {
        final Method method = ReflectionUtils.getGetter(this, "sample1");
        Assert.assertNull(method);
    }

    @Test
    public void getSetterOnExisting() throws Exception {
        final Method method = ReflectionUtils.getSetter(this, "sample");
        Assert.assertNotNull(method);
        Assert.assertEquals(method.getName(), "setSample");
    }

    @Test
    public void getSetterOnNotExisting() throws Exception {
        final Method method = ReflectionUtils.getSetter(this, "sample1");
        Assert.assertNull(method);
    }

}