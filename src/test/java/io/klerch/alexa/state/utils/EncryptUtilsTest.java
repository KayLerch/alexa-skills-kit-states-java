/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.utils;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class EncryptUtilsTest {
    @Test
    public void encryptSha1() throws Exception {
        Assert.assertNotNull(EncryptUtils.encryptSha1("value"));
        assertEquals(EncryptUtils.encryptSha1(null), "");
        Assert.assertNotNull(EncryptUtils.encryptSha1(""));
    }
}