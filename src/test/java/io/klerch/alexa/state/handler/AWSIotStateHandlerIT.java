/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iotdata.AWSIotDataClient;
import io.klerch.alexa.state.IntegrationTest;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class AWSIotStateHandlerIT extends AlexaStateHandlerTest<AWSIotStateHandler> {
    @Override
    public AWSIotStateHandler givenHandler() {
        // credentials need to be set in local environment
        // see http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
        return new AWSIotStateHandler(session, new AWSIotClient(), new AWSIotDataClient());
    }
}