/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.model;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class AlexaScopeTest {
    @Test
    public void getValue() throws Exception {
        assertEquals(AlexaScope.APPLICATION.getValue(), 1);
        assertEquals(AlexaScope.USER.getValue(), 1);
        assertEquals(AlexaScope.SESSION.getValue(), 0);
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
    public void isIn() throws Exception {
        Assert.assertTrue(AlexaScope.SESSION.isIn(AlexaScope.SESSION));
        Assert.assertTrue(AlexaScope.SESSION.isIn(AlexaScope.SESSION, AlexaScope.USER));
        Assert.assertTrue(AlexaScope.SESSION.isIn(AlexaScope.SESSION, AlexaScope.APPLICATION));
        Assert.assertFalse(AlexaScope.SESSION.isIn(AlexaScope.USER, AlexaScope.APPLICATION));
        Assert.assertTrue(AlexaScope.USER.isIn(AlexaScope.USER));
        Assert.assertTrue(AlexaScope.USER.isIn(AlexaScope.SESSION, AlexaScope.USER));
        Assert.assertTrue(AlexaScope.USER.isIn(AlexaScope.USER, AlexaScope.APPLICATION));
        Assert.assertFalse(AlexaScope.USER.isIn(AlexaScope.SESSION, AlexaScope.APPLICATION));
        Assert.assertTrue(AlexaScope.APPLICATION.isIn(AlexaScope.APPLICATION));
        Assert.assertTrue(AlexaScope.APPLICATION.isIn(AlexaScope.APPLICATION, AlexaScope.USER));
        Assert.assertTrue(AlexaScope.APPLICATION.isIn(AlexaScope.SESSION, AlexaScope.APPLICATION));
        Assert.assertFalse(AlexaScope.APPLICATION.isIn(AlexaScope.SESSION, AlexaScope.USER));
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