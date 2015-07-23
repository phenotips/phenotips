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
package org.phenotips.data.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Disorder;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.diff.DiffManager;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.web.Utils;

import net.sf.json.JSONObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PhenoTipsDisorderTest
{
    @Mock
    private ComponentManager cm;

    @Mock
    private Provider<ComponentManager> mockProvider;

    @Mock
    private VocabularyManager vm;

    @Mock
    private VocabularyTerm mim200100;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        Utils.setComponentManager(this.cm);
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", this.mockProvider);
        when(this.mockProvider.get()).thenReturn(this.cm);
        when(this.cm.getInstance(DiffManager.class)).thenReturn(null);
        when(this.cm.getInstance(VocabularyManager.class)).thenReturn(this.vm);

        when(this.mim200100.getId()).thenReturn("MIM:200100");
        when(this.mim200100.getName()).thenReturn("#200100 ABETALIPOPROTEINEMIA");
        when(this.vm.resolveTerm("MIM:200100")).thenReturn(this.mim200100);
    }

    @Test
    public void testNormalBehavior() throws XWikiException
    {
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("omim_id");

        Disorder d = new PhenoTipsDisorder(prop, "200100");

        Assert.assertEquals("MIM:200100", d.getId());
        Assert.assertEquals("#200100 ABETALIPOPROTEINEMIA", d.getName());
        Assert.assertEquals("200100", d.getValue());

        // Testing the JSON format
        JSONObject json = d.toJSON();

        Assert.assertEquals("MIM:200100", json.getString("id"));
        Assert.assertEquals("#200100 ABETALIPOPROTEINEMIA", json.getString("label"));
    }

    @Test
    public void customTermsOnlyUseTheLabel() throws XWikiException
    {
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("omim_id");

        Disorder d = new PhenoTipsDisorder(prop, "Sickness");

        Assert.assertEquals("", d.getId());
        Assert.assertEquals("Sickness", d.getName());
        Assert.assertEquals("Sickness", d.getValue());

        // Testing the JSON format
        JSONObject json = d.toJSON();

        Assert.assertFalse(json.has("id"));
        Assert.assertEquals("Sickness", json.getString("label"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullValueThrowsError()
    {
        ListProperty prop = mock(ListProperty.class);
        when(prop.getName()).thenReturn("omim_id");

        new PhenoTipsDisorder(prop, null);
    }
}
