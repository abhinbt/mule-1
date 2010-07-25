/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import org.mule.api.config.MuleProperties;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.model.InvocationResult;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class InboundHeadersAnnotationTestCase extends AbstractAnnotatedEntrypointResolverTestCase
{
    @Override
    protected Object getComponent()
    {
        return new InboundHeadersAnnotationComponent();
    }

    public void testSingleHeader() throws Exception
    {
        InvocationResult response = invokeResolver("processHeader", eventContext);
        assertEquals("fooValue", response.getResult());
    }

    public void testSingleHeaderOptional() throws Exception
    {
        InvocationResult response = invokeResolver("processHeaderOptional", eventContext);
        assertEquals("faz not set", response.getResult());
    }

    public void testSingleHeaderWithType() throws Exception
    {
        Apple apple = new Apple();
        eventContext.getMessage().setInboundProperty("apple", apple);
        InvocationResult response = invokeResolver("processHeaderWithType", eventContext);
        assertEquals(apple, response.getResult());
    }

    public void testSingleHeaderWithBaseType() throws Exception
    {
        Apple apple = new Apple();
        eventContext.getMessage().setInboundProperty("apple", apple);
        InvocationResult response = invokeResolver("processHeaderWithBaseType", eventContext);
        assertEquals(apple, response.getResult());
    }

    public void testMapHeaders() throws Exception
    {
        InvocationResult response = invokeResolver("processHeaders", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response.getResult();
        assertEquals(2, result.size());
        assertEquals("fooValue", result.get("foo"));
        assertEquals("barValue", result.get("bar"));
        assertNull(result.get("baz"));
    }

    public void testMapHeadersMissing() throws Exception
    {
        eventContext.getMessage().removeProperty("foo", PropertyScope.INBOUND);
        try
        {
            invokeResolver("processHeaders", eventContext);
            fail("Foo header is missing but required");
        }
        catch (RequiredValueException e)
        {
            //expected
        }
    }

    public void testMapSingleHeader() throws Exception
    {
        InvocationResult response = invokeResolver("processSingleMapHeader", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response.getResult();
        assertEquals(1, result.size());
        assertEquals("fooValue", result.get("foo"));
        assertNull(result.get("bar"));
        assertNull(result.get("baz"));
    }

    public void testMapHeadersOptional() throws Exception
    {
        eventContext.getMessage().removeProperty("baz", PropertyScope.INBOUND);

        InvocationResult response = invokeResolver("processHeadersOptional", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response.getResult();
        assertEquals(2, result.size());
        assertEquals("fooValue", result.get("foo"));
        assertEquals("barValue", result.get("bar"));
        assertNull(result.get("baz"));
    }

    public void testMapHeadersAllOptional() throws Exception
    {
        eventContext.getMessage().clearProperties(PropertyScope.INBOUND);

        InvocationResult response = invokeResolver("processHeadersAllOptional", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response.getResult();
        //We just want ot make sure we don't return null collections
        assertEquals(0, result.size());
    }

    public void testMapHeadersUnmodifiable() throws Exception
    {
        try
        {
            invokeResolver("processUnmodifiableHeaders", eventContext);
            fail("Foo header is missing but required");
        }
        catch (InvocationTargetException e)
        {
            //expected
            assertTrue(e.getTargetException() instanceof UnsupportedOperationException);
        }
    }

    public void testMapHeadersAll() throws Exception
    {
        InvocationResult response = invokeResolver("processHeadersOptional", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response.getResult();
        //Will include all Mule headers too
        assertTrue(result.size() >= 3);
        assertEquals("fooValue", result.get("foo"));
        assertEquals("barValue", result.get("bar"));
        assertEquals("bazValue", result.get("baz"));
    }

    public void testMapHeadersWildcard() throws Exception
    {
        eventContext.getMessage().setInboundProperty(MuleProperties.MULE_ENCODING_PROPERTY, "UTF-8");
        InvocationResult response = invokeResolver("processHeadersWildcard", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response.getResult();
        //Will match all Mule headers
        assertEquals(1, result.size());
        assertEquals(result.get(MuleProperties.MULE_ENCODING_PROPERTY), "UTF-8");
    }

    public void testMapHeadersMultiWildcard() throws Exception
    {
        eventContext.getMessage().setInboundProperty(MuleProperties.MULE_ENCODING_PROPERTY, "UTF-8");
        InvocationResult response = invokeResolver("processHeadersMultiWildcard", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response.getResult();
        //Will match all Mule headers
        assertEquals(3, result.size());

        //Match on MULE_*
        assertEquals(result.get(MuleProperties.MULE_ENCODING_PROPERTY), "UTF-8");

        //Match on ba*
        assertEquals(result.get("bar"), "barValue");
        assertEquals(result.get("baz"), "bazValue");

    }

    public void testMapHeadersWithGenerics() throws Exception
    {
        eventContext.getMessage().setInboundProperty("apple", new Apple());
        eventContext.getMessage().setInboundProperty("banana", new Banana());
        eventContext.getMessage().setInboundProperty("orange", new Orange());

        eventContext.getMessage().setInboundProperty(MuleProperties.MULE_ENCODING_PROPERTY, "UTF-8");
        InvocationResult response = invokeResolver("processHeadersWithGenerics", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) response.getResult();
        //Will match all Mule headers
        assertEquals(2, result.size());

        assertEquals(new Apple(), result.get("apple"));
        assertEquals(new Orange(), result.get("orange"));
        assertNull(result.get("banana"));
    }

    public void testListHeaders() throws Exception
    {
        InvocationResult response = invokeResolver("processHeadersList", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<?> result = (List<?>) response.getResult();
        assertEquals(3, result.size());
        assertTrue(result.contains("fooValue"));
        assertTrue(result.contains("barValue"));
        assertTrue(result.contains("bazValue"));
    }

    public void testListHeadersWithOptional() throws Exception
    {
        eventContext.getMessage().removeProperty("baz", PropertyScope.INBOUND);
        InvocationResult response = invokeResolver("processHeadersListOptional", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<?> result = (List<?>) response.getResult();
        assertEquals(2, result.size());
        assertTrue(result.contains("fooValue"));
        assertTrue(result.contains("barValue"));
    }

    public void testListHeadersWithMissing() throws Exception
    {
        eventContext.getMessage().removeProperty("bar", PropertyScope.INBOUND);
        try
        {
            invokeResolver("processHeadersList", eventContext);
            fail("Bar header is missing but required");
        }
        catch (RequiredValueException e)
        {
            //expected
        }
    }

    public void testSingleListHeader() throws Exception
    {
        InvocationResult response = invokeResolver("processSingleHeaderList", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<?> result = (List<?>) response.getResult();
        assertEquals(1, result.size());
        assertTrue(result.contains("fooValue"));
    }

    public void testListHeadersUnmodifiable() throws Exception
    {
       try
        {
            invokeResolver("processUnmodifiableHeadersList", eventContext);
            fail("Foo header is missing but required");
        }
        catch (InvocationTargetException e)
        {
            //expected
            assertTrue(e.getTargetException() instanceof UnsupportedOperationException);
        }
    }

    public void testListHeadersAll() throws Exception
    {
        InvocationResult response = invokeResolver("processHeadersListAll", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<?> result = (List<?>) response.getResult();
        //Will include all Mule headers too
        assertTrue(result.size() >= 3);
        assertTrue(result.contains("fooValue"));
        assertTrue(result.contains("barValue"));
        assertTrue(result.contains("bazValue"));
    }

    public void testMapHeadersListAllOptional() throws Exception
    {
        eventContext.getMessage().clearProperties(PropertyScope.INBOUND);

        InvocationResult response = invokeResolver("processHeadersListAllOptional", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<?> result = (List<?>) response.getResult();
        //We just want ot make sure we don't return null collections
        assertEquals(0, result.size());
    }

    public void testListHeadersWilcard() throws Exception
    {
        eventContext.getMessage().setInboundProperty(MuleProperties.MULE_ENCODING_PROPERTY, "UTF-8");
        InvocationResult response = invokeResolver("processHeadersListWildcard", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<?> result = (List<?>) response.getResult();
        //Will match all Mule headers
        assertEquals(1, result.size());
        //MULE_ENCODING
        assertTrue(result.contains("UTF-8"));
    }

    public void testListHeadersMultiWilcard() throws Exception
    {
        eventContext.getMessage().setInboundProperty(MuleProperties.MULE_ENCODING_PROPERTY, "UTF-8");
        InvocationResult response = invokeResolver("processHeadersListMultiWildcard", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<?> result = (List<?>) response.getResult();
        //Will match on MULE_* and ba*
        assertEquals(3, result.size());

        //Match on MULE_*
        //MULE_ENCODING
        assertTrue(result.contains("UTF-8"));

        //Match on ba*
        //bar
        assertTrue(result.contains("barValue"));

        //baz
        assertTrue(result.contains("bazValue"));
    }

    public void testListHeadersWithGenerics() throws Exception
    {
        Apple apple = new Apple();
        Banana banana = new Banana();
        Orange orange = new Orange();
        eventContext.getMessage().setInboundProperty("apple", apple);
        eventContext.getMessage().setInboundProperty("banana", banana);
        eventContext.getMessage().setInboundProperty("orange", orange);

        eventContext.getMessage().setInboundProperty(MuleProperties.MULE_ENCODING_PROPERTY, "UTF-8");
        InvocationResult response = invokeResolver("processHeadersListWithGenerics", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<?> result = (List<?>) response.getResult();
        //Will match all Mule headers
        assertEquals(2, result.size());

        assertTrue(result.contains(apple));
        assertTrue(result.contains(orange));
        assertFalse(result.contains(banana));
    }
}
