/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 * <p>
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import io.klerch.alexa.state.model.AlexaScope;
import io.klerch.alexa.state.utils.AlexaStateException;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class AWSDynamoStateHandlerTest extends AlexaStateHandlerTest<AWSDynamoStateHandler> {
    static String tableName = "tableName";

    @Override
    public AWSDynamoStateHandler givenHandler() throws Exception {
        final AmazonDynamoDBClient awsClient = mock(AmazonDynamoDBClient.class, (Answer) invocation -> {
            if (invocation.getMethod().getName().equals("createTable")) {
                // on create table call always return table in ACTIVE state
                final TableDescription tableDescription = new TableDescription().withTableName(tableName).withTableStatus(TableStatus.ACTIVE);
                return new CreateTableResult().withTableDescription(tableDescription);
            }
            if (invocation.getMethod().getName().equals("describeTable")) {
                // mock describe table request to always return a table whose name is tableName
                final TableDescription tableDescription = new TableDescription().withTableName(tableName).withTableStatus(TableStatus.ACTIVE);
                return new DescribeTableResult().withTable(tableDescription);
            }
            if (invocation.getMethod().getName().equals("batchGetItem")) {
                final List<Map<String, AttributeValue>> resultItems = new ArrayList<>();
                final BatchGetItemRequest getItemRequest = invocation.getArgumentAt(0, BatchGetItemRequest.class);
                // go through all request items
                getItemRequest.getRequestItems().forEach((tableName, keysAndAttributes) -> {
                    // go through keys of a request
                    keysAndAttributes.getKeys().forEach(attributes -> {
                        // obtain scope the state of an object is requested
                        final AlexaScope scope = attributes.get(AWSDynamoStateHandler.pkUser).getS().equals(AWSDynamoStateHandler.attributeValueApp) ? AlexaScope.APPLICATION : AlexaScope.USER;
                        final String modelRef = attributes.get(AWSDynamoStateHandler.pkModel).getS();
                        // prepare result item which takes over the primary keys of the request
                        final Map<String, AttributeValue> map = new HashMap<>();
                        map.put(AWSDynamoStateHandler.pkUser, attributes.get(AWSDynamoStateHandler.pkUser));
                        map.put(AWSDynamoStateHandler.pkModel, attributes.get(AWSDynamoStateHandler.pkModel));
                        // if modelRef is equal to test modelIds then a single state object is requested
                        // cause models would have been requested with attributeKey which contains the qualified class name
                        if (Arrays.asList(modelId, modelId2).contains(modelRef)) {
                            // always return the static value given by the parent
                            map.put(handler.getAttributeKeyState(), new AttributeValue(stateModelValue));
                            // add result item
                            resultItems.add(map);
                        } else {
                            // extract modelId
                            final String id = modelRef.contains(":") ? modelRef.substring(modelRef.lastIndexOf(":") + 1) : null;
                            // ensure modelId is not absent modelId
                            if (!absentModelId.equals(id) && !absentModelId.equals(modelRef)) {
                                // json payload of requested model
                                try {
                                    final String payload = givenModel(id).toJSON(scope);
                                    map.put(handler.getAttributeKeyState(), new AttributeValue(payload));
                                    // add result item
                                    resultItems.add(map);
                                } catch (AlexaStateException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                });
                final BatchGetItemResult batchResult = new BatchGetItemResult();
                batchResult.addResponsesEntry(tableName, resultItems);
                return batchResult;
            }
            return null;
        });
        // return handler with mocked Dynamo client
        return new AWSDynamoStateHandler(session, awsClient, tableName);
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

        final AWSDynamoStateHandler handler2 = new AWSDynamoStateHandler(session, tableName);
        assertEquals(tableName, handler2.getTableName());
    }

    @Test
    public void getAwsClientAndTableName() throws Exception {
        final AmazonDynamoDBClient awsClient = new AmazonDynamoDBClient();
        final AWSDynamoStateHandler handler = new AWSDynamoStateHandler(session, awsClient, tableName);
        assertEquals(awsClient, handler.getAwsClient());
        assertEquals(tableName, handler.getTableName());
    }

    @Test
    public void getAwsClientAndTableNameAndCapacity() throws Exception {
        final AmazonDynamoDBClient awsClient = new AmazonDynamoDBClient();
        final AWSDynamoStateHandler handler = new AWSDynamoStateHandler(session, awsClient, 100L, 200L);
        assertEquals(awsClient, handler.getAwsClient());
    }
}