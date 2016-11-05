/**
 * Made by Kay Lerch (https://twitter.com/KayLerch)
 * <p>
 * Attached license applies.
 * This library is licensed under GNU GENERAL PUBLIC LICENSE Version 3 as of 29 June 2007
 */
package io.klerch.alexa.state.handler;

import com.amazonaws.services.s3.AmazonS3Client;
import io.klerch.alexa.state.IntegrationTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class AWSS3StateHandlerIT extends AWSS3StateHandlerTest {
    @BeforeClass
    public static void createBucket() {
        // credentials need to be set in local environment
        // see http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
        final AmazonS3Client amazonS3Client = new AmazonS3Client();
        amazonS3Client.createBucket(bucketName);
    }

    @Override
    public AWSS3StateHandler givenHandler() throws Exception {
        // credentials need to be set in local environment
        // see http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
        return new AWSS3StateHandler(session, bucketName);
    }

    @AfterClass
    public static void deleteBucket() {
        // credentials need to be set in local environment
        // see http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
        final AmazonS3Client amazonS3Client = new AmazonS3Client();
        amazonS3Client.deleteBucket(bucketName);
    }
}