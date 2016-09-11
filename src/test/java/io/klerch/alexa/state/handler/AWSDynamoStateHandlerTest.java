/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import io.klerch.alexa.state.model.dummies.Model;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

public class AWSDynamoStateHandlerTest extends AlexaStateHandlerTest<AWSDynamoStateHandler> {

    @Override
    public AWSDynamoStateHandler getHandler() {
        final AmazonDynamoDBClient awsClient = mock(AmazonDynamoDBClient.class);
        handler = new AWSDynamoStateHandler(session, awsClient);

        final String tableName = handler.getTableName();

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

        // mock get items for model with id
        Mockito.when(awsClient.getItem(tableName, handler.getUserScopedKeyAttributes(Model.class, modelId)))
                .thenReturn(resultUserId);
        Mockito.when(awsClient.getItem(tableName, handler.getAppScopedKeyAttributes(Model.class, modelId)))
                .thenReturn(resultAppId);

        // mock get items for absent model (with empty response)
        Mockito.when(awsClient.getItem(tableName, handler.getUserScopedKeyAttributes(Model.class, absentModelId)))
                .thenReturn(new GetItemResult());
        Mockito.when(awsClient.getItem(tableName, handler.getAppScopedKeyAttributes(Model.class, absentModelId)))
                .thenReturn(new GetItemResult());

        // on create table call always return table in ACTIVE state
        final TableDescription tableDescription = new TableDescription().withTableName(tableName).withTableStatus(TableStatus.ACTIVE);
        final CreateTableResult createResult = new CreateTableResult().withTableDescription(tableDescription);
        Mockito.when(awsClient.createTable(any(CreateTableRequest.class))).thenReturn(createResult);

        // mock describe table request to always return a table whose name is
        final DescribeTableResult describeResult = new DescribeTableResult().withTable(tableDescription);
        Mockito.when(awsClient.describeTable(any(DescribeTableRequest.class))).thenReturn(describeResult);

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