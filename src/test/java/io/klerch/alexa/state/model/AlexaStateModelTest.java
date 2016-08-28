package io.klerch.alexa.state.model;

import com.amazon.speech.speechlet.Application;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.User;
import io.klerch.alexa.state.handler.AlexaSessionStateHandler;
import io.klerch.alexa.state.handler.AlexaStateHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class AlexaStateModelTest {
    private Session session;
    private final String attributeKey = "key";

    @Before
    public void createSession() {
        final Application application = new Application("applicationId");
        final User user = User.builder().withUserId("userId").withAccessToken("accessToken").build();
        session = Session.builder().withSessionId("sessionId")
                .withApplication(application).withUser(user).build();
        session.setAttribute(attributeKey, "value");
    }

    @Test
    public void getSetValidId() throws Exception {
        final String id = "abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ_0123456789";
        final Model model = new Model();
        model.setId(id);
        assertEquals(id, model.getId());
    }

    public void getSetNullId() throws Exception {
        final Model model = new Model();
        model.setId(null);
        Assert.assertNull(model.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getSetInvalidId() {
        final String id = "abcd/f";
        final Model model = new Model();
        model.setId(id);
    }

    @Test
    public void getSetHandler() throws Exception {
        final AlexaStateHandler handler = new AlexaSessionStateHandler(session);
        final Model model = new Model();
        model.setHandler(handler);
        Assert.assertEquals(handler, model.getHandler());
    }

    @Test
    public void withHandler() throws Exception {
        final AlexaStateHandler handler = new AlexaSessionStateHandler(session);
        final Model model1 = new Model();
        final AlexaStateModel model2 = model1.withHandler(handler);
        assertEquals(model1, model2);
        Assert.assertEquals(handler, model1.getHandler());
    }

    @Test (expected = NullPointerException.class)
    public void saveStateWithoutHandler() throws Exception {
        new Model().saveState();
    }

    @Test
    public void saveAndRemoveStateWithHandler() throws Exception {
        final Model model = new Model();
        final AlexaSessionStateHandler handler = new AlexaSessionStateHandler(session);
        model.withHandler(handler).saveState();
        Assert.assertNotNull(session.getAttribute(handler.getAttributeKey(model)));
        model.removeState();
        Assert.assertNull(session.getAttribute(handler.getAttributeKey(model)));
    }

    @Test (expected = NullPointerException.class)
    public void removeStateWithoutHandler() throws Exception {
        new Model().removeState();
    }

    @Test
    public void createWithHandler() throws Exception {
        final AlexaSessionStateHandler handler = new AlexaSessionStateHandler(session);
        Model model = AlexaStateModel.create(Model.class).withHandler(handler).build();
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getHandler(), handler);
    }

    @Test
    public void createWithHandlerAndId() throws Exception {
        final String id = "id";
        final AlexaSessionStateHandler handler = new AlexaSessionStateHandler(session);
        Model model = AlexaStateModel.create(Model.class).withHandler(handler).withId(id).build();
        Assert.assertNotNull(model);
        Assert.assertEquals(model.getHandler(), handler);
        assertEquals(model.getId(), id);
    }

    @Test (expected = NullPointerException.class)
    public void createWithoutHandler() throws Exception {
        AlexaStateModel.create(Model.class).build();
    }

    @Test
    public void getExisting() throws Exception {
        Model model = new Model();
        final String value = "value";
        model.sampleString = value;
        final Field field = model.getClass().getField("sampleString");
        assertEquals(model.get(field), value);
    }

    @Test
    public void set() throws Exception {

    }

    @Test
    public void fromJSON() throws Exception {

    }

    @Test
    public void fromJSON1() throws Exception {

    }

    @Test
    public void toJSON() throws Exception {

    }

    @Test
    public void toMap() throws Exception {

    }

    @Test
    public void hasSessionScopedField() throws Exception {

    }

    @Test
    public void hasUserScopedField() throws Exception {

    }

    @Test
    public void hasApplicationScopedField() throws Exception {

    }

    @Test
    public void getSaveStateFields() throws Exception {

    }

    @Test
    public void getSaveStateFields1() throws Exception {

    }

}