/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mule.test.infrastructure.maven.MavenTestUtils.getMavenLocalRepository;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.module.tooling.api.data.DataProviderResult;
import org.mule.runtime.module.tooling.api.data.DataProviderService;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.module.tooling.api.ArtifactAgnosticServiceBuilder;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.runtime.module.tooling.api.data.DataResult;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class DataProviderServiceTestCase extends AbstractFakeMuleServerTestCase implements TestExtensionAware {

  private static final String EXTENSION_GROUP_ID = "org.mule.tooling";
  private static final String EXTENSION_ARTIFACT_ID = "tooling-support-test-extension";
  private static final String EXTENSION_VERSION = "1.0.0-SNAPSHOT";
  private static final String EXTENSION_CLASSIFIER = "mule-plugin";
  private static final String EXTENSION_TYPE = "jar";

  private static final String CONFIG_NAME = "dummyConfig";
  private static final String CLIENT_NAME = "client";
  private static final String PROVIDED_PARAMETER_NAME = "providedParameter";
  private static final String METADATA_KEY_PARAMETER = "metadataKey";

  private ToolingService toolingService;

  @ClassRule
  public static SystemProperty artifactsLocation =
      new SystemProperty("mule.test.maven.artifacts.dir", DataProviderService.class.getResource("/").getPath());

  @Rule
  public SystemProperty repositoryLocation =
      new SystemProperty("muleRuntimeConfig.maven.repositoryLocation", getMavenLocalRepository().getAbsolutePath());

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.toolingService = this.muleServer.toolingService();
    this.muleServer.start();
  }

  @Test
  public void testConnection() {
    ConnectivityTestingService connectivityTestingService =
        addDependency(toolingService.newConnectivityTestingServiceBuilder())
            .setArtifactDeclaration(buildArtifactDeclaration())
            .build();

    ConnectionValidationResult connectionValidationResult =
        connectivityTestingService.testConnection(Location.builderFromStringRepresentation(CONFIG_NAME).build());
    assertThat(connectionValidationResult.isValid(), equalTo(true));
  }

  @Test
  public void discoverAllValues() {
    DataProviderService dataProviderService =
        addDependency(toolingService.newDataProviderServiceBuilder())
            .setArtifactDeclaration(buildArtifactDeclaration())
            .build();
    DataProviderResult<List<DataResult>> providerResult = dataProviderService.discover();
    assertThat(providerResult.isSuccessful(), is(true));

    AtomicInteger totalValidations = new AtomicInteger();
    Consumer<DataResult> failureValidator = r -> {
      assertThat(r.isSuccessful(), is(false));
      totalValidations.incrementAndGet();
    };

    validateResult(providerResult.getResult(), "ActingParameterVP", failureValidator);
    validateResult(providerResult.getResult(), "ConfigLessConnectionLessNoActingParamVP", r -> {
      assertThat(r.isSuccessful(), is(true));
      assertThat(r.getData(), hasSize(1));
      assertThat(r.getData().iterator().next().getId(), is("ConfigLessConnectionLessNoActingParameter"));
      totalValidations.incrementAndGet();
    });
    validateResult(providerResult.getResult(), "ConfigLessNoActingParamVP", r -> {
      assertThat(r.isSuccessful(), is(true));
      assertThat(r.getData(), hasSize(1));
      assertThat(r.getData().iterator().next().getId(), is(CLIENT_NAME));
      totalValidations.incrementAndGet();
    });
    validateResult(providerResult.getResult(), "ConfigLessConnectionLessMetadataResolver", r -> {
      assertThat(r.isSuccessful(), is(true));
      assertThat(r.getData(), hasSize(1));
      assertThat(r.getData().iterator().next().getId(), is("ConfigLessConnectionLessMetadataResolver"));
      totalValidations.incrementAndGet();
    });
    validateResult(providerResult.getResult(), "ConfigLessMetadataResolver", r -> {
      assertThat(r.isSuccessful(), is(true));
      assertThat(r.getData(), hasSize(1));
      assertThat(r.getData().iterator().next().getId(), is(CLIENT_NAME));
      totalValidations.incrementAndGet();
    });
    assertThat(totalValidations.get(), is(providerResult.getResult().size()));
  }

  private void validateResult(List<DataResult> results, String resolverName, Consumer<DataResult> validator) {
    results.stream().filter(r -> r.getResolverName().equals(resolverName)).forEach(validator);
  }

  @Test
  public void configLessConnectionLessOnOperation() {
    ComponentElementDeclaration elementDeclaration = configLessConnectionLessOPDeclaration(CONFIG_NAME);
    getResultAndValidate(elementDeclaration, PROVIDED_PARAMETER_NAME, "ConfigLessConnectionLessNoActingParameter");
    getResultAndValidate(elementDeclaration, METADATA_KEY_PARAMETER, "ConfigLessConnectionLessMetadataResolver");
  }

  @Test
  public void configLessOnOperation() {
    ComponentElementDeclaration elementDeclaration = configLessOPDeclaration(CONFIG_NAME);
    getResultAndValidate(elementDeclaration, PROVIDED_PARAMETER_NAME, CLIENT_NAME);
    getResultAndValidate(elementDeclaration, METADATA_KEY_PARAMETER, CLIENT_NAME);
  }

  @Test
  public void actingParameterOnOperation() {
    final String actingParameter = "actingParameter";
    ComponentElementDeclaration elementDeclaration = actingParameterOPDeclaration(CONFIG_NAME, actingParameter);
    getResultAndValidate(elementDeclaration, PROVIDED_PARAMETER_NAME, actingParameter);
  }

  private void getResultAndValidate(ComponentElementDeclaration elementDeclaration, String parameterName, String expectedValue) {
    DataProviderService dataProviderService =
        addDependency(toolingService.newDataProviderServiceBuilder())
            .setArtifactDeclaration(buildArtifactDeclaration())
            .build();

    DataProviderResult<DataResult> providerResult =
        dataProviderService.getValues(elementDeclaration,
                                      parameterName);

    assertThat(providerResult.isSuccessful(), equalTo(true));
    DataResult dataResult = providerResult.getResult();
    assertThat(dataResult.isSuccessful(), is(true));
    assertThat(dataResult.getData(), hasSize(1));
    assertThat(dataResult.getData().iterator().next().getId(), is(expectedValue));
  }

  private <T extends ArtifactAgnosticServiceBuilder<T, ?>> T addDependency(T serviceBuilder) {
    return serviceBuilder.addDependency(EXTENSION_GROUP_ID, EXTENSION_ARTIFACT_ID, EXTENSION_VERSION, EXTENSION_CLASSIFIER,
                                        EXTENSION_TYPE);
  }

  private ArtifactDeclaration buildArtifactDeclaration() {
    return artifactDeclaration(configurationDeclaration(CONFIG_NAME, connectionDeclaration(CLIENT_NAME)));
  }

}