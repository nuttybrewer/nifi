/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.processors.aws.credentials.provider.factory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.PropertiesFileCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleWithWebIdentitySessionCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processors.aws.s3.FetchS3Object;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests of the validation and credentials provider capabilities of CredentialsProviderFactory.
 */
public class TestCredentialsProviderFactory {

    @Test
    public void testImpliedDefaultCredentials() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.assertValid();

        Map<PropertyDescriptor, String> properties = runner.getProcessContext().getProperties();
        final CredentialsProviderFactory factory = new CredentialsProviderFactory();
        final AWSCredentialsProvider credentialsProvider = factory.getCredentialsProvider(properties);
        assertNotNull(credentialsProvider);
        assertEquals(DefaultAWSCredentialsProviderChain.class,
                credentialsProvider.getClass(), "credentials provider should be equal");
    }

    @Test
    public void testExplicitDefaultCredentials() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.USE_DEFAULT_CREDENTIALS, "true");
        runner.assertValid();

        Map<PropertyDescriptor, String> properties = runner.getProcessContext().getProperties();
        final CredentialsProviderFactory factory = new CredentialsProviderFactory();
        final AWSCredentialsProvider credentialsProvider = factory.getCredentialsProvider(properties);
        assertNotNull(credentialsProvider);
        assertEquals(DefaultAWSCredentialsProviderChain.class,
                credentialsProvider.getClass(), "credentials provider should be equal");
    }

    @Test
    public void testExplicitDefaultCredentialsExclusive() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.USE_DEFAULT_CREDENTIALS, "true");
        runner.setProperty(CredentialPropertyDescriptors.ACCESS_KEY, "BogusAccessKey");
        runner.assertNotValid();
    }

    @Test
    public void testAccessKeyPairCredentials() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.USE_DEFAULT_CREDENTIALS, "false");
        runner.setProperty(CredentialPropertyDescriptors.ACCESS_KEY, "BogusAccessKey");
        runner.setProperty(CredentialPropertyDescriptors.SECRET_KEY, "BogusSecretKey");
        runner.assertValid();

        Map<PropertyDescriptor, String> properties = runner.getProcessContext().getProperties();
        final CredentialsProviderFactory factory = new CredentialsProviderFactory();
        final AWSCredentialsProvider credentialsProvider = factory.getCredentialsProvider(properties);
        assertNotNull(credentialsProvider);
        assertEquals(StaticCredentialsProvider.class,
                credentialsProvider.getClass(), "credentials provider should be equal");
    }

    @Test
    public void testAccessKeyPairIncomplete() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.ACCESS_KEY, "BogusAccessKey");
        runner.assertNotValid();
    }

    @Test
    public void testAccessKeyPairIncompleteS3() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(FetchS3Object.class);
        runner.setProperty(CredentialPropertyDescriptors.ACCESS_KEY, "BogusAccessKey");
        runner.assertNotValid();
    }

    @Test
    public void testFileCredentials() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.CREDENTIALS_FILE, "src/test/resources/mock-aws-credentials.properties");
        runner.assertValid();

        Map<PropertyDescriptor, String> properties = runner.getProcessContext().getProperties();
        final CredentialsProviderFactory factory = new CredentialsProviderFactory();
        final AWSCredentialsProvider credentialsProvider = factory.getCredentialsProvider(properties);
        assertNotNull(credentialsProvider);
        assertEquals(PropertiesFileCredentialsProvider.class,
                credentialsProvider.getClass(), "credentials provider should be equal");
    }

    @Test
    public void testAssumeRoleCredentials() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.CREDENTIALS_FILE, "src/test/resources/mock-aws-credentials.properties");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_ARN, "BogusArn");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_NAME, "BogusSession");
        runner.assertValid();

        Map<PropertyDescriptor, String> properties = runner.getProcessContext().getProperties();
        final CredentialsProviderFactory factory = new CredentialsProviderFactory();
        final AWSCredentialsProvider credentialsProvider = factory.getCredentialsProvider(properties);
        assertNotNull(credentialsProvider);
        assertEquals(STSAssumeRoleSessionCredentialsProvider.class,
                credentialsProvider.getClass(), "credentials provider should be equal");
    }
    
    @Test
    public void testAssumeRoleWithWebIdentity() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_ARN, "BogusArn");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_NAME, "BogusSession");
        // The credentials don't matter since we're not connecting, just use the credentials file as a mock
        // Normally this would contain a base64 encoded string, but it doesn't get validated until we get a response
        // from the Amazon STS web service.
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_WITH_WEB_IDENTITY_TOKEN_FILENAME, "src/test/resources/mock-aws-credentials.properties");
        runner.assertValid();

        Map<PropertyDescriptor, String> properties = runner.getProcessContext().getProperties();
        
        final CredentialsProviderFactory factory = new CredentialsProviderFactory();
        final AWSCredentialsProvider credentialsProvider = factory.getCredentialsProvider(properties);
        Assert.assertNotNull(credentialsProvider);
        assertEquals("credentials provider should be equal", STSAssumeRoleWithWebIdentitySessionCredentialsProvider.class,
                credentialsProvider.getClass());
    }

    @Test
    public void testAssumeRoleWithWebIdentityMissingArn() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_NAME, "BogusSession");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_WITH_WEB_IDENTITY_TOKEN_FILENAME, "src/test/resources/mock-aws-credentials.properties");
        runner.assertNotValid();
    }
    
    @Test
    public void testAssumeRoleWithWebIdentityMissingRoleName() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_ARN, "BogusArn");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_WITH_WEB_IDENTITY_TOKEN_FILENAME, "src/test/resources/mock-aws-credentials.properties");
        runner.assertNotValid();
    }
    
    @Test
    public void testAssumeRoleWithWebIdentityMissingWebIdentityTokenFilename() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_ARN, "BogusArn");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_NAME, "BogusSession");
        runner.assertValid();
        
        // Expected behavior since the criteria for assume with credentials is just role arn and role name
        Map<PropertyDescriptor, String> properties = runner.getProcessContext().getProperties();
        final CredentialsProviderFactory factory = new CredentialsProviderFactory();
        final AWSCredentialsProvider credentialsProvider = factory.getCredentialsProvider(properties);
        Assert.assertNotNull(credentialsProvider);
        assertEquals("credentials provider should be equal", STSAssumeRoleSessionCredentialsProvider.class,
                credentialsProvider.getClass());
    }
    
    @Test
    public void testAssumeRoleCredentialsMissingARN() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_WITH_WEB_IDENTITY_TOKEN_FILENAME, "src/test/resources/mock-aws-credentials.properties");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_NAME, "BogusSession");
        runner.assertNotValid();
    }

    
    @Test
    public void testAssumeRoleCredentialsInvalidSessionTime() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.CREDENTIALS_FILE, "src/test/resources/mock-aws-credentials.properties");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_ARN, "BogusArn");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_NAME, "BogusSession");
        runner.setProperty(CredentialPropertyDescriptors.MAX_SESSION_TIME, "10");
        runner.assertNotValid();
    }

    @Test
    public void testAssumeRoleExternalIdMissingArnAndName() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.CREDENTIALS_FILE, "src/test/resources/mock-aws-credentials.properties");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_EXTERNAL_ID, "BogusExternalId");
        runner.assertNotValid();
    }

    @Test
    public void testAssumeRoleSTSEndpointMissingArnAndName() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.CREDENTIALS_FILE, "src/test/resources/mock-aws-credentials.properties");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_STS_ENDPOINT, "BogusSTSEndpoint");
        runner.assertNotValid();
    }

    @Test
    public void testAnonymousCredentials() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.USE_ANONYMOUS_CREDENTIALS, "true");
        runner.assertValid();

        Map<PropertyDescriptor, String> properties = runner.getProcessContext().getProperties();
        final CredentialsProviderFactory factory = new CredentialsProviderFactory();
        final AWSCredentialsProvider credentialsProvider = factory.getCredentialsProvider(properties);
        assertNotNull(credentialsProvider);
        final AWSCredentials creds = credentialsProvider.getCredentials();
        assertEquals(AnonymousAWSCredentials.class, creds.getClass(), "credentials should be equal");
    }

    @Test
    public void testAnonymousAndDefaultCredentials() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.USE_DEFAULT_CREDENTIALS, "true");
        runner.setProperty(CredentialPropertyDescriptors.USE_ANONYMOUS_CREDENTIALS, "true");
        runner.assertNotValid();
    }

    @Test
    public void testNamedProfileCredentials() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.USE_DEFAULT_CREDENTIALS, "false");
        runner.setProperty(CredentialPropertyDescriptors.PROFILE_NAME, "BogusProfile");
        runner.assertValid();

        Map<PropertyDescriptor, String> properties = runner.getProcessContext().getProperties();
        final CredentialsProviderFactory factory = new CredentialsProviderFactory();
        final AWSCredentialsProvider credentialsProvider = factory.getCredentialsProvider(properties);
        assertNotNull(credentialsProvider);
        assertEquals(ProfileCredentialsProvider.class,
                credentialsProvider.getClass(), "credentials provider should be equal");
    }

    @Test
    public void testAssumeRoleCredentialsWithProxy() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.CREDENTIALS_FILE, "src/test/resources/mock-aws-credentials.properties");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_ARN, "BogusArn");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_NAME, "BogusSession");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_PROXY_HOST, "proxy.company.com");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_PROXY_PORT, "8080");
        runner.assertValid();

        Map<PropertyDescriptor, String> properties = runner.getProcessContext().getProperties();
        final CredentialsProviderFactory factory = new CredentialsProviderFactory();
        final AWSCredentialsProvider credentialsProvider = factory.getCredentialsProvider(properties);
        assertNotNull(credentialsProvider);
        assertEquals(STSAssumeRoleSessionCredentialsProvider.class,
                credentialsProvider.getClass(), "credentials provider should be equal");
    }

    @Test
    public void testAssumeRoleMissingProxyHost() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.CREDENTIALS_FILE, "src/test/resources/mock-aws-credentials.properties");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_PROXY_PORT, "8080");
        runner.assertNotValid();
    }

    @Test
    public void testAssumeRoleMissingProxyPort() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.CREDENTIALS_FILE, "src/test/resources/mock-aws-credentials.properties");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_PROXY_HOST, "proxy.company.com");
        runner.assertNotValid();
    }

    @Test
    public void testAssumeRoleInvalidProxyPort() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.CREDENTIALS_FILE, "src/test/resources/mock-aws-credentials.properties");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_PROXY_HOST, "proxy.company.com");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_PROXY_PORT, "notIntPort");
        runner.assertNotValid();
    }
    
    @Test
    public void testAssumeRoleWithWebIdentityTokenFilenameWithProxy() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_ARN, "BogusArn");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_NAME, "BogusSession");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_WITH_WEB_IDENTITY_TOKEN_FILENAME, "src/test/resources/mock-aws-credentials.properties");
 ;
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_PROXY_HOST, "proxy.company.com");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_PROXY_PORT, "8080");
        runner.assertValid();

        Map<PropertyDescriptor, String> properties = runner.getProcessContext().getProperties();
        final CredentialsProviderFactory factory = new CredentialsProviderFactory();
        final AWSCredentialsProvider credentialsProvider = factory.getCredentialsProvider(properties);
        Assert.assertNotNull(credentialsProvider);
        assertEquals("credentials provider should be equal", STSAssumeRoleWithWebIdentitySessionCredentialsProvider.class,
                credentialsProvider.getClass());
    }

    @Test
    public void testAssumeRoleWithWebIdentityTokenFilenameMissingProxyHost() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_ARN, "BogusArn");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_NAME, "BogusSession");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_WITH_WEB_IDENTITY_TOKEN_FILENAME, "src/test/resources/mock-aws-credentials.properties");
 ;
        runner.setProperty(CredentialPropertyDescriptors.CREDENTIALS_FILE, "src/test/resources/mock-aws-credentials.properties");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_PROXY_PORT, "8080");
        runner.assertNotValid();
    }

    @Test
    public void testAssumeRoleWithWebIdentityTokenFilenameMissingProxyPort() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_ARN, "BogusArn");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_NAME, "BogusSession");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_WITH_WEB_IDENTITY_TOKEN_FILENAME, "src/test/resources/mock-aws-credentials.properties");
 ;
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_PROXY_HOST, "proxy.company.com");
        runner.assertNotValid();
    }

    @Test
    public void testAssumeRoleWithWebIdentityTokenFilenameInvalidProxyPort() throws Throwable {
        final TestRunner runner = TestRunners.newTestRunner(MockAWSProcessor.class);
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_ARN, "BogusArn");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_NAME, "BogusSession");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_WITH_WEB_IDENTITY_TOKEN_FILENAME, "src/test/resources/mock-aws-credentials.properties");
 ;
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_PROXY_HOST, "proxy.company.com");
        runner.setProperty(CredentialPropertyDescriptors.ASSUME_ROLE_PROXY_PORT, "notIntPort");
        runner.assertNotValid();
    }
    
}
