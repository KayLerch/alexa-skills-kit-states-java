package io.klerch.alexa.state.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class AlexaStateObjectTest {
    @Test
    public void getKey() throws Exception {
        final AlexaStateObject stateObject = new AlexaStateObject("id", "value");
        assertEquals(stateObject.getId(), "id");
    }

    @Test
    public void getValue() throws Exception {
        final AlexaStateObject stateObject = new AlexaStateObject("id", "value");
        assertEquals(stateObject.getValue(), "value");
    }

    @Test
    public void getScope() throws Exception {
        final AlexaStateObject stateObject = new AlexaStateObject("id", "value");
        assertEquals(stateObject.getScope(), AlexaScope.SESSION);

        final AlexaStateObject stateObject2 = new AlexaStateObject("id", "value", AlexaScope.USER);
        assertEquals(stateObject2.getScope(), AlexaScope.USER);
    }

}