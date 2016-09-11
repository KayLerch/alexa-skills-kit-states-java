/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 * <p>
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

import com.amazon.speech.speechlet.Session;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import io.klerch.alexa.state.utils.AlexaStateException;
import io.klerch.alexa.state.model.AlexaStateModel;
import io.klerch.alexa.state.model.AlexaScope;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * As this handler works in the user and application scope it persists all models to a AWS DynamoDB table.
 * This handler reads and writes state for AlexaStateModels and considers all its fields annotated with AlexaSaveState-tags.
 * This handler derives from the AlexaSessionStateHandler thus it reads and writes state out of DynamoDB also to your Alexa
 * session.
 */
public class AWSDynamoStateHandler extends AlexaSessionStateHandler {
    private final Logger log = Logger.getLogger(AWSDynamoStateHandler.class);

    private final AmazonDynamoDB awsClient;
    private final String tableName;
    private final long readCapacityUnits;
    private final long writeCapacityUnits;
    private static final String tablePrefix = "alexa-";
    private static final String attributeValueApp = "__application";
    private static final String pkModel = "model-class";
    private static final String pkUser = "amzn-user-id";
    private static final String attributeKeyState = "state";
    private Boolean tableExistenceApproved = false;

    /**
     * The most convenient constructor just takes the Alexa session. An AWS client for accessing DynamoDB
     * will make use of all defaults in regards to credentials and region. The credentials used in this client need permission for reading, writing and
     * removing items from a DynamoDB table and also the right to create a table. On the very first read or
     * write operation of this handler it creates a table named like your Alexa App Id.
     * The table created consist of a hash-key and a sort-key.
     * If you don't want this handler to auto-create a table provide the name of an existing table in DynamoDB
     * in another constructor.
     * @param session The Alexa session of your current skill invocation.
     */
    public AWSDynamoStateHandler(final Session session) {
        this(session, new AmazonDynamoDBClient(), null, 10L, 5L);
    }

    /**
     * Takes the Alexa session and a AWS client which is set up for
     * the correct AWS region. The credentials used in this client need permission for reading, writing and
     * removing items from a DynamoDB table and also the right to create a table. On the very first read or
     * write operation of this handler it creates a table named like your Alexa App Id.
     * The table created consist of a hash-key and a sort-key.
     * If you don't want this handler to auto-create a table provide the name of an existing table in DynamoDB
     * in another constructor.
     * @param session The Alexa session of your current skill invocation.
     * @param awsClient An AWS client capable of creating DynamoDB table plus reading, writing and removing items.
     */
    public AWSDynamoStateHandler(final Session session, final AmazonDynamoDB awsClient) {
        this(session, awsClient, null, 10L, 5L);
    }

    /**
     * Takes the Alexa session and a table. An AWS client for accessing DynamoDB
     * will make use of all defaults in regards to credentials and region. The credentials used in this client need permission for reading, writing and
     * removing items from the given DynamoDB table. The table needs a string hash-key with name model-class and a string sort-key
     * of name amzn-user-id. The option of providing an existing table to this handler prevents it from checking its existence
     * which might end up with a better performance. You also don't need to provide permission of creating a DynamoDB table to
     * the credentials of the given AWS client.
     * @param session The Alexa session of your current skill invocation.
     * @param tableName An existing table accessible by the client and with string hash-key named model-class and a string sort-key named amzn-user-id.
     */
    public AWSDynamoStateHandler(final Session session, final String tableName) {
        this(session, new AmazonDynamoDBClient(), tableName, 10L, 5L);
    }

