/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.exception.MessagingException;

/**
 * Commits any pending transaction.
 * <p/>
 * This interceptor must be executed before the error handling interceptor so if there is any failure doing commit, the error
 * handler gets executed.
 */
public class CommitTransactionInterceptor implements ExecutionInterceptor<CoreEvent> {

  private final ExecutionInterceptor<CoreEvent> nextInterceptor;

  public CommitTransactionInterceptor(ExecutionInterceptor<CoreEvent> nextInterceptor) {
    this.nextInterceptor = nextInterceptor;
  }

  @Override
  public CoreEvent execute(ExecutionCallback<CoreEvent> callback, ExecutionContext executionContext) throws Exception {
    CoreEvent result = nextInterceptor.execute(callback, executionContext);
    if (executionContext.needsTransactionResolution()) {
      try {
        TransactionCoordination.getInstance().resolveTransaction();
      } catch (Exception e) {
        throw new MessagingException(result, e);
      }
    }
    return result;
  }
}
