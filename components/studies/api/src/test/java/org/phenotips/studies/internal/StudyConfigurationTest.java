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
package org.phenotips.studies.internal;

import org.xwiki.component.manager.ComponentLookupException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Tests for {@link StudyConfiguration}.
 *
 * @version $Id$
 */
public class StudyConfigurationTest
{
    /** {@link StudyConfiguration#getSectionsOverride()} returns the list specified in the object. */
    @Test
    public void getSectionsOverride() throws ComponentLookupException, XWikiException
    {
        BaseObject o = Mockito.mock(BaseObject.class);
        List<String> sections = Collections.singletonList("patient_info");
        Mockito.when(o.getListValue("sections")).thenReturn(sections);

        StudyConfiguration result = new StudyConfiguration(o);
        Assert.assertSame(sections, result.getSectionsOverride());
    }

    /** {@link StudyConfiguration#getFieldsOverride()} returns the list specified in the object. */
    @Test
    public void getFieldsOverride() throws ComponentLookupException, XWikiException
    {
        BaseObject o = Mockito.mock(BaseObject.class);
        List<String> fields = new LinkedList<>();
        fields.add("external_id");
        fields.add("gender");
        fields.add("name");
        fields.add("phenotype");
        Mockito.when(o.getListValue("fields")).thenReturn(fields);

        StudyConfiguration result = new StudyConfiguration(o);
        Assert.assertSame(fields, result.getFieldsOverride());
    }

    /** {@link StudyConfiguration#getPhenotypeMapping()} returns the list specified in the object. */
    @Test
    public void getPhenotypeMapping() throws ComponentLookupException, XWikiException
    {
        BaseObject o = Mockito.mock(BaseObject.class);
        Mockito.when(o.getStringValue("mapping")).thenReturn("PhenoTips.XPhenotypeMapping");

        StudyConfiguration result = new StudyConfiguration(o);
        Assert.assertEquals("PhenoTips.XPhenotypeMapping", result.getPhenotypeMapping());
    }
}
