/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 * <p>
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

import com.amazon.speech.speechlet.Session;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.S3Object;
import io.klerch.alexa.state.model.AlexaStateObject;
import io.klerch.alexa.state.utils.AlexaStateException;
import io.klerch.alexa.state.model.AlexaScope;
import io.klerch.alexa.state.model.AlexaStateModel;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * As this handler works in the user and application scope it persists all models to an S3 bucket.
 * This handler reads and writes state for AlexaStateModels and considers all its fields annotated with AlexaSaveState-tags.
 * This handler derives from the AlexaSessionStateHandler thus it reads and writes state out of S3 files also to your Alexa
 * session. For each individual scope (which is described by the Alexa User Id there will be a directory in your bucket which
 * then contains files - one for each instance of a saved model. Be aware that S3 does not support
 * bulk uploads thus writeModels and writeValues upload files one by one without batch processing.
 */
public class AWSS3StateHandler extends AlexaSessionStateHandler {
    private final Logger log = Logger.getLogger(AWSS3StateHandler.class);

    private final AmazonS3 awsClient;
    private final String bucketName;
    private static final String folderNameApp = "__application";
    private static final String fileExtension = "json";

    /**
     * Takes the Alexa session. An AWS client for accessing the S3 bucket will make use
     * of all the defaults in your runtime environment in regards to AWS region and credentials. The
     * credentials of this client need permission for getting and putting objects to this bucket.
     *
     * @param session    The Alexa session of your current skill invocation.
     * @param bucketName The bucket where all saved states will go into.
     */
    public AWSS3StateHandler(final Session session, final String bucketName) {
        this(session, AmazonS3ClientBuilder.defaultClient(), bucketName);
    }

    /**
     * Takes the Alexa session and an AWS client set up for the AWS region the given bucket is in. The
     * credentials of this client need permission for getting and putting objects to this bucket.
     *
     * @param session    The Alexa session of your current skill invocation.
     * @param awsClient  An AWS client capable of getting and putting objects to the given bucket.
     * @param bucketName The bucket where all saved states will go into.
     */
    public AWSS3StateHandler(final Session session, final AmazonS3 awsClient, final String bucketName) {
        super(session);
        this.awsClient = awsClient;
        this.bucketName = bucketName;
    }

    /**
     * Returns the AWS connection client used to write to and read from files in S3 bucket.
     *
     * @return AWS connection client to S3
     */
    public AmazonS3 getAwsClient() {
        return this.awsClient;
    }

    /**
     * Returns the name of the S3 bucket which is used by this handler to store JSON files with
     * model states.
     *
     * @return Name of the S3 bucket
     */
    public String getBucketName() {
        return this.bucketName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AWSS3StateHandler withUserId(final String userId) {
        return (AWSS3StateHandler)super.withUserId(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeModels(final Collection<? extends AlexaStateModel> models) throws AlexaStateException {
        // write to session
        super.writeModels(models);

        for (final AlexaStateModel model : models) {
            if (model.hasUserScopedField()) {
                final String filePath = getUserScopedFilePath(model.getClass(), model.getId());
                // add json as new content of file
                final String fileContents = model.toJSON(AlexaScope.USER);
                // write all user-scoped attributes to file
                awsClient.putObject(bucketName, filePath, fileContents);
            }
            if (model.hasApplicationScopedField()) {
                // add primary keys as attributes
                final String filePath = getAppScopedFilePath(model.getClass(), model.getId());
                // add json as new content of file
                final String fileContents = model.toJSON(AlexaScope.APPLICATION);
                // write all app-scoped attributes to file
                awsClient.putObject(bucketName, filePath, fileContents);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeValues(final Collection<? extends AlexaStateObject> stateObjects) throws AlexaStateException {
        // write to session
        super.writeValues(stateObjects);
        stateObjects.stream()
                // select only USER or APPLICATION scoped state objects
                .filter(stateObject -> stateObject.getScope().isIn(AlexaScope.USER, AlexaScope.APPLICATION))
                .forEach(stateObject -> {
                    final String id = stateObject.getId();
                    final String value = String.valueOf(stateObject.getValue());
                    final AlexaScope scope = stateObject.getScope();
                    final String filePath = AlexaScope.USER.includes(scope) ?
                            getUserScopedFilePath(id) : getAppScopedFilePath(id);
                    // write all app-scoped attributes to file
                    awsClient.putObject(bucketName, filePath, value);
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeValues(final Collection<String> ids) throws AlexaStateException {
        super.removeValues(ids);
        final List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>();
        ids.forEach(id -> keys.addAll(Arrays.asList(
                new DeleteObjectsRequest.KeyVersion(getUserScopedFilePath(id)),
                new DeleteObjectsRequest.KeyVersion(getAppScopedFilePath(id)))));
        final DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName)
                .withKeys(keys);
        awsClient.deleteObjects(deleteObjectsRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(final String id, final AlexaScope scope) throws AlexaStateException {
        if (AlexaScope.SESSION.includes(scope)) {
            return super.exists(id, scope);
        } else {
            final String filePath = AlexaScope.USER.includes(scope) ?
                    getUserScopedFilePath(id) : getAppScopedFilePath(id);
            return awsClient.doesObjectExist(bucketName, filePath);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <TModel extends AlexaStateModel> Map<String, TModel> readModels(final Class<TModel> modelClass, final Collection<String> ids) throws AlexaStateException {
        // select all models that have a representation in the session
        final Map<String, TModel> existingModels = super.readModels(modelClass, ids);

        final Map<String, TModel> allModels = new HashMap<>(existingModels);
        // create new models were there was no representation in the session with given id. for now we assume a model exists for this id. we find out by
        // querying dynamodb in the following lines. only if there's actually something for it in dynamo we'll keep it.
        ids.stream().filter(id -> !existingModels.containsKey(id)).forEach(id -> {
            allModels.putIfAbsent(id, createModel(modelClass, id));
        });

        // this is where we store models that were updated with values found in DynamoDb
        final Map<String, TModel> updatedModels = new HashMap<>();

        for (final TModel model : allModels.values()) {
            // we need to remember if there will be something from S3 to be written to the model
            // in order to write those values back to the session at the end of this method
            Boolean modelChanged = false;
            // and if there are user-scoped fields ...
            if (model.hasUserScopedField() && fromS3FileContentsToModel(model, model.getId(), AlexaScope.USER)) {
                modelChanged = true;
            }
            // and if there are app-scoped fields ...
            if (model.hasApplicationScopedField() && fromS3FileContentsToModel(model, model.getId(), AlexaScope.APPLICATION)) {
                modelChanged = true;
            }
            // so if model changed from within something out of S3 we want this to be in the speechlet as well
            // this gives you access to user- and app-scoped attributes throughout a session without reading from S3 over and over again
            if (modelChanged) {
                updatedModels.put(model.getId(), model);
            }
        }
        // write back updated values to session
        super.writeModels(updatedModels.values());

        // finally we join models that were found in the session + models with updates from Dynamo
        existingModels.forEach((id, model) -> {
            if (!updatedModels.containsKey(id)) {
                updatedModels.put(id, model);
            }
        });
        return updatedModels;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, AlexaStateObject> readValues(final Map<String, AlexaScope> idsInScope) throws AlexaStateException {
        final Map<String, AlexaStateObject> stateObjectMap = new HashMap<>();
        // first read all the session-scoped items and put to result map
        stateObjectMap.putAll(super.readValues(idsInScope));

        idsInScope.forEach((id, scope) -> {
            if (scope.isIn(AlexaScope.USER, AlexaScope.APPLICATION)) {
                final String filePath = AlexaScope.USER.includes(scope) ?
                        getUserScopedFilePath(id) : getAppScopedFilePath(id);
                try {
                    // get S3 file
                    getS3FileContentsAsString(filePath)
                            // wrap its contents in state object
                            .map(fileContents -> new AlexaStateObject(id, fileContents, scope))
                            // add to result map
                            .ifPresent(stateObject -> stateObjectMap.putIfAbsent(id, stateObject));
                } catch (final AlexaStateException | AmazonS3Exception e) {
                    // we are fine with an exception likely caused by file (state) not exists
                    log.warn("Could not read from '" + filePath + "'.", e);
                }
            }
        });
        return stateObjectMap;
    }

    private boolean fromS3FileContentsToModel(final AlexaStateModel alexaStateModel, final String id, final AlexaScope scope) throws AlexaStateException {
        // read from item with scoped model
        final String filePath = AlexaScope.APPLICATION.includes(scope) ? getAppScopedFilePath(alexaStateModel.getClass(), id) : getUserScopedFilePath(alexaStateModel.getClass(), id);
        // extract values from json and assign it to model
        return awsClient.doesObjectExist(bucketName, filePath) && alexaStateModel.fromJSON(getS3FileContentsAsString(filePath).orElse("{}"), scope);
    }

    private Optional<String> getS3FileContentsAsString(final String filePath) throws AlexaStateException {
        final S3Object file = awsClient.getObject(bucketName, filePath);
        if (file == null) {
            return Optional.empty();
        }
        final BufferedReader reader = new BufferedReader(new InputStreamReader(file.getObjectContent()));
        final StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            final String error = String.format("Could not read from S3-file '%1$s' from Bucket '%2$s'.", filePath, bucketName);
            log.error(error, e);
            throw AlexaStateException.create(error).withCause(e).withHandler(this).build();
        }
        final String fileContents = sb.toString();
        return fileContents.isEmpty() ? Optional.empty() : Optional.of(fileContents);
    }

    private <TModel extends AlexaStateModel> String getUserScopedFilePath(final Class<TModel> modelClass, final String id) {
        return getUserId() + "/" + TModel.getAttributeKey(modelClass, id) + "." + fileExtension;
    }

    private String getUserScopedFilePath(final String id) {
        return getUserId() + "/" + id + "." + fileExtension;
    }

    private <TModel extends AlexaStateModel> String getAppScopedFilePath(final Class<TModel> modelClass, final String id) {
        return folderNameApp + "/" + TModel.getAttributeKey(modelClass, id) + "." + fileExtension;
    }

    private String getAppScopedFilePath(final String id) {
        return folderNameApp + "/" + id + "." + fileExtension;
    }
}
