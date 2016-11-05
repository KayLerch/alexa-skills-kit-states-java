/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 * <p>
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import io.klerch.alexa.state.model.AlexaScope;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.*;

public class AWSS3StateHandlerTest extends AlexaStateHandlerTest<AWSS3StateHandler> {
    static String bucketName = "alexa-test-" + UUID.randomUUID().toString();

    @Override
    public AWSS3StateHandler givenHandler() throws Exception {
        final AmazonS3Client s3Client = Mockito.mock(AmazonS3Client.class, new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (invocationOnMock.getMethod().getName().equals("doesObjectExist")) {
                    // true in case of any model requested beside the one assumed as absent
                    return !(Arrays.stream(invocationOnMock.getArguments()).filter(p -> p.toString().contains(absentModelId)).findAny().isPresent());
                }
                if (invocationOnMock.getMethod().getName().equals("putObject")) return new PutObjectResult();
                if (invocationOnMock.getMethod().getName().equals("deleteObject")) return null;
                if (invocationOnMock.getMethod().getName().equals("getObject")) {
                    // look for a parameter which contains the application-foldername
                    // this indicates getObject was called to get the application file
                    final String filePath = invocationOnMock.getArgumentAt(1, String.class);
                    if (filePath.contains("__application")) {
                        // get payload of respective test instance if their id is found in the filepath
                        final String payload =
                                filePath.contains(modelId) ? givenModel(modelId).toJSON(AlexaScope.APPLICATION) :
                                        filePath.contains(modelId2) ? givenModel(modelId2).toJSON(AlexaScope.APPLICATION) :
                                                !filePath.contains(absentModelId) ? givenModel(null).toJSON(AlexaScope.APPLICATION) : null;
                        // dummy file for application which will be returned by mocked S3-getObject
                        if (payload != null) {
                            final S3Object appFile = new S3Object();
                            appFile.setObjectContent(new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)));
                            return appFile;
                        } else {
                            return null;
                        }
                    }
                    // otherwise it must have been a call to getObject() to read from user-file
                    else {
                        // get payload of respective test instance if their id is found in the filepath
                        final String payload =
                                filePath.contains(modelId) ? givenModel(modelId).toJSON(AlexaScope.USER) :
                                        filePath.contains(modelId2) ? givenModel(modelId2).toJSON(AlexaScope.USER) :
                                                !filePath.contains(absentModelId) ? givenModel(null).toJSON(AlexaScope.USER) : null;
                        // dummy file for application which will be returned by mocked S3-getObject
                        if (payload != null) {
                            final S3Object appFile = new S3Object();
                            appFile.setObjectContent(new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)));
                            return appFile;
                        }
                        return null;
                    }
                }
                return null;
            }
        });
        // construct handler with mocked s3-client
        return new AWSS3StateHandler(session, s3Client, bucketName);
    }

    @Test
    public void getAwsClient() throws Exception {
        // first check if client is constructed if no one is given
        final AWSS3StateHandler handler2 = new AWSS3StateHandler(session, bucketName);
        assertNotNull(handler2.getAwsClient());

        // check if the client given is the client returned
        final AmazonS3 s3Client = new AmazonS3Client();
        final AWSS3StateHandler handler3 = new AWSS3StateHandler(session, s3Client, bucketName);
        assertNotNull(handler3.getAwsClient());
        assertEquals(s3Client, handler3.getAwsClient());
    }

    @Test
    public void getBucketName() throws Exception {
        assertEquals(bucketName, handler.getBucketName());
    }

    @Test
    public void getAwsClientAndBucketName() throws Exception {
        final AWSS3StateHandler handler = new AWSS3StateHandler(session, bucketName);
        assertNotNull(handler.getBucketName());
        assertNotNull(handler.getAwsClient());

        final AmazonS3 awsClient = new AmazonS3Client();
        final AWSS3StateHandler handler2 = new AWSS3StateHandler(session, awsClient, bucketName);
        assertEquals(awsClient, handler2.getAwsClient());
        assertEquals(bucketName, handler2.getBucketName());
    }
}