package io.klerch.alexa.state.handler;

import com.amazon.speech.speechlet.Application;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.User;
import io.klerch.alexa.state.model.Model;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public abstract class AlexaStateHandlerTest<THandler extends AlexaStateHandler> {
    THandler handler;
    Session session;
    final String modelId = "modelId";

    public abstract THandler getHandler();

    @Before
    public void setUp() {
        final Application application = new Application("applicationId");
        final User user = User.builder().withUserId("userId").withAccessToken("accessToken").build();
        session = Session.builder().withSessionId("sessionId")
                .withApplication(application).withUser(user).build();
        handler = getHandler();
    }

    @Test
    public void getAttributeKey() throws Exception {
        final Model model = new Model();
        final String expected = model.getClass().getTypeName();
        assertEquals(expected, handler.getAttributeKey(model));
        assertEquals(expected, handler.getAttributeKey(Model.class, ""));
        assertEquals(expected, handler.getAttributeKey(Model.class, null));

        final String id = "id";
        final String expected2 = expected + ":" + id;
        model.setId(id);
        assertEquals(expected2, handler.getAttributeKey(model));
        assertEquals(expected2, handler.getAttributeKey(Model.class, id));
    }

    @Test
    public void createModel() throws Exception {
        final Model model = handler.createModel(Model.class);
        assertNotNull(model);
        assertNull(model.getId());
        assertEquals(handler, model.getHandler());
    }

    @Test
    public void createModelWithId() throws Exception {
        final String id = "id";
        final Model model = handler.createModel(Model.class, id);
        assertNotNull(model);
        assertEquals(handler, model.getHandler());
        assertEquals(id, model.getId());
    }

    @Test
    public void crudModel() throws Exception {
        session.getAttributes().clear();

        final String value = "value";
        final String valueU = "sampleUser";

        final Model model = new Model();
        model.sampleString = value;
        model.sampleApplication = true;
        model.sampleUser = valueU;

        final String key = handler.getAttributeKey(model);

        handler.writeModel(model);
        assertTrue(session.getAttributes().containsKey(key));

        // clean session attributes to ensure values come from mocked dynamoDB
        session.getAttributes().clear();

        final Optional<Model> model2 = handler.readModel(Model.class);
        assertTrue(model2.isPresent());
        assertNull(model2.get().getId());
        assertNull(model2.get().sampleString);
        assertEquals(true, model2.get().sampleApplication);
        assertEquals(valueU, model2.get().sampleUser);

        handler.removeModel(model);
        assertFalse(session.getAttributes().containsKey(key));
    }

    @Test
    public void crudModelWithId() throws Exception {
        session.getAttributes().clear();

        final String value = "value";
        final String valueU = "sampleUser";

        final Model model = new Model();
        model.setId(modelId);
        model.sampleString = value;
        model.sampleApplication = true;
        model.sampleUser = valueU;

        final String key = handler.getAttributeKey(model);

        handler.writeModel(model);
        assertTrue(session.getAttributes().containsKey(key));

        // clean session attributes to ensure values come from mocked dynamoDB
        session.getAttributes().clear();

        final Optional<Model> model2 = handler.readModel(Model.class, modelId);
        assertTrue(model2.isPresent());
        assertEquals(modelId, model2.get().getId());
        assertNull(model2.get().sampleString);
        assertEquals(true, model2.get().sampleApplication);
        assertEquals(valueU, model2.get().sampleUser);

        handler.removeModel(model);
        assertFalse(session.getAttributes().containsKey(key));
    }
}
