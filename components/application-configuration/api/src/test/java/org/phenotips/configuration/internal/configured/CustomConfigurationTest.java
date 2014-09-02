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
package org.phenotips.configuration.internal.configured;

import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.configuration.internal.global.GlobalRecordConfiguration;

import org.xwiki.component.manager.ComponentLookupException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link RecordConfigurationManager} implementation, {@link GlobalRecordConfiguration}.
 *
 * @version $Id$
 */
public class CustomConfigurationTest
{
    /** {@link CustomConfiguration#getSectionsOverride()} returns the list specified in the object. */
    @Test
    public void getSectionsOverride() throws ComponentLookupException, XWikiException
    {
        BaseObject o = mock(BaseObject.class);
        List<String> sections = Collections.singletonList("patient_info");
        when(o.getListValue("sections")).thenReturn(sections);

        CustomConfiguration result = new CustomConfiguration(o);
        Assert.assertSame(sections, result.getSectionsOverride());
    }

    /** {@link CustomConfiguration#getFieldsOverride()} returns the list specified in the object. */
    @Test
    public void getFieldsOverride() throws ComponentLookupException, XWikiException
    {
        BaseObject o = mock(BaseObject.class);
        List<String> fields = new LinkedList<String>();
        fields.add("external_id");
        fields.add("gender");
        fields.add("name");
        fields.add("phenotype");
        when(o.getListValue("fields")).thenReturn(fields);

        CustomConfiguration result = new CustomConfiguration(o);
        Assert.assertSame(fields, result.getFieldsOverride());
    }

    /** {@link CustomConfiguration#getPhenotypeMapping()} returns the list specified in the object. */
    @Test
    public void getPhenotypeMapping() throws ComponentLookupException, XWikiException
    {
        BaseObject o = mock(BaseObject.class);
        when(o.getStringValue("mapping")).thenReturn("PhenoTips.XPhenotypeMapping");

        CustomConfiguration result = new CustomConfiguration(o);
        Assert.assertEquals("PhenoTips.XPhenotypeMapping", result.getPhenotypeMapping());
    }
}
