package org.mule.tooling.extensions.metadata.internal.value;

import static java.util.Collections.singleton;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;


import java.util.Set;

public class ConfigLessConnectionLessNoActingParamVP implements ValueProvider {

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return singleton(new SimpleValue("ConfigLessConnectionLessNoActingParameter"));
  }
}
