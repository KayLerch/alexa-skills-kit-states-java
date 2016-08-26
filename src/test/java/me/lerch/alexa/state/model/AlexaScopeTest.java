package me.lerch.alexa.state.model;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class AlexaScopeTest {
    @Test
    public void getValue() throws Exception {
        Assert.assertEquals(AlexaScope.APPLICATION.getValue(), 1);
        Assert.assertEquals(AlexaScope.USER.getValue(), 1);
        Assert.assertEquals(AlexaScope.SESSION.getValue(), 0);
    }

    @Test
    public void includes() throws Exception {
        Assert.assertTrue(AlexaScope.SESSION.includes(AlexaScope.APPLICATION));
        Assert.assertTrue(AlexaScope.SESSION.includes(AlexaScope.USER));
        Assert.assertTrue(AlexaScope.SESSION.includes(AlexaScope.SESSION));
        Assert.assertTrue(AlexaScope.USER.includes(AlexaScope.USER));
        Assert.assertTrue(AlexaScope.APPLICATION.includes(AlexaScope.APPLICATION));
        Assert.assertFalse(AlexaScope.APPLICATION.includes(AlexaScope.SESSION));
        Assert.assertFalse(AlexaScope.APPLICATION.includes(AlexaScope.USER));
        Assert.assertFalse(AlexaScope.USER.includes(AlexaScope.SESSION));
        Assert.assertFalse(AlexaScope.USER.includes(AlexaScope.APPLICATION));
    }

    @Test
    public void excludes() throws Exception {
        Assert.assertTrue(AlexaScope.APPLICATION.excludes(AlexaScope.SESSION));
        Assert.assertTrue(AlexaScope.APPLICATION.excludes(AlexaScope.USER));
        Assert.assertTrue(AlexaScope.USER.excludes(AlexaScope.APPLICATION));
        Assert.assertTrue(AlexaScope.USER.excludes(AlexaScope.SESSION));
        Assert.assertFalse(AlexaScope.APPLICATION.excludes(AlexaScope.APPLICATION));
        Assert.assertFalse(AlexaScope.USER.excludes(AlexaScope.USER));
        Assert.assertFalse(AlexaScope.SESSION.excludes(AlexaScope.SESSION));
        Assert.assertFalse(AlexaScope.SESSION.excludes(AlexaScope.USER));
        Assert.assertFalse(AlexaScope.SESSION.excludes(AlexaScope.APPLICATION));
    }
}