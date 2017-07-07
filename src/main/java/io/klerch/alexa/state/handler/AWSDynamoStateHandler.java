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
import io.klerch.alexa.state.model.AlexaStateObject;
import io.klerch.alexa.state.utils.AlexaStateException;
import io.klerch.alexa.state.model.AlexaStateModel;
import io.klerch.alexa.state.model.AlexaScope;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

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
    // context value for the primary index for each item saved in application scope
    static final String attributeValueApp = "__application";
    // column-name for primary index of the dynamo table to store the context key (mostly user-id)
    static final String pkUser = "amzn-user-id";
    // column-name for secondary index of the dynamo table to store the object identifier (aka id)
    static final String pkModel = "model-class";
    // column-name for table attribute used to store the state value (model JSON, single value)
    private static final String attributeKeyState = "state";
    // flag that indicates if existence of table is approved to avoid multiple checks in
    // dynamodb in single instance lifetime
    private Boolean tableExistenceApproved = false;

    /**
     * The most convenient constructor just takes the Alexa session. An AWS client for accessing DynamoDB
     * will make use of all defaults in regards to credentials and region. The credentials used in this client need permission for reading, writing and
     * removing items from a DynamoDB table and also the right to create a table. On the very first read or
     * write operation of this handler it creates a table named like your Alexa App Id.
     * The table created consist of a hash-key and a sort-key.
     * If you don't want this handler to auto-create a table provide the name of an existing table in DynamoDB
     * in another constructor.
     *
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
     *
     * @param session   The Alexa session of your current skill invocation.
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
     *
     * @param session   The Alexa session of your current skill invocation.
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
     *
     * @param session   The Alexa session of your current skill invocation.
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
     *
     * @param session            The Alexa session of your current skill invocation.
     * @param awsClient          An AWS client capable of creating DynamoDB table plus reading, writing and removing items.
     * @param readCapacityUnits  Read capacity for the table which is applied only on creation of table (what happens at the very first read or write operation with this handler)
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
     *
     * @return AWS connection client to DynamoDB
     */
    public AmazonDynamoDB getAwsClient() {
        return this.awsClient;
    }

    /**
     * Returns the name of the DynamoDB table which is used by this handler to store items with
     * model states.
     *
     * @return name of the DynamoDB table
     */
    String getTableName() {
        return this.tableName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeModels(final Collection<AlexaStateModel> models) throws AlexaStateException {
        // write to session
        super.writeModels(models);

        final List<WriteRequest> items = new ArrayList<>();
        // go for each model asked to be saved
        for (final AlexaStateModel model : models) {
            // convert model to a dynamo-item having in place all attributes
            getItems(model, true).forEach(item ->
                    // wrap each model in a write-request and collect all of them
                    items.add(new WriteRequest(new PutRequest(item)))
            );
        }
        // write batch of write-request to dynamo
        writeItemsToDb(items);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeValues(final Collection<AlexaStateObject> stateObjects) throws AlexaStateException {
        // write to session
        super.writeValues(stateObjects);

        final List<WriteRequest> items = new ArrayList<>();
        stateObjects.stream()
                // select only USER or APPLICATION scoped state objects
                .filter(stateObject -> stateObject.getScope().isIn(AlexaScope.USER, AlexaScope.APPLICATION))
                // go for each state object to be saved
                .forEach(stateObject -> {
                    final String id = stateObject.getId();
                    final Object value = stateObject.getValue();
                    final AlexaScope scope = stateObject.getScope();
                    // set primary keys which differ depending on the scope to save value for
                    final Map<String, AttributeValue> item = AlexaScope.USER.includes(scope) ?
                            getUserScopedKeyAttributes(id) : getAppScopedKeyAttributes(id);
                    // add an attribute which holds the actual value
                    item.put(attributeKeyState, new AttributeValue(String.valueOf(value)));
                    // wrap each value in a write-request and collect all of them
                    items.add(new WriteRequest(new PutRequest(item)));
                });
        // write batch of write-requests to dynamo
        writeItemsToDb(items);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeValues(final Collection<String> ids) throws AlexaStateException {
        super.removeValues(ids);
        final List<WriteRequest> items = new ArrayList<>();
        ids.forEach(id -> {
            // removeState user-scoped item
            items.add(new WriteRequest(new DeleteRequest(getUserScopedKeyAttributes(id))));
            // removeState app-scoped item
            items.add(new WriteRequest(new DeleteRequest(getAppScopedKeyAttributes(id))));
        });
        writeItemsToDb(items);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(final String id, final AlexaScope scope) throws AlexaStateException {
        if (AlexaScope.SESSION.includes(scope)) {
            return super.exists(id, scope);
        } else {
            return readValueFromDb(id, scope).isPresent();
        }
    }

    String getAttributeKeyState() {
        return attributeKeyState;
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
        // get read-request items (could be two - one for user-scoped item, one for app-scoped item)
        final List<Map<String, AttributeValue>> readRequests = getItems(model, false);
        if (!readRequests.isEmpty()) {
            // calculate attribute key for model
            final String attributeKey = TModel.getAttributeKey(modelClass, id);
            // go through resultset
            boolean updatedAtAll = false;
            for (final Map<String, AttributeValue> item : readItemsFromDb(readRequests)) {
                if (item.get(pkModel).getS().equals(attributeKey)) {
                    // only fields in requested scope should be updated in the model
                    final AlexaScope scope = item.get(pkUser).getS().equals(attributeValueApp) ? AlexaScope.APPLICATION : AlexaScope.USER;
                    final boolean updated = model.fromJSON(item.getOrDefault(attributeKeyState, new AttributeValue("{}")).getS(), scope);
                    // write back user and app-scoped fields to session
                    super.writeModel(model);
                    // remember that either user- or app-scoped fields have been updated
                    updatedAtAll = updatedAtAll || updated;
                }
            }
            if (updatedAtAll)
                return Optional.of(model);
        }
        log.debug(String.format("No state for application- or user-scoped fields of model '%1$s' found in DynamoDB.", model));
        // if there was nothing received from dynamo and there is nothing to return from session
        // then its not worth to return the model. better indicate this model does not exist
        return modelSession.isPresent() ? Optional.of(model) : Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, AlexaStateObject> readValues(final Map<String, AlexaScope> idsInScope) throws AlexaStateException {
        final Map<String, AlexaStateObject> stateObjectMap = new HashMap<>();
        // first read all the session-scoped items and put to result map
        stateObjectMap.putAll(super.readValues(idsInScope));
        // build a list of attribute-sets of individual to-be-read items
        final List<Map<String, AttributeValue>> attributesList = idsInScope.entrySet().stream()
                // only do this for user and application scoped ids
                .filter(entry -> entry.getValue().isIn(AlexaScope.USER, AlexaScope.APPLICATION))
                // get the attribute-set according to the given scope
                .map(entry -> AlexaScope.USER.includes(entry.getValue()) ?
                        getUserScopedKeyAttributes(entry.getKey()) : getAppScopedKeyAttributes(entry.getKey()))
                .collect(Collectors.toList());
        // go through result and transform result-item to state object
        readItemsFromDb(attributesList).forEach(item -> {
            final String id = item.get(pkModel).getS();
            stateObjectMap.putIfAbsent(id, new AlexaStateObject(id, item.get(attributeKeyState), idsInScope.get(id)));
        });
        return stateObjectMap;
    }

    private List<Map<String, AttributeValue>> getItems(final AlexaStateModel model, final boolean withState) throws AlexaStateException {
        final List<Map<String, AttributeValue>> items = new ArrayList<>();
        if (model.hasUserScopedField()) {
            // add primary keys as attributes
            final Map<String, AttributeValue> attributes = getUserScopedKeyAttributes(model.getClass(), model.getId());
            if (withState) {
                // add json as attribute
                final String jsonState = model.toJSON(AlexaScope.USER);
                attributes.put(attributeKeyState, new AttributeValue(jsonState));
            }
            // write all user-scoped attributes to table
            items.add(attributes);
        }
        if (model.hasApplicationScopedField()) {
            // add primary keys as attributes
            final Map<String, AttributeValue> attributes = getAppScopedKeyAttributes(model.getClass(), model.getId());
            if (withState) {
                // add json as attribute
                final String jsonState = model.toJSON(AlexaScope.APPLICATION);
                attributes.put(attributeKeyState, new AttributeValue(jsonState));
            }
            // write all app-scoped attributes to table
            items.add(attributes);
        }
        return items;
    }

    private Optional<String> readValueFromDb(final String id, final AlexaScope scope) throws AlexaStateException {
        // read from item with scoped model
        final Map<String, AttributeValue> key = AlexaScope.APPLICATION.includes(scope) ? getAppScopedKeyAttributes(id) : getUserScopedKeyAttributes(id);
        final List<Map<String, AttributeValue>> result = readItemsFromDb(Collections.singletonList(key));
        // read state as json-string
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0).getOrDefault(attributeKeyState, new AttributeValue("{}")).getS());
    }

    private List<Map<String, AttributeValue>> readItemsFromDb(final List<Map<String, AttributeValue>> keys) throws AlexaStateException {
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }
        ensureTableExists();
        final KeysAndAttributes keysAndAttributes = new KeysAndAttributes().withKeys(keys);
        final Map<String, KeysAndAttributes> requestItems = new HashMap<>();
        requestItems.put(tableName, keysAndAttributes);
        final BatchGetItemRequest getItemRequest = new BatchGetItemRequest().withRequestItems(requestItems);
        final BatchGetItemResult result = awsClient.batchGetItem(getItemRequest);
        return result.getResponses().getOrDefault(tableName, Collections.emptyList());
    }

    private void writeItemsToDb(final List<WriteRequest> items) throws AlexaStateException {
        if (!items.isEmpty()) {
            // if there is something which needs to be written to dynamo db ensure table exists
            ensureTableExists();
            final BatchWriteItemRequest writeItemRequest = new BatchWriteItemRequest();
            writeItemRequest.addRequestItemsEntry(tableName, items);
            awsClient.batchWriteItem(writeItemRequest);
        }
    }

    private void ensureTableExists() throws AlexaStateException {
        // given custom table is always assumed as existing so you can have this option to bypass existance checks
        // for reason of least privileges on used AWS credentials or better performance
        if (!tableExistenceApproved || !tableExists()) {
            final ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<>();
            // describe keys (both will be Strings)
            attributeDefinitions.add(new AttributeDefinition()
                    .withAttributeName(pkUser)
                    .withAttributeType("S"));
            attributeDefinitions.add(new AttributeDefinition()
                    .withAttributeName(pkModel)
                    .withAttributeType("S"));
            // define keys with name and type
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
                try {
                    TableUtils.waitUntilActive(awsClient, awsRequest.getTableName());
                } catch (final InterruptedException e) {
                    final String message = String.format("Could not create DynamoDb-Table '%1$s' before writing state", tableName);
                    log.error(message, e);
                    throw AlexaStateException.create(message).withCause(e).withHandler(this).build();
                }
            }
        }
    }

    private <TModel extends AlexaStateModel> Map<String, AttributeValue> getUserScopedKeyAttributes(final Class<TModel> modelClass, final String id) {
        return getUserScopedKeyAttributes(TModel.getAttributeKey(modelClass, id));
    }

    private Map<String, AttributeValue> getUserScopedKeyAttributes(final String id) {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put(pkModel, new AttributeValue(id));
        attributes.put(pkUser, new AttributeValue(session.getUser().getUserId()));
        return attributes;
    }

    private <TModel extends AlexaStateModel> Map<String, AttributeValue> getAppScopedKeyAttributes(final Class<TModel> modelClass, final String id) {
        return getAppScopedKeyAttributes(TModel.getAttributeKey(modelClass, id));
    }

    private Map<String, AttributeValue> getAppScopedKeyAttributes(final String id) {
        Map<String, AttributeValue> attributes = new HashMap<>();
        attributes.put(pkModel, new AttributeValue(id));
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
            final String message = String.format("Could not find table '%1$s'", tableName);
            log.warn(message, e);
            return false;
        }
    }
}
