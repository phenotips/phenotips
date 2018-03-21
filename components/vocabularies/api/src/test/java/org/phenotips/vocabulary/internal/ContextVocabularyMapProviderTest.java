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
package org.phenotips.vocabulary.internal;

import org.phenotips.vocabulary.Vocabulary;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

/**
 * Test for the {@link ContextVocabularyMapProvider} component.
 *
 * @version $Id$
 */
public class ContextVocabularyMapProviderTest
{
    @Rule
    public MockitoComponentMockingRule<Provider<Map<String, Vocabulary>>> mocker =
        new MockitoComponentMockingRule<>(ContextVocabularyMapProvider.class);

    @Mock
    private Vocabulary vocabulary1;

    @Mock
    private Vocabulary vocabulary2;

    @Mock
    private Vocabulary vocabulary3;

    private ComponentManager componentManager;

    private Map<String, Vocabulary> vocabularies;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.vocabularies = new HashMap<>();
        this.vocabularies.put("v1", this.vocabulary1);
        this.vocabularies.put("v2", this.vocabulary2);
        this.vocabularies.put("v3", this.vocabulary3);
        this.componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        doReturn(this.vocabularies).when(this.componentManager).getInstanceMap(Vocabulary.class);
    }

    @Test
    public void allVocabulariesAreReturned() throws ComponentLookupException
    {
        Map<String, Vocabulary> result = this.mocker.getComponentUnderTest().get();
        Assert.assertEquals(3, result.size());
        Assert.assertSame(this.vocabulary1, result.get("v1"));
        Assert.assertSame(this.vocabulary2, result.get("v2"));
        Assert.assertSame(this.vocabulary3, result.get("v3"));
    }

    @Test
    public void componentLookupExceptionIsCaughtAndEmptyMapIsReturned() throws ComponentLookupException
    {
        doThrow(new ComponentLookupException("test")).when(this.componentManager).getInstanceMap(
            Vocabulary.class);
        Assert.assertTrue(this.mocker.getComponentUnderTest().get().isEmpty());
    }
}
