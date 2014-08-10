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
 * Tests for the {@link PublicVisibility public visibility level}.
 *
 * @version $Id$
 */
public class PublicVisibilityTest
{
    @Rule
    public final MockitoComponentMockingRule<Visibility> mocker =
        new MockitoComponentMockingRule<Visibility>(PublicVisibility.class);

    /** Basic test for {@link Visibility#getName()}. */
    @Test
    public void getName() throws ComponentLookupException
    {
        Assert.assertEquals("public", this.mocker.getComponentUnderTest().getName());
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
        when(lm.getTranslation("phenotips.permissions.visibility.public.label", Locale.US)).thenReturn(t);
        when(t.render(Locale.US)).thenReturn(b);
        Mockito.doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                WikiPrinter printer = (WikiPrinter) invocation.getArguments()[1];
                printer.print("Public");
                return null;
            }
        }).when(r).render(same(b), any(WikiPrinter.class));
        Assert.assertEquals("Public", this.mocker.getComponentUnderTest()
            .getLabel());
    }

    /** {@link Visibility#getLabel()} returns the capitalized name when a translation isn't found. */
    @Test
    public void getLabelWithoutTranslation() throws ComponentLookupException
    {
        Assert.assertEquals("Public", this.mocker.getComponentUnderTest().getLabel());
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
        when(lm.getTranslation("phenotips.permissions.visibility.public.description", Locale.US)).thenReturn(t);
        when(t.render(Locale.US)).thenReturn(b);
        Mockito.doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                WikiPrinter printer = (WikiPrinter) invocation.getArguments()[1];
                printer.print("All registered users can view public cases.");
                return null;
            }
        }).when(r).render(same(b), any(WikiPrinter.class));
        Assert.assertEquals("All registered users can view public cases.", this.mocker.getComponentUnderTest()
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
        Assert.assertEquals("public", this.mocker.getComponentUnderTest().toString());
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
        when(other.getName()).thenReturn("public", "public", "private", "private");
        AccessLevel edit = mock(AccessLevel.class);
        AccessLevel view = this.mocker.getInstance(AccessLevel.class, "view");
        when(other.getDefaultAccessLevel()).thenReturn(view, edit, view, edit);
        // Equals another visibility with the same name and access level
        Assert.assertTrue(this.mocker.getComponentUnderTest().equals(other));
        // Doesn't equal a visibility with a different access level but the same name
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals(other));
        // Doesn't equal a visibility with the same access level but a different name
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals(other));
        // Doesn't equal a visibility with a different access level and a different name
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals(other));
        // Doesn't equal other types of objects
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals("edit"));
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
        AccessLevel view = this.mocker.getInstance(AccessLevel.class, "view");
        // Equals another visibility with the same permissiveness
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().compareTo(new MockVisibility("readable", 50, view)));
        // Respects the permissiveness order
        Assert.assertTrue(this.mocker.getComponentUnderTest().compareTo(new MockVisibility("hidden", 0, other)) > 0);
        Assert.assertTrue(this.mocker.getComponentUnderTest().compareTo(new MockVisibility("open", 100, other)) < 0);
        // Other types of visibilities are placed after
        Assert.assertTrue(this.mocker.getComponentUnderTest().compareTo(mock(Visibility.class)) < 0);
    }

    /** Basic tests for {@link Visibility#hashCode()}. */
    @Test
    public void hashCodeTest() throws ComponentLookupException
    {
        Visibility edit = this.mocker.getComponentUnderTest();
        AccessLevel none = mock(AccessLevel.class);
        AccessLevel view = this.mocker.getInstance(AccessLevel.class, "view");
        Visibility other = new MockVisibility("public", 120, view);
        // Same hashcode for a different access level with the same name and assignable flag, ignoring permissiveness
        Assert.assertEquals(edit.hashCode(), other.hashCode());
        // Different hashcodes for different coordinates
        other = new MockVisibility("public", 50, none);
        Assert.assertNotEquals(edit.hashCode(), other.hashCode());
        other = new MockVisibility("private", 50, view);
        Assert.assertNotEquals(edit.hashCode(), other.hashCode());
        other = new MockVisibility("private", 50, none);
        Assert.assertNotEquals(edit.hashCode(), other.hashCode());
    }
}
