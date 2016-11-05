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
import io.klerch.alexa.state.utils.AlexaStateException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public abstract class AlexaStateHandlerTest<THandler extends AlexaStateHandler> {
    static Session session;
    THandler handler;
    final String modelId = "modelId";
    final String modelId2 = "modelId2";
    private final String modelSampleStringValue = "value";
    private final Boolean modelSampleApplicationValue = true;
    private final String modelSampleUserValue = "userValue";
    final String stateModelValue = "stateValue";
    final String absentModelId = "nonexisting";

    public abstract THandler givenHandler() throws Exception;

    AlexaStateHandlerTest() {
        try {
            handler = givenHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @BeforeClass
    public static void createSession() {
        final Application application = new Application("test-" + UUID.randomUUID().toString());
        final User user = User.builder().withUserId("test-" + UUID.randomUUID().toString()).withAccessToken("test-" + UUID.randomUUID().toString()).build();
        session = Session.builder().withSessionId("test-" + UUID.randomUUID().toString())
                .withApplication(application).withUser(user).build();
    }

    Model givenModel(final String id) {
        final Model model = new Model();
        model.setId(id);
        model.setHandler(handler);
        model.sampleString = modelSampleStringValue;
        model.sampleApplication = modelSampleApplicationValue;
        model.sampleUser = modelSampleUserValue;
        model.sampleIgnore = "valueToDissapear";
        model.sampleIgnoreSession = "sessionValueToDissapear";
        model.sampleIgnoreUser = "userValueToDissappear";
        model.sampleIgnoreApplication = "appValueToDissappear";
        return model;
    }

    private AlexaStateObject givenStateObject(final String id, final AlexaScope scope) {
        return new AlexaStateObject(id, stateModelValue, scope);
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
    public void readAbsentModel() throws Exception {
        Assert.assertFalse(handler.readModel(Model.class, absentModelId).isPresent());
    }

    @Test
    public void readAbsentSingleValue() throws Exception {
        Assert.assertFalse(handler.readValue(absentModelId).isPresent());
        Assert.assertFalse(handler.readValue(absentModelId, AlexaScope.APPLICATION).isPresent());
        Assert.assertTrue(handler.readValues(Collections.singletonList(absentModelId)).isEmpty());
    }

    @Test
    public void crudSingleValueToSession() throws Exception {
        session.getAttributes().clear();
        crudSingleValueInScope(modelId, AlexaScope.SESSION);
    }

    @Test
    public void crudSingleValueToUser() throws Exception {
        session.getAttributes().clear();
        crudSingleValueInScope(modelId, AlexaScope.USER);
    }

    @Test
    public void crudSingleValueToApplication() throws Exception {
        session.getAttributes().clear();
        crudSingleValueInScope(modelId, AlexaScope.APPLICATION);
    }

    @Test
    public void crudMultiSingleValuesToSession() throws Exception {
        session.getAttributes().clear();

        final AlexaStateObject stateObject1 = givenStateObject(modelId, AlexaScope.SESSION);
        final AlexaStateObject stateObject2 = givenStateObject(modelId2, AlexaScope.SESSION);
        crudMultiSingleValuesInScope(Arrays.asList(stateObject1, stateObject2));
    }

    @Test
    public void crudMultiSingleValuesToUser() throws Exception {
        session.getAttributes().clear();

        final AlexaStateObject stateObject1 = givenStateObject(modelId, AlexaScope.USER);
        final AlexaStateObject stateObject2 = givenStateObject(modelId2, AlexaScope.USER);
        crudMultiSingleValuesInScope(Arrays.asList(stateObject1, stateObject2));
    }

    @Test
    public void crudMultiSingleValuesToApplication() throws Exception {
        session.getAttributes().clear();

        final AlexaStateObject stateObject1 = givenStateObject(modelId, AlexaScope.APPLICATION);
        final AlexaStateObject stateObject2 = givenStateObject(modelId2, AlexaScope.APPLICATION);
        crudMultiSingleValuesInScope(Arrays.asList(stateObject1, stateObject2));
    }

    @Test
    public void crudMultiSingleValuesToSessionAndApplication() throws Exception {
        session.getAttributes().clear();

        final AlexaStateObject stateObject1 = givenStateObject(modelId, AlexaScope.APPLICATION);
        final AlexaStateObject stateObject2 = givenStateObject(modelId2, AlexaScope.SESSION);
        crudMultiSingleValuesInScope(Arrays.asList(stateObject1, stateObject2));
    }

    @Test
    public void crudMultiSingleValuesToSessionAndUser() throws Exception {
        session.getAttributes().clear();

        final AlexaStateObject stateObject1 = givenStateObject(modelId, AlexaScope.USER);
        final AlexaStateObject stateObject2 = givenStateObject(modelId2, AlexaScope.SESSION);
        crudMultiSingleValuesInScope(Arrays.asList(stateObject1, stateObject2));
    }

    @Test
    public void crudMultiSingleValuesToUserAndApplication() throws Exception {
        session.getAttributes().clear();

        final AlexaStateObject stateObject1 = givenStateObject(modelId, AlexaScope.USER);
        final AlexaStateObject stateObject2 = givenStateObject(modelId2, AlexaScope.APPLICATION);
        crudMultiSingleValuesInScope(Arrays.asList(stateObject1, stateObject2));
    }

    @Test
    public void crudModelSingleton() throws Exception {
        session.getAttributes().clear();
        crudModel(givenModel(null));
    }

    @Test
    public void crudModelWithId() throws Exception {
        session.getAttributes().clear();
        crudModel(givenModel(modelId));
    }

    private void crudModel(final Model model) throws Exception {
        handler.writeModel(model);
        // must always exist in session scope
        existsInScope(model, AlexaScope.SESSION);

        assertTrue(session.getAttributes().containsKey(model.getAttributeKey()));

        if (!(handler instanceof AlexaSessionStateHandler)) {
            // must exist in user and application scope as well
            existsInScope(model, AlexaScope.USER);
            existsInScope(model, AlexaScope.APPLICATION);
            // clean session attributes to ensure values come from mocked store
            // but do not this for session handler as the session itself is the store
            session.getAttributes().clear();
        }

        final Optional<Model> model2 = model.getId() == null ? handler.readModel(Model.class) : handler.readModel(Model.class, model.getId());
        assertTrue(model2.isPresent());
        assertEquals(model.getId(), model2.get().getId());
        assertEquals(model.sampleUser, model2.get().sampleUser);
        assertEquals(model.sampleApplication, model2.get().sampleApplication);
        assertNotEquals(model.sampleIgnore, model2.get().sampleIgnore);
        assertNotEquals(model.sampleIgnoreSession, model2.get().sampleIgnoreSession);
        assertNotEquals(model.sampleIgnoreUser, model2.get().sampleIgnoreUser);
        assertNotEquals(model.sampleIgnoreApplication, model2.get().sampleIgnoreApplication);

        if (handler instanceof AlexaSessionStateHandler)
            assertEquals(modelSampleStringValue, model2.get().sampleString);
        else {
            assertNull(model2.get().sampleString);
        }
        handler.removeModel(model);
        assertFalse(session.getAttributes().containsKey(model.getAttributeKey()));
    }

    private void existsInScope(final Model model, final AlexaScope scope) throws Exception {
        if (model.getId() == null) {
            assertTrue(handler.exists(Model.class));
            assertTrue(handler.exists(Model.class, scope));
        } else if (modelId.equals(model.getId())) {
            assertTrue(handler.exists(Model.class, modelId));
            assertTrue(handler.exists(Model.class, modelId, scope));
        } else if (modelId2.equals(model.getId())) {
            assertTrue(handler.exists(Model.class, modelId2));
            assertTrue(handler.exists(Model.class, modelId2, scope));
        }
        assertTrue(handler.exists(model.getAttributeKey()));
        assertTrue(handler.exists(model.getAttributeKey(), scope));
    }

    private void crudMultiSingleValuesInScope(final Collection<AlexaStateObject> values) throws Exception {
        handler.writeValues(values);

        // read/write all
        values.forEach(stateObject -> {
            try {
                checkForStateObject(stateObject.getId(), stateObject.getScope());
            } catch (AlexaStateException e) {
                e.printStackTrace();
            }
        });

        // remove all
        handler.removeValues(values.stream().map(AlexaStateObject::getId).collect(Collectors.toList()));

        // false positive read all
        values.forEach(stateObject -> {
            try {
                assertFalse(handler.exists(stateObject.getId()));
                assertFalse(handler.readValue(stateObject.getId()).isPresent());
            } catch (AlexaStateException e) {
                e.printStackTrace();
            }
        });
    }

    private void crudSingleValueInScope(final String id, final AlexaScope scope) throws Exception {
        final AlexaStateObject stateObject = givenStateObject(id, scope);
        handler.writeValue(stateObject);

        checkForStateObject(id, scope);

        handler.removeValue(id);
        assertFalse(handler.exists(id));
        assertFalse(handler.readValue(id).isPresent());
    }

    private void checkForStateObject(final String id, final AlexaScope scope) throws AlexaStateException {
        if (AlexaScope.SESSION.includes(scope)) {
            assertTrue(handler.exists(id));
        }
        assertTrue(handler.exists(id ,scope));
        final Optional<AlexaStateObject> stateObject = handler.readValue(id, scope);
        assertTrue(stateObject.isPresent());
        assertEquals(stateObject.get().getId(), id);
        assertEquals(stateObject.get().getValue(), stateModelValue);
        assertEquals(stateObject.get().getScope(), scope);
    }
}
