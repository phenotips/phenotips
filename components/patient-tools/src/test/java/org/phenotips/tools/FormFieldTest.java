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

import org.xwiki.xml.XMLUtils;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;

public class FormFieldTest
{
    private DOMImplementationLS domls;

    public FormFieldTest() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        this.domls = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS 3.0");
    }

    @Test
    public void testDisplayEditWithNothingSelected()
    {
        FormField f = new FormField("HP:0000722", "OCD", "Obsessive-compulsive disorder", "", false, false, false);
        String output = f.display(DisplayMode.Edit, new String[] {"phenotype", "negative_phenotype"});
        Assert.assertNotNull(output);
        LSInput input = this.domls.createLSInput();
        input.setStringData(output);
        Document doc = XMLUtils.parse(input);

        Element e = doc.getDocumentElement();
        Assert.assertEquals("term-entry", e.getAttribute("class"));

        e = (Element) e.getFirstChild();
        Assert.assertEquals("yes-no-picker", e.getAttribute("class"));

        e = (Element) e.getFirstChild();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("na", e.getAttribute("class"));
        Assert.assertEquals("none_HP:0000722", e.getAttribute("for"));

        // NA
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("none", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertEquals("checked", e.getAttribute("checked"));
        Assert.assertEquals("none_HP:0000722", e.getAttribute("id"));

        e = (Element) e.getParentNode().getNextSibling();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("yes", e.getAttribute("class"));
        Assert.assertEquals("phenotype_HP:0000722", e.getAttribute("for"));

        // YES
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("phenotype", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertFalse(e.hasAttribute("checked"));
        Assert.assertEquals("phenotype_HP:0000722", e.getAttribute("id"));
        Assert.assertEquals("Obsessive-compulsive disorder", e.getAttribute("title"));

        e = (Element) e.getParentNode().getNextSibling();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("no", e.getAttribute("class"));
        Assert.assertEquals("negative_phenotype_HP:0000722", e.getAttribute("for"));

        // NO
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("negative_phenotype", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertFalse(e.hasAttribute("checked"));
        Assert.assertEquals("negative_phenotype_HP:0000722", e.getAttribute("id"));
        Assert.assertEquals("Obsessive-compulsive disorder", e.getAttribute("title"));
    }

    @Test
    public void testDisplayEditWithYesSelected()
    {
        FormField f = new FormField("HP:0000722", "OCD", "Obsessive-compulsive disorder", "", false, true, false);
        String output = f.display(DisplayMode.Edit, new String[] {"phenotype", "negative_phenotype"});
        Assert.assertNotNull(output);
        LSInput input = this.domls.createLSInput();
        input.setStringData(output);
        Document doc = XMLUtils.parse(input);

        Element e = doc.getDocumentElement();
        Assert.assertEquals("term-entry", e.getAttribute("class"));

        e = (Element) e.getFirstChild();
        Assert.assertEquals("yes-no-picker", e.getAttribute("class"));

        e = (Element) e.getFirstChild();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("na", e.getAttribute("class"));
        Assert.assertEquals("none_HP:0000722", e.getAttribute("for"));

        // NA
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("none", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertFalse(e.hasAttribute("checked"));
        Assert.assertEquals("none_HP:0000722", e.getAttribute("id"));

        e = (Element) e.getParentNode().getNextSibling();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("yes", e.getAttribute("class"));
        Assert.assertEquals("phenotype_HP:0000722", e.getAttribute("for"));

        // YES
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("phenotype", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertEquals("checked", e.getAttribute("checked"));
        Assert.assertEquals("phenotype_HP:0000722", e.getAttribute("id"));
        Assert.assertEquals("Obsessive-compulsive disorder", e.getAttribute("title"));

        e = (Element) e.getParentNode().getNextSibling();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("no", e.getAttribute("class"));
        Assert.assertEquals("negative_phenotype_HP:0000722", e.getAttribute("for"));

        // NO
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("negative_phenotype", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertFalse(e.hasAttribute("checked"));
        Assert.assertEquals("negative_phenotype_HP:0000722", e.getAttribute("id"));
        Assert.assertEquals("Obsessive-compulsive disorder", e.getAttribute("title"));
    }

    @Test
    public void testDisplayEditWithNoSelected()
    {
        FormField f = new FormField("HP:0000722", "OCD", "Obsessive-compulsive disorder", "", false, false, true);
        String output = f.display(DisplayMode.Edit, new String[] {"phenotype", "negative_phenotype"});
        Assert.assertNotNull(output);
        LSInput input = this.domls.createLSInput();
        input.setStringData(output);
        Document doc = XMLUtils.parse(input);

        Element e = doc.getDocumentElement();
        Assert.assertEquals("term-entry", e.getAttribute("class"));

        e = (Element) e.getFirstChild();
        Assert.assertEquals("yes-no-picker", e.getAttribute("class"));

        e = (Element) e.getFirstChild();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("na", e.getAttribute("class"));
        Assert.assertEquals("none_HP:0000722", e.getAttribute("for"));

        // NA
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("none", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertFalse(e.hasAttribute("checked"));
        Assert.assertEquals("none_HP:0000722", e.getAttribute("id"));

        e = (Element) e.getParentNode().getNextSibling();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("yes", e.getAttribute("class"));
        Assert.assertEquals("phenotype_HP:0000722", e.getAttribute("for"));

        // YES
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("phenotype", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertFalse(e.hasAttribute("checked"));
        Assert.assertEquals("phenotype_HP:0000722", e.getAttribute("id"));
        Assert.assertEquals("Obsessive-compulsive disorder", e.getAttribute("title"));

        e = (Element) e.getParentNode().getNextSibling();
        Assert.assertEquals("label", e.getLocalName());
        Assert.assertEquals("no", e.getAttribute("class"));
        Assert.assertEquals("negative_phenotype_HP:0000722", e.getAttribute("for"));

        // NO
        e = (Element) e.getFirstChild();
        Assert.assertEquals("input", e.getLocalName());
        Assert.assertEquals("checkbox", e.getAttribute("type"));
        Assert.assertEquals("negative_phenotype", e.getAttribute("name"));
        Assert.assertEquals("HP:0000722", e.getAttribute("value"));
        Assert.assertEquals("checked", e.getAttribute("checked"));
        Assert.assertEquals("negative_phenotype_HP:0000722", e.getAttribute("id"));
        Assert.assertEquals("Obsessive-compulsive disorder", e.getAttribute("title"));
    }
}
