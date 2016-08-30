package io.klerch.alexa.state.handler;

import com.amazon.speech.speechlet.Application;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.User;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import io.klerch.alexa.state.model.Model;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class AWSDynamoStateHandlerTest extends AlexaStateHandlerTest<AWSDynamoStateHandler> {
    private final String tableName = "tableName";

    @Override
    public AWSDynamoStateHandler getHandler() {
        final AmazonDynamoDBClient awsClient = mock(AmazonDynamoDBClient.class);
        handler = new AWSDynamoStateHandler(session, awsClient, tableName);

        // prepare static read return from DynamoDB without given model-Id
        final String jsonApp = "{\"id\":null,\"sampleApplication\":true}";
        final String jsonUser = "{\"id\":null,\"sampleUser\":\"sampleUser\"}";

        final Map<String, AttributeValue> mapUser = new HashMap<>();
        mapUser.put(handler.getAttributeKeyState(), new AttributeValue(jsonUser));

        final Map<String, AttributeValue> mapApp = new HashMap<>();
        mapApp.put(handler.getAttributeKeyState(), new AttributeValue(jsonApp));

        final GetItemResult resultUser = new GetItemResult().withItem(mapUser);
        final GetItemResult resultApp = new GetItemResult().withItem(mapApp);

        Mockito.when(awsClient.getItem(tableName, handler.getUserScopedKeyAttributes(Model.class)))
                .thenReturn(resultUser);
        Mockito.when(awsClient.getItem(tableName, handler.getAppScopedKeyAttributes(Model.class)))
                .thenReturn(resultApp);

        // prepare static read return from DynamoDB with given model-Id

        final String jsonAppId = "{\"id\":\"" + modelId + "\",\"sampleApplication\":true}";
        final String jsonUserId = "{\"id\":\"" + modelId + "\",\"sampleUser\":\"sampleUser\"}";

        final Map<String, AttributeValue> mapUserId = new HashMap<>();
        mapUserId.put(handler.getAttributeKeyState(), new AttributeValue(jsonUserId));

        final Map<String, AttributeValue> mapAppId = new HashMap<>();
        mapAppId.put(handler.getAttributeKeyState(), new AttributeValue(jsonAppId));

        final GetItemResult resultUserId = new GetItemResult().withItem(mapUserId);
        final GetItemResult resultAppId = new GetItemResult().withItem(mapAppId);

        Mockito.when(awsClient.getItem(tableName, handler.getUserScopedKeyAttributes(Model.class, modelId)))
                .thenReturn(resultUserId);
        Mockito.when(awsClient.getItem(tableName, handler.getAppScopedKeyAttributes(Model.class, modelId)))
                .thenReturn(resultAppId);
        return handler;
    }

    @Test
    public void getAwsClient() throws Exception {
        final AWSDynamoStateHandler handler = new AWSDynamoStateHandler(session);
        assertNotNull(handler.getAwsClient());

        final AmazonDynamoDBClient awsClient = new AmazonDynamoDBClient();
        final AWSDynamoStateHandler handler2 = new AWSDynamoStateHandler(session, awsClient);
        assertEquals(awsClient, handler2.getAwsClient());
    }

    @Test
    public void getTableName() throws Exception {
        final AWSDynamoStateHandler handler = new AWSDynamoStateHandler(session);
        assertNotNull(handler.getTableName());

        final AWSDynamoStateHandler handler2 = new AWSDynamoStateHandler(session, "tableName");
        assertEquals("tableName", handler2.getTableName());
    }

    @Test
    public void getAwsClientAndTableName() throws Exception {
        final AWSDynamoStateHandler handler = new AWSDynamoStateHandler(session);
        assertNotNull(handler.getTableName());
        assertNotNull(handler.getAwsClient());

        final AmazonDynamoDBClient awsClient = new AmazonDynamoDBClient();
        final AWSDynamoStateHandler handler2 = new AWSDynamoStateHandler(session, awsClient, "tableName");
        assertEquals(awsClient, handler2.getAwsClient());
        assertEquals("tableName", handler2.getTableName());
    }
}