    /**
     * Takes the Alexa session and a AWS client which is set up for
     * the correct AWS region. The credentials used in this client need permission for reading, writing and
     * removing items from the given DynamoDB table. The table needs a string hash-key with name model-class and a string sort-key
     * of name amzn-user-id. The option of providing an existing table to this handler prevents it from checking its existence
     * which might end up with a better performance. You also don't need to provide permission of creating a DynamoDB table to
     * the credentials of the given AWS client.
     * @param session The Alexa session of your current skill invocation.
     * @param awsClient An AWS client capable of reading, writing and removing items of the given DynamoDB table.
     * @param tableName An existing table accessible by the client and with string hash-key named model-class and a string sort-key named amzn-user-id.
     */
    public AWSDynamoStateHandler(final Session session, final AmazonDynamoDB awsClient, final String tableName) {
        this(session, awsClient, tableName, 10L, 5L);
    }

    /**
     * Takes the Alexa session, an AWS client which is set up for
     * the correct AWS region. The credentials used in this client need permission for reading, writing and
     * removing items from a DynamoDB table and also the right to create a table. On the very first read or
     * write operation of this handler it creates a table named like your Alexa App Id.
     * The table created consist of a hash-key and a sort-key.
     * If you don't want this handler to auto-create a table provide the name of an existing table in DynamoDB
     * in another constructor.
     * @param session The Alexa session of your current skill invocation.
     * @param awsClient An AWS client capable of creating DynamoDB table plus reading, writing and removing items.
     * @param readCapacityUnits Read capacity for the table which is applied only on creation of table (what happens at the very first read or write operation with this handler)
     * @param writeCapacityUnits Write capacity for the table which is applied only on creation of table (what happens at the very first read or write operation with this handler)
     */
    public AWSDynamoStateHandler(final Session session, final AmazonDynamoDB awsClient, final long readCapacityUnits, final long writeCapacityUnits) {
        this(session, awsClient, null, readCapacityUnits, writeCapacityUnits);
    }

    private AWSDynamoStateHandler(final Session session, final AmazonDynamoDB awsClient, final String tableName, final long readCapacityUnits, final long writeCapacityUnits) {
        super(session);
        this.awsClient = awsClient;
        // assume table exists if table name provided.
        this.tableExistenceApproved = tableName != null && !tableName.isEmpty();
        this.tableName = tableName != null ? tableName : tablePrefix + session.getApplication().getApplicationId();
        this.readCapacityUnits = readCapacityUnits;
        this.writeCapacityUnits = writeCapacityUnits;
    }

    /**
     * Returns the AWS connection client used to write to and read from items in DynamoDB table.
     * @return AWS connection client to DynamoDB
     */
    public AmazonDynamoDB getAwsClient() {
        return this.awsClient;
    }

