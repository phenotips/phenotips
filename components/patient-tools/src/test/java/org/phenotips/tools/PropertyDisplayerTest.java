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
package org.phenotips.tools;

import org.phenotips.ontology.OntologyService;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.xml.XMLUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;

/**
 * Tests for the {@link PropertyDisplayer}.
 *
 * @version $Id$
 */
public class PropertyDisplayerTest
{
    /** For parsing the generated HTML code into navigable DOM trees. */
    private DOMImplementationLS domls;

    /** Instantiates the {@link #domls DOM parser}. */
    public PropertyDisplayerTest() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        this.domls = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS 3.0");
    }

    /** Displaying phenotypes in edit mode should output hidden empty values which allow unselecting all values. */
    @Test
    public void testDisplayOutputsHiddenEmptyValues()
    {
        FormData configuration = new FormData();
        configuration.setMode(DisplayMode.Edit);
        com.xpn.xwiki.api.Document doc = Mockito.mock(com.xpn.xwiki.api.Document.class);
        configuration.setDocument(doc);
        Mockito.when(doc.getObjects("PhenoTips.PhenotypeMetaClass")).thenReturn(new Vector<com.xpn.xwiki.api.Object>());
        configuration.setPositiveFieldName("PhenoTips.PatientClass_0_phenotype");
        Collection<Map<String, ?>> data = Collections.emptySet();
        OntologyService ontologyService = Mockito.mock(OntologyService.class);
        Mockito.doReturn(new HashSet<OntologyTerm>()).when(ontologyService)
            .search(Matchers.anyMapOf(String.class, Object.class));

        PropertyDisplayer displayer = new PropertyDisplayer(data, configuration, ontologyService);
        String output = displayer.display();
        Assert.assertTrue(StringUtils.isNotBlank(output));
        LSInput input = this.domls.createLSInput();
        input.setStringData("<div>" + output + "</div>");
        Document xmldoc = XMLUtils.parse(input);
        NodeList inputs = xmldoc.getElementsByTagName("input");
        Assert.assertEquals(1, inputs.getLength());
        Element positive = (Element) inputs.item(0);
        Assert.assertEquals("PhenoTips.PatientClass_0_phenotype", positive.getAttribute("name"));
        Assert.assertEquals("", positive.getAttribute("value"));
        Assert.assertEquals("hidden", positive.getAttribute("type"));

        configuration.setNegativeFieldName("PhenoTips.PatientClass_0_negative_phenotype");
        displayer = new PropertyDisplayer(data, configuration, ontologyService);
        output = displayer.display();
        Assert.assertTrue(StringUtils.isNotBlank(output));
        input = this.domls.createLSInput();
        input.setStringData("<div>" + output + "</div>");
        xmldoc = XMLUtils.parse(input);
        inputs = xmldoc.getElementsByTagName("input");
        Assert.assertEquals(2, inputs.getLength());
        positive = (Element) inputs.item(0);
        Assert.assertEquals("PhenoTips.PatientClass_0_phenotype", positive.getAttribute("name"));
        Assert.assertEquals("", positive.getAttribute("value"));
        Assert.assertEquals("hidden", positive.getAttribute("type"));
        Element negative = (Element) inputs.item(1);
        Assert.assertEquals("PhenoTips.PatientClass_0_negative_phenotype", negative.getAttribute("name"));
        Assert.assertEquals("", negative.getAttribute("value"));
        Assert.assertEquals("hidden", negative.getAttribute("type"));

        configuration.setMode(DisplayMode.View);
        displayer = new PropertyDisplayer(data, configuration, ontologyService);
        output = displayer.display();
        Assert.assertTrue(StringUtils.isBlank(output));
    }
}
