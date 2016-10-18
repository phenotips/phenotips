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
package org.phenotips.storage.migrators.internal;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.ConversionException;
import org.xwiki.environment.Environment;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class LegacyXWikiConfigurationSourceTest
{
    @Rule
    public final MockitoComponentMockingRule<ConfigurationSource> mocker =
        new MockitoComponentMockingRule<ConfigurationSource>(
            LegacyXWikiConfigurationSource.class);

    @Before
    public void setup() throws ComponentLookupException
    {
        Environment env = this.mocker.getInstance(Environment.class);
        when(env.getResourceAsStream("/WEB-INF/xwiki.cfg"))
            .thenReturn(this.getClass().getResourceAsStream("/xwiki.cfg"));
        ConverterManager conv = this.mocker.getInstance(ConverterManager.class);
        when(conv.convert(Integer.class, "42")).thenReturn(Integer.valueOf(42));
        when(conv.convert(Boolean.class, "true")).thenReturn(Boolean.TRUE);
        when(conv.convert(Integer.class, "Hello World!")).thenThrow(
            new org.apache.commons.configuration.ConversionException());
    }

    @Test
    public void getStringProperty() throws ComponentLookupException
    {
        Assert.assertEquals("Hello World!", this.mocker.getComponentUnderTest().getProperty("a.string"));
        Assert.assertEquals("Hello World!", this.mocker.getComponentUnderTest().getProperty("a.string", (String) null));
    }

    @Test
    public void getIntegerProperty() throws ComponentLookupException
    {
        Assert.assertEquals(42, this.mocker.getComponentUnderTest().<Integer>getProperty("an.integer", Integer.class)
            .intValue());
    }

    @Test(expected = ConversionException.class)
    public void getInvalidIntegerProperty() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().<Integer>getProperty("a.string", Integer.class);
    }

    @Test
    public void getBooleanProperty() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().<Boolean>getProperty("a.boolean", Boolean.class));
    }

    @Test
    public void getListProperty() throws ComponentLookupException
    {
        List<String> defaultValue = Collections.singletonList("default value");
        List<String> result = this.mocker.getComponentUnderTest().getProperty("simple.list", defaultValue);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("xwiki/2.0", result.get(0));
        Assert.assertEquals("xwiki/2.1", result.get(1));
    }

    @Test
    public void undefinedPropertyReturnsNull() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().getProperty("not.defined"));
        Assert.assertNull(this.mocker.getComponentUnderTest().getProperty("not.defined", String.class));
    }

    @Test
    public void commentedPropertyReturnsNull() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().getProperty("commented"));
        Assert.assertNull(this.mocker.getComponentUnderTest().getProperty("commented", String.class));
    }

    @Test
    public void lastDefinedValueIsReturned() throws ComponentLookupException
    {
        Assert.assertEquals("right", this.mocker.getComponentUnderTest().getProperty("last.value.is.used"));
    }

    @Test
    public void getListWithNewlines() throws ComponentLookupException
    {
        @SuppressWarnings("unchecked")
        List<String> result = this.mocker.getComponentUnderTest().getProperty("list.with.newlines", List.class);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("value 1", result.get(0));
        Assert.assertEquals("value 2", result.get(1));
        Assert.assertEquals("value 3", result.get(2));
    }

    @Test
    public void defaultValueIsUsedWhenPropertyIsNotDefined() throws ComponentLookupException
    {
        Assert.assertEquals("right", this.mocker.getComponentUnderTest().getProperty("not.defined", "right"));
        Assert.assertNull(this.mocker.getComponentUnderTest().getProperty("not.defined", (String) null));
    }

    @Test
    public void defaultValueIsIgnoredWhenPropertyIsDefined() throws ComponentLookupException
    {
        Assert.assertEquals("override", this.mocker.getComponentUnderTest().getProperty("defaulted", "incorrect"));
    }

    @Test
    public void getKeysReturnsEachKeyOnce() throws ComponentLookupException
    {
        List<String> keys = this.mocker.getComponentUnderTest().getKeys();
        Assert.assertEquals(10, keys.size());
        Assert.assertFalse(keys.contains("commented"));
        Assert.assertFalse(keys.contains("#commented"));
        Assert.assertTrue(keys.contains("an.integer"));
        Assert.assertTrue(keys.contains("a.boolean"));
        Assert.assertTrue(keys.contains("space"));
        Assert.assertTrue(keys.contains("Just.a.keyWord"));
    }

    @Test
    public void containsKey() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().containsKey("commented"));
        Assert.assertFalse(this.mocker.getComponentUnderTest().containsKey("#commented"));
        Assert.assertFalse(this.mocker.getComponentUnderTest().containsKey("World!"));
        Assert.assertTrue(this.mocker.getComponentUnderTest().containsKey("an.integer"));
        Assert.assertTrue(this.mocker.getComponentUnderTest().containsKey("a.boolean"));
        Assert.assertTrue(this.mocker.getComponentUnderTest().containsKey("last.value.is.used"));
        Assert.assertTrue(this.mocker.getComponentUnderTest().containsKey("space"));
        Assert.assertTrue(this.mocker.getComponentUnderTest().containsKey("Just.a.keyWord"));
        Assert.assertFalse(this.mocker.getComponentUnderTest().containsKey("Just.a.keyword"));
    }

    @Test
    public void isEmpty() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().isEmpty());
    }

    @Test
    public void missingConfigurationIsOK() throws ComponentLookupException
    {
        Environment env = this.mocker.getInstance(Environment.class);
        when(env.getResourceAsStream("/WEB-INF/xwiki.cfg")).thenReturn(null);
        Assert.assertTrue(this.mocker.getComponentUnderTest().isEmpty());
        Assert.assertFalse(this.mocker.getComponentUnderTest().containsKey("a.string"));
        Assert.assertNull(this.mocker.getComponentUnderTest().getProperty("a.string"));
        Assert.assertEquals("def", this.mocker.getComponentUnderTest().getProperty("a.string", "def"));
    }

    @Test
    public void emptyConfigurationIsOK() throws ComponentLookupException
    {
        Environment env = this.mocker.getInstance(Environment.class);
        when(env.getResourceAsStream("/WEB-INF/xwiki.cfg")).thenReturn(
            this.getClass().getResourceAsStream("/empty.cfg"));
        Assert.assertTrue(this.mocker.getComponentUnderTest().isEmpty());
        Assert.assertFalse(this.mocker.getComponentUnderTest().containsKey("a.string"));
        Assert.assertNull(this.mocker.getComponentUnderTest().getProperty("a.string"));
        Assert.assertEquals("def", this.mocker.getComponentUnderTest().getProperty("a.string", "def"));
    }

    @Test
    public void failedConfigurationIsOK() throws ComponentLookupException
    {
        Environment env = this.mocker.getInstance(Environment.class);
        when(env.getResourceAsStream("/WEB-INF/xwiki.cfg")).thenThrow(new NotImplementedException(""));
        Assert.assertTrue(this.mocker.getComponentUnderTest().isEmpty());
        Assert.assertFalse(this.mocker.getComponentUnderTest().containsKey("a.string"));
        Assert.assertNull(this.mocker.getComponentUnderTest().getProperty("a.string"));
        Assert.assertEquals("def", this.mocker.getComponentUnderTest().getProperty("a.string", "def"));
    }
}