    /**
     * Returns the name of the DynamoDB table which is used by this handler to store items with
     * model states.
     * @return name of the DynamoDB table
     */
    public String getTableName() {
        return this.tableName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeModel(final AlexaStateModel model) throws AlexaStateException {
        // write to session
        super.writeModel(model);
        boolean hasAppScopedFields = model.getSaveStateFields(AlexaScope.APPLICATION).stream().findAny().isPresent();
        boolean hasUserScopedFields = model.getSaveStateFields(AlexaScope.USER).stream().findAny().isPresent();

        // if there is something which needs to be written to dynamo db ensure table exists
        if (hasAppScopedFields || hasUserScopedFields) {
            try {
                ensureTableExists();
            } catch (InterruptedException e) {
                final String error = String.format("Could not create DynamoDb-Table '%1$s' before writing state for '%2$s'", tableName, model);
                log.debug(error);
                throw AlexaStateException.create(error).withCause(e).withHandler(this).build();
            }
        }

        if (hasUserScopedFields) {
            // add primary keys as attributes
            final Map<String, AttributeValue> attributes = getUserScopedKeyAttributes(model.getClass(), model.getId());
            // add json as attribute
            final String jsonState = model.toJSON(AlexaScope.USER);
            attributes.put(attributeKeyState, new AttributeValue(jsonState));
            // write all user-scoped attributes to table
            awsClient.putItem(tableName, attributes);
        }
        if (hasAppScopedFields) {
            // add primary keys as attributes
            final Map<String, AttributeValue> attributes = getAppScopedKeyAttributes(model.getClass(), model.getId());
            // add json as attribute
            final String jsonState = model.toJSON(AlexaScope.APPLICATION);
            attributes.put(attributeKeyState, new AttributeValue(jsonState));
            // write all app-scoped attributes to table
            awsClient.putItem(tableName, attributes);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeModel(final AlexaStateModel model) throws AlexaStateException {
        super.removeModel(model);
        // removeState user-scoped item
        awsClient.deleteItem(tableName, getUserScopedKeyAttributes(model.getClass(), model.getId()));
        // removeState app-scoped item
        awsClient.deleteItem(tableName, getAppScopedKeyAttributes(model.getClass(), model.getId()));
        log.debug(String.format("Removed state from DynamoDB for '%1$s'.", model));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <TModel extends AlexaStateModel> Optional<TModel> readModel(final Class<TModel> modelClass) throws AlexaStateException {
        return this.readModel(modelClass, null);
    }

    public String getAttributeKeyState() {
        return this.attributeKeyState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <TModel extends AlexaStateModel> Optional<TModel> readModel(final Class<TModel> modelClass, final String id) throws AlexaStateException {
        // if there is nothing for this model in the session ...
        final Optional<TModel> modelSession = super.readModel(modelClass, id);
        // create new model with given id. for now we assume a model exists for this id. we find out by
        // querying dynamodb in the following lines. only if this is true model will be written back to session
        final TModel model = modelSession.orElse(createModel(modelClass, id));
        // must ensure table is existing in case there are user- or app-scoped field awaiting values from db
        if (model.hasUserScopedField() || model.hasApplicationScopedField()) {
            try {
                ensureTableExists();
            } catch (InterruptedException e) {
                final String error = String.format("Could not create DynamoDb-Table '%1$s' before writing state for '%2$s'", tableName, model);
                log.error(error);
                throw AlexaStateException.create(error).withCause(e).withHandler(this).build();
            }
        }
        // we need to remember if there will be something from dynamodb to be written to the model
        // in order to write those values back to the session at the end of this method
        Boolean modelChanged = false;
        // and if there are user-scoped fields ...
        if (model.hasUserScopedField() && fromDbStatetoModel(model, id, AlexaScope.USER)) {
            log.debug(String.format("Values applied from DynamoDB to user-scoped fields of model '%1$s'.", model));
            modelChanged = true;
        }
        // and if there are app-scoped fields ...
        if (model.hasApplicationScopedField() && fromDbStatetoModel(model, id, AlexaScope.APPLICATION)) {
            log.debug(String.format("Values applied from DynamoDB to application-scoped fields of model '%1$s'.", model));
            modelChanged = true;
        }
        // so if model changed from within something out of dynamodb we want this to be in the speechlet as well
        // this gives you access to user- and app-scoped attributes throughout a session without reading from db over and over again
        if (modelChanged) {
            super.writeModel(model);
            return Optional.of(model);
        } else {
            log.debug(String.format("No state for application- or user-scoped fields of model '%1$s' found in DynamoDB.", model));
            // if there was nothing received from dynamo and there is nothing to return from session
            // then its not worth return the model. better indicate this model does not exist
            return modelSession.isPresent() ? Optional.of(model) : Optional.empty();
        }
    }

    private boolean fromDbStatetoModel(final AlexaStateModel alexaStateModel, final String id, final AlexaScope scope) throws AlexaStateException {
        // read from item with scoped model
        final Map<String, AttributeValue> key = AlexaScope.APPLICATION.includes(scope) ? getAppScopedKeyAttributes(alexaStateModel.getClass(), id) : getUserScopedKeyAttributes(alexaStateModel.getClass(), id);
        final GetItemResult awsResult = awsClient.getItem(tableName, key);
        final Map<String, AttributeValue> attributes = awsResult.getItem();
        // if no item found then return false
        if (attributes == null || attributes.isEmpty()) return false;
        // read state as json-string
        final String json = attributes.getOrDefault(attributeKeyState, new AttributeValue("{}")).getS();
        // extract values from json and assign it to model
        return alexaStateModel.fromJSON(json, scope);
    }

    private void ensureTableExists() throws InterruptedException {
        // given custom table is always assumed as existing so you can have this option to bypass existance checks
        // for reason of least privileges on used AWS credentials or better performance
        if (!tableExistenceApproved || !tableExists()) {
            final ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<>();
            // describe keys
            attributeDefinitions.add(new AttributeDefinition()
                    .withAttributeName(pkUser)
                    .withAttributeType("S"));
            attributeDefinitions.add(new AttributeDefinition()
                    .withAttributeName(pkModel)
                    .withAttributeType("S"));
            // define keys
            final ArrayList<KeySchemaElement> keySchema = new ArrayList<>();
            keySchema.add(new KeySchemaElement()
                    .withAttributeName(pkUser)
                    .withKeyType(KeyType.HASH));
            keySchema.add(new KeySchemaElement()
                    .withAttributeName(pkModel)
                    .withKeyType(KeyType.RANGE));
            // prepare table creation request
            final CreateTableRequest awsRequest = new CreateTableRequest()
                    .withTableName(tableName)
                    .withKeySchema(keySchema)
                    .withAttributeDefinitions(attributeDefinitions)
                    .withProvisionedThroughput(new ProvisionedThroughput()
                            .withReadCapacityUnits(readCapacityUnits)
                            .withWriteCapacityUnits(writeCapacityUnits));
            // create on not existing table
            if (TableUtils.createTableIfNotExists(awsClient, awsRequest)) {
                log.info(String.format("Table '%1$s' is created in DynamoDB. Now standing by for up to ten minutes for this table to be in active state.", tableName));
                // wait for table to be in ACTIVE state in order to proceed with read or write
                // this could take up to possible ten minutes so be sure to run this code once before publishing your skill ;)
                TableUtils.waitUntilActive(awsClient, awsRequest.getTableName());
            }
        }
    }

    <TModel extends AlexaStateModel> Map<String, AttributeValue> getUserScopedKeyAttributes(final Class<TModel> modelClass) {
        return getUserScopedKeyAttributes(modelClass, null);
    }

    <TModel extends AlexaStateModel> Map<String, AttributeValue> getUserScopedKeyAttributes(final Class<TModel> modelClass, final String id) {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put(pkModel, new AttributeValue(TModel.getAttributeKey(modelClass, id)));
        attributes.put(pkUser, new AttributeValue(session.getUser().getUserId()));
        return attributes;
    }

    <TModel extends AlexaStateModel> Map<String, AttributeValue> getAppScopedKeyAttributes(final Class<TModel> modelClass) {
        return getAppScopedKeyAttributes(modelClass, null);
    }

    <TModel extends AlexaStateModel> Map<String, AttributeValue> getAppScopedKeyAttributes(final Class<TModel> modelClass, final String id) {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put(pkModel, new AttributeValue(TModel.getAttributeKey(modelClass, id)));
        attributes.put(pkUser, new AttributeValue(attributeValueApp));
        return attributes;
    }

    private boolean tableExists() {
        // if custom table is set assume it is present
        // this is how you can bypass table existence checks and table creation as you maybe do not want
        // to authorize your AWS client with broader permission sets
        if (tableExistenceApproved) return true;
        try {
            // due to absence of existence check in AWS SDK we check the table descriptor for its state
            final TableDescription table = awsClient.describeTable(new DescribeTableRequest(tableName)).getTable();
            // save result to a local variable to not let this handler check for table existence again
            tableExistenceApproved = TableStatus.ACTIVE.toString().equals(table.getTableStatus());
            return tableExistenceApproved;
        } catch (ResourceNotFoundException e) {
            final String error = String.format("Could not find table '%1$s'", tableName);
            log.warn(error);
            return false;
        }
    }
}
