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
package org.phenotips.vocabulary.translation;

import org.phenotips.xliff12.XLIFFResourceBundleControl;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link XLIFFResourceBundleControl}.
 *
 * @version $Id$
 */
public class XLIFFResourceBundleTest
{
    @Test
    public void testRead()
    {
        ResourceBundle baseBundle = ResourceBundle.getBundle("test", Locale.ROOT, XLIFFResourceBundleControl.INSTANCE);
        ResourceBundle enBundle = ResourceBundle.getBundle("test", Locale.ENGLISH, XLIFFResourceBundleControl.INSTANCE);
        ResourceBundle usBundle = ResourceBundle.getBundle("test", Locale.US, XLIFFResourceBundleControl.INSTANCE);
        ResourceBundle udBundle =
            ResourceBundle.getBundle("test", new Locale("en", "US", "UD"), XLIFFResourceBundleControl.INSTANCE);

        // Basic override chain
        assertEquals("Dummy 1", baseBundle.getString("DUM_0001"));
        assertEquals("The Dummy 1", enBundle.getString("DUM_0001"));
        assertEquals("That Dummy 1, yo!", usBundle.getString("DUM_0001"));
        assertEquals("\u295D \u028E\u026F\u026Fn\u15E1", udBundle.getString("DUM_0001"));

        // Fallback to parent when requesting a missing translation
        assertEquals("Dummy 5", enBundle.getString("DUM_0005"));
        assertEquals("The Dummy 4", usBundle.getString("DUM_0004"));
        assertEquals("That Dummy 2, yo!", udBundle.getString("DUM_0002"));

        // Fallback to ancestors when requesting a missing translation
        assertEquals("Dummy 5", usBundle.getString("DUM_0005"));
        assertEquals("The Dummy 4", udBundle.getString("DUM_0004"));
        assertEquals("Dummy 5", udBundle.getString("DUM_0005"));

        // It's OK to add new translations
        assertEquals("That Dummy 7, yo!", usBundle.getString("DUM_0007"));
        assertEquals("That Dummy 7, yo!", udBundle.getString("DUM_0007"));
    }

    @Test(expected = MissingResourceException.class)
    public void missingTranslationThrowsException()
    {
        ResourceBundle baseBundle = ResourceBundle.getBundle("test", Locale.ROOT, XLIFFResourceBundleControl.INSTANCE);
        baseBundle.getString("DUM_0010");
    }

    @Test
    public void nestedElementsAreSupported()
    {
        ResourceBundle baseBundle = ResourceBundle.getBundle("test", Locale.ROOT, XLIFFResourceBundleControl.INSTANCE);
        assertEquals("Dummy 6", baseBundle.getString("DUM_0006"));
    }

    @Test
    public void placeholdersAreIgnored()
    {
        ResourceBundle baseBundle = ResourceBundle.getBundle("test", Locale.ROOT, XLIFFResourceBundleControl.INSTANCE);
        assertEquals("Dummy 90", baseBundle.getString("DUM_0090"));
    }

    @Test
    public void requestingMissingTranslationReturnsAncestorTranslation()
    {
        ResourceBundle frBundle = ResourceBundle.getBundle("test", Locale.FRENCH, XLIFFResourceBundleControl.INSTANCE);
        assertEquals("Dummy 1", frBundle.getString("DUM_0001"));

        ResourceBundle ukBundle = ResourceBundle.getBundle("test", Locale.UK, XLIFFResourceBundleControl.INSTANCE);
        assertEquals("The Dummy 1", ukBundle.getString("DUM_0001"));
    }

    @Test
    public void requestingBundleWithoutBase()
    {
        ResourceBundle usBundle = ResourceBundle.getBundle("baseless", Locale.US, XLIFFResourceBundleControl.INSTANCE);
        assertEquals("Dummy 1", usBundle.getString("DUM_0001"));
    }

    @Test(expected = MissingResourceException.class)
    public void requestingMissingBundleThrowsException()
    {
        ResourceBundle.getBundle("missing", Locale.ROOT, XLIFFResourceBundleControl.INSTANCE);
    }
}
