/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 * <p>
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package me.lerch.alexa.state.handler;

import com.amazon.speech.speechlet.Session;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.*;
import com.amazonaws.services.iotdata.AWSIotDataClient;
import com.amazonaws.services.iotdata.model.DeleteThingShadowRequest;
import com.amazonaws.services.iotdata.model.GetThingShadowRequest;
import com.amazonaws.services.iotdata.model.GetThingShadowResult;
import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.lerch.alexa.state.model.AlexaScope;
import me.lerch.alexa.state.model.AlexaStateModel;
import me.lerch.alexa.state.utils.AlexaStateErrorException;
import me.lerch.alexa.state.utils.EncryptUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

public class AwsIOTStateHandler extends AlexaSessionStateHandler {

    private final AWSIotClient awsClient;
    private final AWSIotDataClient awsDataClient;
    private final String thingAttributeName = "name";
    private final String thingAttributeUser = "amzn-user-id";
    private final String thingAttributeApp = "amzn-app-id";
    private boolean thingExistsApproved = false;

    public AwsIOTStateHandler(final Session session) {
        this(session, new AWSIotClient(), new AWSIotDataClient());
    }

    public AwsIOTStateHandler(final Session session, final AWSIotClient awsClient, final AWSIotDataClient awsDataClient) {
        super(session);
        this.awsClient = awsClient;
        this.awsDataClient = awsDataClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeModel(final AlexaStateModel model) throws AlexaStateErrorException {
        // write to session
        super.writeModel(model);

        boolean hasAppScopedFields = model.getSaveStateFields(AlexaScope.APPLICATION).stream().findAny().isPresent();
        boolean hasUserScopedFields = model.getSaveStateFields(AlexaScope.USER).stream().findAny().isPresent();

        if (hasUserScopedFields) {
            publishState(getUserScopedThingName(), model, AlexaScope.USER);
        }
        if (hasAppScopedFields) {
            publishState(getAppScopedThingName(), model, AlexaScope.APPLICATION);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <TModel extends AlexaStateModel> Optional<TModel> readModel(final Class<TModel> modelClass) throws AlexaStateErrorException {
        return this.readModel(modelClass, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeModel(AlexaStateModel model) throws AlexaStateErrorException {
        super.removeModel(model);
        // get all fields which are user-scoped
        final boolean hasUserScopedFields = !model.getSaveStateFields(AlexaScope.USER).isEmpty();
        // get all fields which are app-scoped
        final boolean hasAppScopedFields = !model.getSaveStateFields(AlexaScope.APPLICATION).isEmpty();
        final String nodeName = getAttributeKey(model);

        if (hasUserScopedFields) {
            removeModelFromShadow(model, AlexaScope.USER);
        }
        if (hasAppScopedFields) {
            removeModelFromShadow(model, AlexaScope.APPLICATION);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <TModel extends AlexaStateModel> Optional<TModel> readModel(final Class<TModel> modelClass, final String id) throws AlexaStateErrorException {
        // if there is nothing for this model in the session ...
        // create new model with given id. for now we assume a model exists for this id. we find out by
        // reading file from the bucket in the following lines. only if this is true model will be written back to session
        final TModel model = super.readModel(modelClass, id).orElse(createModel(modelClass, id));
        // get all fields which are user-scoped
        final boolean hasUserScopedFields = !model.getSaveStateFields(AlexaScope.USER).isEmpty();
        // get all fields which are app-scoped
        final boolean hasAppScopedFields = !model.getSaveStateFields(AlexaScope.APPLICATION).isEmpty();
        // we need to remember if there will be something from thing shadow to be written to the model
        // in order to write those values back to the session at the end of this method
        Boolean modelChanged = false;
        // and if there are user-scoped fields ...
        if (hasUserScopedFields && fromThingShadowToModel(model, AlexaScope.USER)) {
            modelChanged = true;
        }
        // and if there are app-scoped fields ...
        if (hasAppScopedFields && fromThingShadowToModel(model, AlexaScope.APPLICATION)) {
            modelChanged = true;
        }
        // so if model changed from within something out of the shadow we want this to be in the speechlet as well
        // this gives you access to user- and app-scoped attributes throughout a session without reading from S3 over and over again
        if (modelChanged) {
            super.writeModel(model);
            return Optional.of(model);
        }
        else {
            // get all fields which are session-scoped
            final boolean hasSessionScopedFields = !model.getSaveStateFields(AlexaScope.SESSION).isEmpty();
            // if there was nothing received from IOT and there is nothing to return from session
            // then its not worth return the model. better indicate this model does not exist
            return hasSessionScopedFields ? Optional.of(model) : Optional.empty();
        }
    }

    private void removeModelFromShadow(final AlexaStateModel model, final AlexaScope scope) throws AlexaStateErrorException {
        final String nodeName = getAttributeKey(model);
        final String thingName = AlexaScope.USER.includes(scope) ? getUserScopedThingName() : getAppScopedThingName();
        final String thingState = getState(thingName, scope);
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode root = mapper.readTree(thingState);
            if (!root.isMissingNode()) {
                final JsonNode desired = root.path("state").path("desired");
                if (!desired.isMissingNode() && desired instanceof ObjectNode) {
                    ((ObjectNode) desired).remove(nodeName);
                }
            }
            final String json = mapper.writeValueAsString(root);
            publishState(thingName, json);
        } catch (IOException e) {
            throw AlexaStateErrorException.create("Could not extract model state from thing shadow state of " + thingName).withCause(e).withModel(model).build();
        }
    }

    private boolean fromThingShadowToModel(final AlexaStateModel model, final AlexaScope scope) throws AlexaStateErrorException {
        // read from item with scoped model
        final String thingName = AlexaScope.APPLICATION.includes(scope) ? getAppScopedThingName() : getUserScopedThingName();
        final String thingState = getState(thingName, scope);
        final String nodeName = getAttributeKey(model);
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode node = mapper.readTree(thingState).path("state").path("reported").path(nodeName);
            if (!node.isMissingNode()) {
                final String json = mapper.writeValueAsString(node);
                return model.fromJSON(json, scope);
            }
        } catch (IOException e) {
            throw AlexaStateErrorException.create("Could not extract model state from thing shadow state of " + thingName).withCause(e).withModel(model).build();
        }
        return false;
    }

    private String getUserScopedThingName() throws AlexaStateErrorException {
        // user-ids in Alexa are too long for thing names in AWS IOT.
        // use the SHA1-hash of the user-id
        final String userHash;
        try {
            userHash = EncryptUtils.encryptSha1(session.getUser().getUserId());
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw AlexaStateErrorException.create("Could not encrypt user-id for generating the IOT thing-name").withHandler(this).withCause(e).build();
        }
        return getAppScopedThingName() + "-" + userHash;
    }

    private String getAppScopedThingName() {
        // thing names do not allow dots in it
        return session.getApplication().getApplicationId().replace(".", "-");
    }

    private String getState(final String thingName, final AlexaScope scope) throws AlexaStateErrorException {
        createThingOfNotExisting(thingName, scope);

        final GetThingShadowRequest awsRequest = new GetThingShadowRequest().withThingName(thingName);
        try {
            final GetThingShadowResult response = awsDataClient.getThingShadow(awsRequest);
            final ByteBuffer buffer = response.getPayload();

            try {
                return (buffer != null && buffer.hasArray()) ? new String(buffer.array(), "UTF-8") : "{}";
            } catch (UnsupportedEncodingException e) {
                throw AlexaStateErrorException.create("Could not handle received contents of thing-shaodw " + thingName).withCause(e).withHandler(this).build();
            }
        }
        // if a thing does not have a shadow this is a usual exception
        catch (com.amazonaws.services.iotdata.model.ResourceNotFoundException e) {
            // we are fine with a thing having no shadow what just means there's nothing to read out for the model
            // return an empty JSON to indicate nothing in the thing shadow
            return "{}";
        }
    }

    private void publishState(final String thingName, final AlexaStateModel model, final AlexaScope scope) throws AlexaStateErrorException {
        createThingOfNotExisting(thingName, scope);
        final String payload = "{\"state\":{\"desired\":{\"" + getAttributeKey(model) + "\":" + model.toJSON(scope) + "}}}";
        publishState(thingName, payload);
    }

    private void publishState(final String thingName, final String json) throws AlexaStateErrorException {
        final ByteBuffer buffer;
        try {
            buffer = ByteBuffer.wrap(json.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw AlexaStateErrorException.create(e.getMessage()).withCause(e).withHandler(this).build();
        }
        final UpdateThingShadowRequest iotRequest = new UpdateThingShadowRequest().withThingName(thingName).withPayload(buffer);
        awsDataClient.updateThingShadow(iotRequest);
    }

    private boolean createThingOfNotExisting(final String thingName, final AlexaScope scope) {
        if (!doesThingExist(thingName)) {
            createThing(thingName, scope);
            return true;
        }
        return false;
    }

    private void createThing(final String thingName, final AlexaScope scope) {
        // only create thing if not already existing
        final AttributePayload attrPayload = new AttributePayload();
        // add thing name as attribute as well. this is how the handler queries for the thing from now on
        attrPayload.addAttributesEntry(thingAttributeName, thingName);
        // if scope is user an attribute saves the plain user id as it is encrypted in the thing name
        if (AlexaScope.USER.includes(scope)) {
            attrPayload.addAttributesEntry(thingAttributeUser, session.getUser().getUserId());
        }
        // another thing attributes holds the Alexa application-id
        attrPayload.addAttributesEntry(thingAttributeApp, session.getApplication().getApplicationId());
        // now create the thing
        final CreateThingRequest request = new CreateThingRequest().withThingName(thingName).withAttributePayload(attrPayload);
        awsClient.createThing(request);

    }

    private boolean doesThingExist(final String thingName) {
        // if already checked existence than return immediately
        if (thingExistsApproved) return true;
        // query by an attribute having the name of the thing
        // unfortunately you can only query for things with their attributes, not directly with their names
        final ListThingsRequest request = new ListThingsRequest().withAttributeName(thingAttributeName).withAttributeValue(thingName).withMaxResults(1);
        thingExistsApproved = !awsClient.listThings(request).getThings().isEmpty();
        return thingExistsApproved;
    }
}
