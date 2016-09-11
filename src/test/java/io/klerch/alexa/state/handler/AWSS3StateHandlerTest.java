/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 *
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;

public class AWSS3StateHandlerTest extends AlexaStateHandlerTest<AWSS3StateHandler> {
    private final String bucketName = "bucketName";

    @Override
    public AWSS3StateHandler getHandler() {
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
                    if (Arrays.stream(invocationOnMock.getArguments()).filter(p -> p.toString().contains("__application")).findAny().isPresent()) {
                        // dummy file for application which will be returned by mocked S3-getObject
                        final S3Object appFile = new S3Object();
                        appFile.setObjectContent(new ByteArrayInputStream("{\"id\":null,\"sampleApplication\":true}".getBytes(StandardCharsets.UTF_8)));
                        return appFile;
                    }
                    // otherwise it must have been a call to getObject() to read from user-file
                    else {
                        // dummy file for user which will be returned by mocked S3-getObject
                        final S3Object userFile = new S3Object();
                        userFile.setObjectContent(new ByteArrayInputStream("{\"id\":null,\"sampleUser\":\"sampleUser\"}".getBytes(StandardCharsets.UTF_8)));
                        return userFile;
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