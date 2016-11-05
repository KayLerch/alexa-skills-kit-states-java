/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 * <p>
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import io.klerch.alexa.state.IntegrationTest;
import org.junit.AfterClass;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class AWSDynamoStateHandlerIT extends AWSDynamoStateHandlerTest {
    @Override
    public AWSDynamoStateHandler givenHandler() throws Exception {
        // credentials need to be set in local environment
        // see http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
        final AWSDynamoStateHandler dynamoStateHandler = new AWSDynamoStateHandler(session);
        tableName = dynamoStateHandler.getTableName();
        return dynamoStateHandler;
    }

    @AfterClass
    public static void deleteTable() {
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest(tableName);
        // credentials need to be set in local environment
        // see http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
        TableUtils.deleteTableIfExists(new AmazonDynamoDBClient(), deleteTableRequest);
    }
}