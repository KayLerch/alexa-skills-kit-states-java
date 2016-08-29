package io.klerch.alexa.state.model;

import io.klerch.alexa.state.handler.AlexaSessionStateHandler;
import org.junit.Test;

import static org.junit.Assert.*;

public class AlexaStateModelFactoryTest {
    @Test
    public void createModel() throws Exception {
        final AlexaSessionStateHandler handler = new AlexaSessionStateHandler(null);
        final Model model = AlexaStateModelFactory.createModel(Model.class, handler);
        assertNotNull(model);
        assertEquals(handler, model.getHandler());
        assertNull(model.getId());
    }

    @Test
    public void createModelWithId() throws Exception {
        final String id = "id";
        final AlexaSessionStateHandler handler = new AlexaSessionStateHandler(null);
        final Model model = AlexaStateModelFactory.createModel(Model.class, handler, id);
        assertNotNull(model);
        assertEquals(handler, model.getHandler());
        assertEquals(id, model.getId());
    }

}