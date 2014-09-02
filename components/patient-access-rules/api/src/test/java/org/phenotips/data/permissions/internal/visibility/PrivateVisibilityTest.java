/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.permissions.internal.visibility;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Visibility;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link PrivateVisibility private visibility level}.
 *
 * @version $Id$
 */
public class PrivateVisibilityTest
{
    @Rule
    public final MockitoComponentMockingRule<Visibility> mocker =
        new MockitoComponentMockingRule<Visibility>(PrivateVisibility.class);

    /** Basic test for {@link Visibility#getName()}. */
    @Test
    public void getName() throws ComponentLookupException
    {
        Assert.assertEquals("private", this.mocker.getComponentUnderTest().getName());
    }

    /** Basic test for {@link Visibility#getLabel()}. */
    @Test
    public void getLabel() throws ComponentLookupException
    {
        LocalizationContext lc = this.mocker.getInstance(LocalizationContext.class);
        LocalizationManager lm = this.mocker.getInstance(LocalizationManager.class);
        Translation t = mock(Translation.class);
        Block b = mock(Block.class);
        BlockRenderer r = this.mocker.getInstance(BlockRenderer.class, "plain/1.0");
        when(lc.getCurrentLocale()).thenReturn(Locale.US);
        when(lm.getTranslation("phenotips.permissions.visibility.private.label", Locale.US)).thenReturn(t);
        when(t.render(Locale.US)).thenReturn(b);
        Mockito.doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                WikiPrinter printer = (WikiPrinter) invocation.getArguments()[1];
                printer.print("Private");
                return null;
            }
        }).when(r).render(same(b), any(WikiPrinter.class));
        Assert.assertEquals("Private", this.mocker.getComponentUnderTest()
            .getLabel());
    }

    /** {@link Visibility#getLabel()} returns the capitalized name when a translation isn't found. */
    @Test
    public void getLabelWithoutTranslation() throws ComponentLookupException
    {
        Assert.assertEquals("Private", this.mocker.getComponentUnderTest().getLabel());
    }

    /** Basic test for {@link Visibility#getDescription()}. */
    @Test
    public void getDescription() throws ComponentLookupException
    {
        LocalizationContext lc = this.mocker.getInstance(LocalizationContext.class);
        LocalizationManager lm = this.mocker.getInstance(LocalizationManager.class);
        Translation t = mock(Translation.class);
        Block b = mock(Block.class);
        BlockRenderer r = this.mocker.getInstance(BlockRenderer.class, "plain/1.0");
        when(lc.getCurrentLocale()).thenReturn(Locale.US);
        when(lm.getTranslation("phenotips.permissions.visibility.private.description", Locale.US)).thenReturn(t);
        when(t.render(Locale.US)).thenReturn(b);
        Mockito.doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                WikiPrinter printer = (WikiPrinter) invocation.getArguments()[1];
                printer.print("Private cases are only accessible to their owners.");
                return null;
            }
        }).when(r).render(same(b), any(WikiPrinter.class));
        Assert.assertEquals("Private cases are only accessible to their owners.", this.mocker.getComponentUnderTest()
            .getDescription());
    }

    /** {@link Visibility#getDescription()} returns the empty string when a translation isn't found. */
    @Test
    public void getDescriptionWithoutTranslation() throws ComponentLookupException
    {
        Assert.assertEquals("", this.mocker.getComponentUnderTest().getDescription());
    }

    /** Basic test for {@link Visibility#toString()}. */
    @Test
    public void toStringTest() throws ComponentLookupException
    {
        Assert.assertEquals("private", this.mocker.getComponentUnderTest().toString());
    }

    /** Basic test for {@link Visibility#equals(Object)}. */
    @Test
    public void equalsTest() throws ComponentLookupException
    {
        // Equals itself
        Assert.assertTrue(this.mocker.getComponentUnderTest().equals(this.mocker.getComponentUnderTest()));
        // Never equals null
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals(null));
        Visibility other = mock(Visibility.class);
        when(other.getName()).thenReturn("private", "private", "public", "public");
        AccessLevel edit = mock(AccessLevel.class);
        AccessLevel none = this.mocker.getInstance(AccessLevel.class, "none");
        when(other.getDefaultAccessLevel()).thenReturn(none, edit, none, edit);
        // Equals another visibility with the same name and access level
        Assert.assertTrue(this.mocker.getComponentUnderTest().equals(other));
        // Doesn't equal a visibility with a different access level but the same name
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals(other));
        // Doesn't equal a visibility with the same access level but a different name
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals(other));
        // Doesn't equal a visibility with a different access level and a different name
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals(other));
        // Doesn't equal other types of objects
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals("private"));
    }

    /** Basic test for {@link Visibility#compareTo(Visibility)}. */
    @Test
    public void compareToTest() throws ComponentLookupException
    {
        // Equals itself
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().compareTo(this.mocker.getComponentUnderTest()));
        // Nulls come after
        Assert.assertTrue(this.mocker.getComponentUnderTest().compareTo(null) < 0);
        AccessLevel other = mock(AccessLevel.class);
        AccessLevel none = this.mocker.getInstance(AccessLevel.class, "none");
        // Equals another visibility with the same permissiveness
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().compareTo(new MockVisibility("personal", 0, none)));
        // Respects the permissiveness order
        Assert.assertTrue(this.mocker.getComponentUnderTest().compareTo(new MockVisibility("hidden", -10, other)) > 0);
        Assert.assertTrue(this.mocker.getComponentUnderTest().compareTo(new MockVisibility("open", 100, other)) < 0);
        // Other types of visibilities are placed after
        Assert.assertTrue(this.mocker.getComponentUnderTest().compareTo(mock(Visibility.class)) < 0);
    }
}
