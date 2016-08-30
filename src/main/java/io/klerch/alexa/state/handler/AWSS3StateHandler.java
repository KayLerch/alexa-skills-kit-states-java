/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 * <p>
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

import com.amazon.speech.speechlet.Session;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import io.klerch.alexa.state.utils.AlexaStateException;
import io.klerch.alexa.state.model.AlexaScope;
import io.klerch.alexa.state.model.AlexaStateModel;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

/**
 * As this handler works in the user and application scope it persists all models to an S3 bucket.
 * This handler reads and writes state for AlexaStateModels and considers all its fields annotated with AlexaSaveState-tags.
 * This handler derives from the AlexaSessionStateHandler thus it reads and writes state out of S3 files also to your Alexa
 * session. For each individual scope (which is described by the Alexa User Id there will be a directory in your bucket which
 * then contains files - one for each instance of a saved model.
 */
public class AWSS3StateHandler extends AlexaSessionStateHandler {
    private final Logger log = Logger.getLogger(AWSS3StateHandler.class);

    private final AmazonS3 awsClient;
    private final String bucketName;
    private final String folderNameApp = "__application";
    private final String fileExtension = "json";

    /**
     * Takes the Alexa session. An AWS client for accessing the S3 bucket will make use
     * of all the defaults in your runtime environment in regards to AWS region and credentials. The
     * credentials of this client need permission for getting and putting objects to this bucket.
     * @param session The Alexa session of your current skill invocation.
     * @param bucketName The bucket where all saved states will go into.
     */
    public AWSS3StateHandler(final Session session, final String bucketName) {
        this(session, new AmazonS3Client(), bucketName);
    }

    /**
     * Takes the Alexa session and an AWS client set up for the AWS region the given bucket is in. The
     * credentials of this client need permission for getting and putting objects to this bucket.
     * @param session The Alexa session of your current skill invocation.
     * @param awsClient An AWS client capable of getting and putting objects to the given bucket.
     * @param bucketName The bucket where all saved states will go into.
     */
    public AWSS3StateHandler(final Session session, final AmazonS3 awsClient, final String bucketName) {
        super(session);
        this.awsClient = awsClient;
        this.bucketName = bucketName;
    }

    /**
     * Returns the AWS connection client used to write to and read from files in S3 bucket.
     * @return AWS connection client to S3
     */
    public AmazonS3 getAwsClient() {
        return this.awsClient;
    }

    /**
     * Returns the name of the S3 bucket which is used by this handler to store JSON files with
     * model states.
     * @return Name of the S3 bucket
     */
    public String getBucketName() {
        return this.bucketName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeModel(final AlexaStateModel model) throws AlexaStateException {
        // write to session
        super.writeModel(model);

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

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeModel(AlexaStateModel model) throws AlexaStateException {
        super.removeModel(model);
        // removeState user-scoped file
        if (model.hasUserScopedField())
            awsClient.deleteObject(bucketName, getUserScopedFilePath(model.getClass(), model.getId()));
        // removeState app-scoped file
        if (model.hasApplicationScopedField())
            awsClient.deleteObject(bucketName, getAppScopedFilePath(model.getClass(), model.getId()));
        log.debug(String.format("Removed state from S3 for '%1$s'.", model));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <TModel extends AlexaStateModel> Optional<TModel> readModel(final Class<TModel> modelClass) throws AlexaStateException {
        return this.readModel(modelClass, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <TModel extends AlexaStateModel> Optional<TModel> readModel(final Class<TModel> modelClass, final String id) throws AlexaStateException {
        // if there is nothing for this model in the session ...
        final Optional<TModel> modelSession = super.readModel(modelClass, id);
        // create new model with given id. for now we assume a model exists for this id. we find out by
        // reading file from the bucket in the following lines. only if this is true model will be written back to session
        final TModel model = modelSession.orElse(createModel(modelClass, id));
        // we need to remember if there will be something from S3 to be written to the model
        // in order to write those values back to the session at the end of this method
        Boolean modelChanged = false;
        // and if there are user-scoped fields ...
        if (model.hasUserScopedField() && fromS3FileContentsToModel(model, id, AlexaScope.USER)) {
            modelChanged = true;
        }
        // and if there are app-scoped fields ...
        if (model.hasApplicationScopedField() && fromS3FileContentsToModel(model, id, AlexaScope.APPLICATION)) {
            modelChanged = true;
        }
        // so if model changed from within something out of S3 we want this to be in the speechlet as well
        // this gives you access to user- and app-scoped attributes throughout a session without reading from S3 over and over again
        if (modelChanged) {
            super.writeModel(model);
            return Optional.of(model);
        }
        else {
            // if there was nothing received from S3 and there is nothing to return from session
            // then its not worth return the model. better indicate this model does not exist
            return modelSession.isPresent() ? Optional.of(model) : Optional.empty();
        }
    }

    private boolean fromS3FileContentsToModel(final AlexaStateModel alexaStateModel, final String id, final AlexaScope scope) throws AlexaStateException {
        // read from item with scoped model
        final String filePath = AlexaScope.APPLICATION.includes(scope) ? getAppScopedFilePath(alexaStateModel.getClass(), id) : getUserScopedFilePath(alexaStateModel.getClass(), id);
        // extract values from json and assign it to model
        return awsClient.doesObjectExist(bucketName, filePath) && alexaStateModel.fromJSON(getS3FileContentsAsString(filePath), scope);
    }

    private String getS3FileContentsAsString(final String filePath) throws AlexaStateException {
        final S3Object file = awsClient.getObject(bucketName, filePath);
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
        return fileContents.isEmpty() ? "{}" : fileContents;
    }

    private <TModel extends AlexaStateModel> String getUserScopedFilePath(final Class<TModel> modelClass) {
        return getUserScopedFilePath(modelClass, null);
    }

    private <TModel extends AlexaStateModel> String getUserScopedFilePath(final Class<TModel> modelClass, final String id) {
        return session.getUser().getUserId() + "/" + TModel.getAttributeKey(modelClass, id) + "." + fileExtension;
    }

    private <TModel extends AlexaStateModel> String getAppScopedFilePath(final Class<TModel> modelClass) {
        return getAppScopedFilePath(modelClass, null);
    }

    private <TModel extends AlexaStateModel> String getAppScopedFilePath(final Class<TModel> modelClass, final String id) {
        return folderNameApp + "/" + TModel.getAttributeKey(modelClass, id) + "." + fileExtension;
    }
}
