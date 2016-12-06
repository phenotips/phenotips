/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.panels.rest.internal;

import org.phenotips.panels.rest.GenePanelsLoadingCache;

import org.xwiki.container.Container;
import org.xwiki.container.Request;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link DefaultGenePanelsResourceImpl}.
 *
 * @version $Id$
 * @since 1.3M5
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultGenePanelsResourceImplTest
{
    @InjectMocks
    private DefaultGenePanelsResourceImpl component;

    @Mock
    private Logger logger;

    @Mock
    private Container container;

    @Mock
    private GenePanelsLoadingCache loadingCache;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void determineLastIndexCalculatedTest()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final Method determineLastIndex = DefaultGenePanelsResourceImpl.class.getDeclaredMethod("determineLastIndex",
            int.class, int.class);
        determineLastIndex.setAccessible(true);

        final Object responseObj1 = determineLastIndex.invoke(this.component, 5, 4);
        assertEquals(4, responseObj1);

        final Object responseObj2 = determineLastIndex.invoke(this.component, 6, 6);
        assertEquals(6, responseObj2);

        final Object responseObj3 = determineLastIndex.invoke(this.component, 2, 50);
        assertEquals(2, responseObj3);
    }

    @Test
    public void determineFirstIndexCalculatedTest()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final Method determineFirstIndex = DefaultGenePanelsResourceImpl.class.getDeclaredMethod("determineFirstIndex",
            int.class, int.class);
        determineFirstIndex.setAccessible(true);

        final Object responseObj1 = determineFirstIndex.invoke(this.component, 6, 6);
        assertEquals(6, responseObj1);

        final Object responseObj2 = determineFirstIndex.invoke(this.component, 2, 50);
        assertEquals(2, responseObj2);

        this.exception.expect(InvocationTargetException.class);
        determineFirstIndex.invoke(this.component, 5, 4);
    }

    @Test
    public void getAmendedJSONReturnOneItem()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        // FIXME test original JSONObject remains unchanged.
        final JSONObject jsonObject = new JSONObject();
        final JSONArray genesArray = new JSONArray();
        jsonObject.put("size", 3);
        genesArray.put(new JSONObject("{\"gene\":\"gene1\", \"count\":3}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene2\", \"count\":2}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene3\", \"count\":1}"));
        jsonObject.put("genes", genesArray);

        final Method getAmendedJSON = DefaultGenePanelsResourceImpl.class.getDeclaredMethod("getAmendedJSON",
            JSONObject.class, int.class, int.class, int.class);
        getAmendedJSON.setAccessible(true);

        final JSONObject expected = new JSONObject();
        expected.put("size", 1);
        expected.put("genes", new JSONArray("[{\"gene\":\"gene2\", \"count\":2}]"));
        expected.put("totalPages", 3);

        final Object responseObj = getAmendedJSON.invoke(this.component, jsonObject, 1, 1, 1);

        assertEquals(expected.toString(), responseObj.toString());
    }

    @Test
    public void getAmendedJSONReturnSubsetOfItems()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        // FIXME test original JSONObject remains unchanged.
        final JSONObject jsonObject = new JSONObject();
        final JSONArray genesArray = new JSONArray();
        jsonObject.put("size", 3);
        genesArray.put(new JSONObject("{\"gene\":\"gene1\", \"count\":3}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene2\", \"count\":2}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene3\", \"count\":1}"));
        jsonObject.put("genes", genesArray);

        final Method getAmendedJSON = DefaultGenePanelsResourceImpl.class.getDeclaredMethod("getAmendedJSON",
            JSONObject.class, int.class, int.class, int.class);
        getAmendedJSON.setAccessible(true);

        final JSONObject expected = new JSONObject();
        expected.put("size", 2);
        expected.put("genes", new JSONArray("[{\"gene\":\"gene1\", \"count\":3},{\"gene\":\"gene2\", \"count\":2}]"));
        expected.put("totalPages", 2);

        final Object responseObj = getAmendedJSON.invoke(this.component, jsonObject, 0, 1, 2);

        assertEquals(expected.toString(), responseObj.toString());
    }

    @Test
    public void getAmendedJSONReturnAllItems()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        // FIXME test original JSONObject remains unchanged.
        final JSONObject jsonObject = new JSONObject();
        final JSONArray genesArray = new JSONArray();
        jsonObject.put("size", 3);
        genesArray.put(new JSONObject("{\"gene\":\"gene1\", \"count\":3}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene2\", \"count\":2}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene3\", \"count\":1}"));
        jsonObject.put("genes", genesArray);

        final Method getAmendedJSON = DefaultGenePanelsResourceImpl.class.getDeclaredMethod("getAmendedJSON",
            JSONObject.class, int.class, int.class, int.class);
        getAmendedJSON.setAccessible(true);

        final JSONObject expected = new JSONObject();
        expected.put("size", 3);
        expected.put("genes", new JSONArray("[{\"gene\":\"gene1\", \"count\":3},"
            + "{\"gene\":\"gene2\", \"count\":2},{\"gene\":\"gene3\", \"count\":1}]"));
        expected.put("totalPages", 1);

        final Object responseObj = getAmendedJSON.invoke(this.component, jsonObject, 0, 2, 3);

        assertEquals(expected.toString(), responseObj.toString());
    }

    @Test
    public void getPageDataStartPageNull()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final JSONObject jsonObject = new JSONObject();
        final JSONArray genesArray = new JSONArray();
        jsonObject.put("size", 3);
        genesArray.put(new JSONObject("{\"gene\":\"gene1\", \"count\":3}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene2\", \"count\":2}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene3\", \"count\":1}"));
        jsonObject.put("genes", genesArray);

        final Method getPageData = DefaultGenePanelsResourceImpl.class.getDeclaredMethod("getPageData",
            JSONObject.class, Object.class, Object.class);
        getPageData.setAccessible(true);

        final Object responseObj = getPageData.invoke(this.component, jsonObject, null, "50");
        assertEquals(3, ((JSONObject) responseObj).getInt("size"));
        assertEquals(1, ((JSONObject) responseObj).getInt("totalPages"));
        assertEquals("[{\"gene\":\"gene1\",\"count\":3},{\"gene\":\"gene2\",\"count\":2},"
            + "{\"gene\":\"gene3\",\"count\":1}]", ((JSONObject) responseObj).getJSONArray("genes").toString());
    }

    @Test
    public void getPageDataNumResultsNull()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final JSONObject jsonObject = new JSONObject();
        final JSONArray genesArray = new JSONArray();
        jsonObject.put("size", 3);
        genesArray.put(new JSONObject("{\"gene\":\"gene1\", \"count\":3}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene2\", \"count\":2}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene3\", \"count\":1}"));
        jsonObject.put("genes", genesArray);

        final Method getPageData = DefaultGenePanelsResourceImpl.class.getDeclaredMethod("getPageData",
            JSONObject.class, Object.class, Object.class);
        getPageData.setAccessible(true);

        final Object responseObj = getPageData.invoke(this.component, jsonObject, "1", null);
        assertEquals(3, ((JSONObject) responseObj).getInt("size"));
        assertEquals(1, ((JSONObject) responseObj).getInt("totalPages"));
        assertEquals("[{\"gene\":\"gene1\",\"count\":3},{\"gene\":\"gene2\",\"count\":2},"
            + "{\"gene\":\"gene3\",\"count\":1}]", ((JSONObject) responseObj).getJSONArray("genes").toString());
    }

    @Test
    public void getPageDataStartPageIllegalDataType()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final JSONObject jsonObject = new JSONObject();
        final JSONArray genesArray = new JSONArray();
        jsonObject.put("size", 3);
        genesArray.put(new JSONObject("{\"gene\":\"gene1\", \"count\":3}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene2\", \"count\":2}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene3\", \"count\":1}"));
        jsonObject.put("genes", genesArray);

        final Method getPageData = DefaultGenePanelsResourceImpl.class.getDeclaredMethod("getPageData",
            JSONObject.class, Object.class, Object.class);
        getPageData.setAccessible(true);

        this.exception.expect(InvocationTargetException.class);
        getPageData.invoke(this.component, jsonObject, "1b", "50");
    }

    @Test
    public void getPageDataNumResultsIllegalDataType()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final JSONObject jsonObject = new JSONObject();
        final JSONArray genesArray = new JSONArray();
        jsonObject.put("size", 3);
        genesArray.put(new JSONObject("{\"gene\":\"gene1\", \"count\":3}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene2\", \"count\":2}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene3\", \"count\":1}"));
        jsonObject.put("genes", genesArray);

        final Method getPageData = DefaultGenePanelsResourceImpl.class.getDeclaredMethod("getPageData",
            JSONObject.class, Object.class, Object.class);
        getPageData.setAccessible(true);

        this.exception.expect(InvocationTargetException.class);
        getPageData.invoke(this.component, jsonObject, "1", "40.6");
    }

    @Test
    public void getPageDataStartPageOutOfBounds()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final JSONObject jsonObject = new JSONObject();
        final JSONArray genesArray = new JSONArray();
        jsonObject.put("size", 3);
        genesArray.put(new JSONObject("{\"gene\":\"gene1\", \"count\":3}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene2\", \"count\":2}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene3\", \"count\":1}"));
        jsonObject.put("genes", genesArray);

        final Method getPageData = DefaultGenePanelsResourceImpl.class.getDeclaredMethod("getPageData",
            JSONObject.class, Object.class, Object.class);
        getPageData.setAccessible(true);

        this.exception.expect(InvocationTargetException.class);
        getPageData.invoke(this.component, jsonObject, "50", "10");
    }

    @Test
    public void getPageDataNumResultsTooMany()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final JSONObject jsonObject = new JSONObject();
        final JSONArray genesArray = new JSONArray();
        jsonObject.put("size", 3);
        genesArray.put(new JSONObject("{\"gene\":\"gene1\", \"count\":3}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene2\", \"count\":2}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene3\", \"count\":1}"));
        jsonObject.put("genes", genesArray);

        final Method getPageData = DefaultGenePanelsResourceImpl.class.getDeclaredMethod("getPageData",
            JSONObject.class, Object.class, Object.class);
        getPageData.setAccessible(true);

        final Object responseObj = getPageData.invoke(this.component, jsonObject, "1", "50");
        assertEquals(3, ((JSONObject) responseObj).getInt("size"));
        assertEquals(1, ((JSONObject) responseObj).getInt("totalPages"));
        assertEquals("[{\"gene\":\"gene1\",\"count\":3},{\"gene\":\"gene2\",\"count\":2},"
            + "{\"gene\":\"gene3\",\"count\":1}]", ((JSONObject) responseObj).getJSONArray("genes").toString());
    }

    @Test
    public void getPageDataNumResultsLess()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        final JSONObject jsonObject = new JSONObject();
        final JSONArray genesArray = new JSONArray();
        jsonObject.put("size", 3);
        genesArray.put(new JSONObject("{\"gene\":\"gene1\", \"count\":3}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene2\", \"count\":2}"));
        genesArray.put(new JSONObject("{\"gene\":\"gene3\", \"count\":1}"));
        jsonObject.put("genes", genesArray);

        final Method getPageData = DefaultGenePanelsResourceImpl.class.getDeclaredMethod("getPageData",
            JSONObject.class, Object.class, Object.class);
        getPageData.setAccessible(true);

        final Object responseObj = getPageData.invoke(this.component, jsonObject, "1", "2");
        assertEquals(2, ((JSONObject) responseObj).getInt("size"));
        assertEquals(2, ((JSONObject) responseObj).getInt("totalPages"));
        assertEquals("[{\"gene\":\"gene1\",\"count\":3},{\"gene\":\"gene2\",\"count\":2}]",
            ((JSONObject) responseObj).getJSONArray("genes").toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getGeneCountsFromPhenotypesNoGeneDataReturned() throws Exception
    {
        final Request request = mock(Request.class);
        final List<Object> inputData = ImmutableList.<Object>of("HP:0001", "HP:0002", "HP:0003");
        doReturn(request).when(this.container).getRequest();
        doReturn(inputData).when(request).getProperties("id");
        doReturn("1").when(request).getProperty("startPage");
        doReturn("2").when(request).getProperty("numResults");
        doReturn("1").when(request).getProperty("reqNo");

        final LoadingCache mockCache = mock(LoadingCache.class);
        doReturn(mockCache).when(this.loadingCache).getCache();

        // ExecutionException is protected, use reflection to get class instance.
        final Class<?> executionExceptionClass = Class.forName("java.util.concurrent.ExecutionException");
        final Constructor<?> constructor = executionExceptionClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        final ExecutionException executionException = (ExecutionException) constructor.newInstance();

        doThrow(executionException).when(mockCache).get(Matchers.anyString());

        final Response response = this.component.getGeneCountsFromPhenotypes();
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(this.logger).warn("No content associated with [{}]", inputData);
    }
}
