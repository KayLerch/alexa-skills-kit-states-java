/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

import com.amazonaws.services.iot.AWSIot;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.CreateThingResult;
import com.amazonaws.services.iot.model.ListThingsResult;
import com.amazonaws.services.iotdata.AWSIotData;
import com.amazonaws.services.iotdata.AWSIotDataClient;
import com.amazonaws.services.iotdata.model.GetThingShadowRequest;
import com.amazonaws.services.iotdata.model.GetThingShadowResult;
import com.amazonaws.services.iotdata.model.UpdateThingShadowResult;
import com.amazonaws.services.s3.model.S3Object;
import io.klerch.alexa.state.model.AlexaScope;
import io.klerch.alexa.state.model.AlexaStateModel;
import io.klerch.alexa.state.model.dummies.Model;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class AWSIotStateHandlerTest extends AlexaStateHandlerTest<AWSIotStateHandler> {
    @Override
    public AWSIotStateHandler givenHandler() {
        // mock the AWS connection client for creating and listing things
        final AWSIot iotClient = Mockito.mock(AWSIotClient.class, (Answer) invocationOnMock -> {
            // on thing created return empty result. it won't be considered by the handler anyway
            if (invocationOnMock.getMethod().getName().equals("createThing")) return new CreateThingResult();
            // if things will be listed always return empty list
            // results in this handler trying to create the thing before each read/write
            if (invocationOnMock.getMethod().getName().equals("listThings")) return new ListThingsResult();
            return null;
        });
        // mock the AWS connection client for reading and writing to thing shadows
        final AWSIotData iotData = Mockito.mock(AWSIotDataClient.class, (Answer) invocationOnMock -> {
            if (invocationOnMock.getMethod().getName().equals("getThingShadow")) {
                // just a dummy handler to get the generated thing shadow names
                final AWSIotStateHandler dummyHandler = new AWSIotStateHandler(session);
                // keys for models
                final String keyModel1 = AlexaStateModel.getAttributeKey(Model.class, modelId);
                final String keyModel2 = AlexaStateModel.getAttributeKey(Model.class, modelId2);
                final String keyModelSingleton = AlexaStateModel.getAttributeKey(Model.class, null);
                final String thingName = invocationOnMock.getArgumentAt(0, GetThingShadowRequest.class).getThingName();
                // this indicates method was called to get the application shadow
                if (thingName.equals(dummyHandler.getAppScopedThingName())) {
                    final String jsonWithModel1 = "\"" + keyModel1 + "\":" + givenModel(modelId).toJSON(AlexaScope.APPLICATION);
                    final String jsonWithModel2 = "\"" + keyModel2 + "\":" + givenModel(modelId2).toJSON(AlexaScope.APPLICATION);
                    final String jsonWithModelSingleton = "\"" + keyModelSingleton + "\":" + givenModel(null).toJSON(AlexaScope.APPLICATION);
                    final String jsonShadow = "{\"state\":{\"reported\":{" + jsonWithModel1 + ", " + jsonWithModel2 + ", " + jsonWithModelSingleton + "}}}";
                    final ByteBuffer payload = Charset.forName("UTF-8").encode(jsonShadow);
                    return new GetThingShadowResult().withPayload(payload);
                }
                // otherwise it must have been a call to read from user-shadow
                else if (thingName.equals(dummyHandler.getUserScopedThingName())) {
                    final String jsonWithModel1 = "\"" + keyModel1 + "\":" + givenModel(modelId).toJSON(AlexaScope.USER);
                    final String jsonWithModel2 = "\"" + keyModel2 + "\":" + givenModel(modelId2).toJSON(AlexaScope.USER);
                    final String jsonWithModelSingleton = "\"" + keyModelSingleton + "\":" + givenModel(null).toJSON(AlexaScope.USER);
                    final String jsonShadow = "{\"state\":{\"reported\":{" + jsonWithModel1 + ", " + jsonWithModel2 + ", " + jsonWithModelSingleton + "}}}";
                    final ByteBuffer payload = Charset.forName("UTF-8").encode(jsonShadow);
                    return new GetThingShadowResult().withPayload(payload);
                }
            }
            if (invocationOnMock.getMethod().getName().equals("updateThingShadow")) return new UpdateThingShadowResult();
            return null;
        });
        // construct handler with mocked AWS clients
        return new AWSIotStateHandler(session, iotClient, iotData);
    }

    @Test
    public void getAwsClients() throws Exception {
        // first check if client is constructed if no one is given
        final AWSIotStateHandler handler2 = new AWSIotStateHandler(session);
        assertNotNull(handler2.getAwsClient());
        assertNotNull(handler2.getAwsDataClient());

        // check if the client given is the client returned
        final AWSIot iotClient = new AWSIotClient();
        final AWSIotData iotData = new AWSIotDataClient();
        final AWSIotStateHandler handler3 = new AWSIotStateHandler(session, iotClient, iotData);
        assertNotNull(handler3.getAwsClient());
        assertNotNull(handler3.getAwsDataClient());
        assertEquals(iotClient, handler3.getAwsClient());
        assertEquals(iotData, handler3.getAwsDataClient());
    }

    @Test
    public void getThingName() throws Exception {
        assertNotNull(handler.getThingName(AlexaScope.APPLICATION));
        assertNotNull(handler.getThingName(AlexaScope.USER));
        assertEquals(handler.getAppScopedThingName(), handler.getThingName(AlexaScope.APPLICATION));
        assertEquals(handler.getUserScopedThingName(), handler.getThingName(AlexaScope.USER));
    }

    @Test
    public void createThingIfNotExisting() throws Exception {
        //
    }

    @Test
    public void doesThingExist() throws Exception {
        // due to listing of things is mocked, thing never exists
        assertFalse(handler.doesThingExist(AlexaScope.APPLICATION));
        assertFalse(handler.doesThingExist(AlexaScope.USER));
    }
}