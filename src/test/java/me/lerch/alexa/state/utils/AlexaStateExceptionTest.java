package me.lerch.alexa.state.utils;

import me.lerch.alexa.state.handler.AlexaSessionStateHandler;
import me.lerch.alexa.state.model.AlexaStateModel;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class AlexaStateExceptionTest {
    private class Model extends AlexaStateModel {
        public Model() {}
    }

    @Test
    public void getModel() throws Exception {
        final Model model = new Model();
        final AlexaStateException e = AlexaStateException.create("").withModel(model).build();
        Assert.assertNotNull(e);
        Assert.assertEquals(model, e.getModel());

        final AlexaStateException e2 = AlexaStateException.create("").build();
        Assert.assertNull(e2.getModel());
    }

    @Test
    public void getHandler() throws Exception {
        final AlexaSessionStateHandler handler = new AlexaSessionStateHandler(null);
        final AlexaStateException e = AlexaStateException.create("").withHandler(handler).build();
        Assert.assertNotNull(e);
        Assert.assertEquals(handler, e.getHandler());
    }

    @Test
    public void create() throws Exception {
        final AlexaSessionStateHandler handler = new AlexaSessionStateHandler(null);
        final Model model = new Model();
        final IOException ioE = new IOException();
        final String message = "An error message";
        final AlexaStateException e = AlexaStateException.create(message).withHandler(handler)
                .withModel(model).withCause(ioE).build();
        Assert.assertNotNull(e);
        Assert.assertEquals(model, e.getModel());
        Assert.assertEquals(handler, e.getHandler());
        Assert.assertEquals(ioE, e.getCause());
        Assert.assertEquals(message, e.getMessage());
    }

}