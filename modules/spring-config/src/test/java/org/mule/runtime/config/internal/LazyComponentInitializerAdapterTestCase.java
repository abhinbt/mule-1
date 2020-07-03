/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.internal;

import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.forExtension;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newArtifact;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_NAME;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.internal.dsl.DslConstants.FLOW_ELEMENT_IDENTIFIER;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ComponentLifeCycle;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.config.api.LazyComponentInitializer.ComponentLocationFilter;
import org.mule.runtime.config.dsl.model.AbstractDslModelTestCase;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;
import org.mule.runtime.dsl.api.ConfigResource;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.google.common.collect.ImmutableSet;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@RunWith(MockitoJUnitRunner.class)
@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(ComponentLifeCycle.COMPONENT_LIFE_CYCLE)
public class LazyComponentInitializerAdapterTestCase extends AbstractDslModelTestCase {

  private LazyMuleArtifactContext lazyMuleArtifactContext;

  private static final String MY_FLOW = "myFlow";

  private ElementDeclarer declarer;

  @Mock
  private ExtensionManager extensionManager;

  @Mock
  private OptionalObjectsController optionalObjectsController;

  @Mock
  private LockFactory lockFactory;

  @Mock(extraInterfaces = {Initialisable.class, Disposable.class, Startable.class, Stoppable.class})
  private Processor targetProcessor;

  @SuppressWarnings("deprecation")
  private MuleContextWithRegistry muleContext;

  private Set<ExtensionModel> extensions;

  private AtomicInteger initializations;

  @Before
  public void setup() throws Exception {
    initializations = new AtomicInteger(0);

    declarer = forExtension(EXTENSION_NAME);
    muleContext = mockContextWithServices();
    extensions = ImmutableSet.<ExtensionModel>builder()
        .add(getExtensionModel())
        .add(mockExtension)
        .build();

    MessageProcessorChainBuilder messageProcessorChainBuilder = new DefaultMessageProcessorChainBuilder().chain(targetProcessor);
    DefaultListableBeanFactory beanFactory = new ObjectProviderAwareBeanFactory(null);
    MuleRegistry mockedRegistry = muleContext.getRegistry();

    when(extensionManager.getExtensions()).thenReturn(extensions);
    when(muleContext.getExecutionClassLoader()).thenReturn(currentThread().getContextClassLoader());
    when(muleContext.getExtensionManager()).thenReturn(extensionManager);
    when(mockedRegistry.lookupObject(MY_FLOW)).thenReturn(messageProcessorChainBuilder);
    when(mockedRegistry.get(OBJECT_REGISTRY)).thenReturn(new DefaultRegistry(muleContext));

    lazyMuleArtifactContext = createLazyMuleArtifactContextStub(beanFactory);

    doAnswer(a -> {
      initializations.incrementAndGet();
      return null;
    }).when((Initialisable) targetProcessor).initialise();
  }


  @Test
  @Issue("MULE-18316")
  public void shouldNotCreateBeansForSameLocationRequest() {
    Location location = builderFromStringRepresentation(MY_FLOW).build();

    lazyMuleArtifactContext.initializeComponent(location);
    lazyMuleArtifactContext.initializeComponent(location);

    assertThat(initializations.get(), is(1));

  }

  @Test
  @Issue("MULE-18316")
  public void shouldCreateBeansForSameLocationRequestIfDifferentPhaseApplied() throws InitialisationException {
    Location location = builderFromStringRepresentation(MY_FLOW).build();

    lazyMuleArtifactContext.initializeComponent(location, false);
    lazyMuleArtifactContext.initializeComponent(location);

    assertThat(initializations.get(), is(2));
  }

  @Test
  @Issue("MULE-18316")
  public void shouldNotCreateBeansForSameLocationFilterRequest() {
    ComponentLocationFilter filter = loc -> loc.getLocation().equals(MY_FLOW);

    lazyMuleArtifactContext.initializeComponents(filter);
    lazyMuleArtifactContext.initializeComponents(filter);

    assertThat(initializations.get(), is(1));
  }

  @Test
  @Issue("MULE-18316")
  public void shouldCreateBeansForSameLocationFilterRequestIfDifferentPhaseApplied() {
    ComponentLocationFilter filter = loc -> loc.getLocation().equals(MY_FLOW);

    lazyMuleArtifactContext.initializeComponents(filter, false);
    lazyMuleArtifactContext.initializeComponents(filter);

    assertThat(initializations.get(), is(2));
  }

  @Override
  protected void initializeExtensionMock(ExtensionModel extension) {
    when(extension.getName()).thenReturn(EXTENSION_NAME);
  }

  private ArtifactDeclaration getSimpleApp() {
    return newArtifact()
        .withGlobalElement(forExtension(MULE_NAME)
            .newConstruct(FLOW_ELEMENT_IDENTIFIER)
            .withRefName(MY_FLOW)
            .getDeclaration())
        .getDeclaration();
  }

  private LazyMuleArtifactContext createLazyMuleArtifactContextStub(DefaultListableBeanFactory beanFactory) {
    LazyMuleArtifactContext muleArtifactContext =
        new LazyMuleArtifactContext(muleContext, new ConfigResource[0], getSimpleApp(),
                                    optionalObjectsController, new HashMap<>(), APP,
                                    emptyList(), empty(), empty(), true, lockFactory) {

          @Override
          protected DefaultListableBeanFactory createBeanFactory() {
            return beanFactory;
          }

          @Override
          protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
            // Bean factory is mocked, so no bean registering here
          }

          @Override
          protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            // Bean factory is mocked, so no bean registering here
          }

          @Override
          protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
            // Bean factory is mocked, so no bean registering here
          }

          @Override
          protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
            // Bean factory is mocked, so no bean invocation here
          }

          @Override
          protected void registerListeners() {
            // Bean factory is mocked, so no bean registering here
          }

          @Override
          protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
            // Bean factory is mocked, so no bean registering here
          }

          @Override
          protected void finishRefresh() {
            // Bean factory is mocked, so no nothing to do here
          }
        };

    muleArtifactContext.refresh();

    return muleArtifactContext;

  }
}
