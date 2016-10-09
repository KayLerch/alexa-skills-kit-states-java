/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

import com.amazon.speech.speechlet.Application;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.User;
import io.klerch.alexa.state.model.AlexaScope;
import io.klerch.alexa.state.model.AlexaStateObject;
import io.klerch.alexa.state.model.dummies.Model;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public abstract class AlexaStateHandlerTest<THandler extends AlexaStateHandler> {
    THandler handler;
    Session session;
    final String modelId = "modelId";
    final String absentModelId = "nonexisting";

    public abstract THandler getHandler();

    @Before
    public void setUp() {
        final Application application = new Application("applicationId2");
        final User user = User.builder().withUserId("userId").withAccessToken("accessToken").build();
        session = Session.builder().withSessionId("sessionId")
                .withApplication(application).withUser(user).build();
        handler = getHandler();
    }

    @Test
    public void getSession() throws Exception {
        assertEquals(session, handler.getSession());
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

        final String key = model.getAttributeKey();

        handler.writeModel(model);
        assertTrue(session.getAttributes().containsKey(key));

        // clean session attributes to ensure values come from mocked dynamoDB
        // but do not this for session handler as the session itself is the store
        if (!(handler instanceof AlexaSessionStateHandler))
            session.getAttributes().clear();

        final Optional<Model> model2 = handler.readModel(Model.class);
        assertTrue(model2.isPresent());
        assertNull(model2.get().getId());
        assertEquals(true, model2.get().sampleApplication);
        assertEquals(valueU, model2.get().sampleUser);

        if (handler instanceof AlexaSessionStateHandler)
            assertEquals(value, model2.get().sampleString);
        else {
            assertNull(model2.get().sampleString);
        }

        handler.removeModel(model);
        assertFalse(session.getAttributes().containsKey(key));
    }

    @Test
    public void readAbsentModel() throws Exception {
        Assert.assertFalse(handler.readModel(Model.class, absentModelId).isPresent());
    }

    @Test
    @Ignore
    public void crudObject() throws Exception {
        session.getAttributes().clear();
        final AlexaStateObject stateObject = new AlexaStateObject("id", "value", AlexaScope.SESSION);
        handler.writeValue(stateObject);
        assertTrue(session.getAttributes().containsKey("id"));


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

        final String key = model.getAttributeKey();

        handler.writeModel(model);
        assertTrue(session.getAttributes().containsKey(key));

        // clean session attributes to ensure values come from mocked store
        // but do not this for session handler as the session itself is the store
        if (!(handler instanceof AlexaSessionStateHandler))
            session.getAttributes().clear();

        final Optional<Model> model2 = handler.readModel(Model.class, modelId);
        assertTrue(model2.isPresent());
        assertEquals(modelId, model2.get().getId());
        assertEquals(true, model2.get().sampleApplication);
        assertEquals(valueU, model2.get().sampleUser);

        if (handler instanceof AlexaSessionStateHandler)
            assertEquals(value, model2.get().sampleString);
        else {
            assertNull(model2.get().sampleString);
        }

        handler.removeModel(model);
        assertFalse(session.getAttributes().containsKey(key));
    }
}
