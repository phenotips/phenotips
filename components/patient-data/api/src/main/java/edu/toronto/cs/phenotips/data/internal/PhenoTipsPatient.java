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
package edu.toronto.cs.phenotips.data.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;

import edu.toronto.cs.phenotips.data.Patient;
import edu.toronto.cs.phenotips.data.Phenotype;

/**
 * Implementation of patient data based on the XWiki data model, where patient data is represented by properties in
 * objects of type {@code PhenoTips.PatientClass}.
 * 
 * @version $Id$
 */
public class PhenoTipsPatient implements Patient
{
    /** Known phenotype properties. */
    private static final String[] PHENOTYPE_PROPERTIES = new String[] {"phenotype", "negative_phenotype"};

    /** @see #getDocument() */
    private DocumentReference document;

    /** @see #getReporter() */
    private DocumentReference reporter;

    /** @see #getPhenotypes() */
    private Set<Phenotype> phenotypes = new HashSet<Phenotype>();

    /**
     * Constructor that copies the data from an XDocument.
     * 
     * @param doc the XDocument representing this patient in XWiki
     */
    public PhenoTipsPatient(XWikiDocument doc)
    {
        this.document = doc.getDocumentReference();
        this.reporter = doc.getCreatorReference();
        BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
        if (data == null) {
            return;
        }
        for (String property : PHENOTYPE_PROPERTIES) {
            try {
                DBStringListProperty values = (DBStringListProperty) data.get(property);
                if (values == null) {
                    continue;
                }
                for (String value : values.getList()) {
                    this.phenotypes.add(new PhenoTipsPhenotype(doc, values, value));
                }
            } catch (XWikiException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        // Readonly from now on
        this.phenotypes = Collections.unmodifiableSet(this.phenotypes);
    }

    @Override
    public DocumentReference getDocument()
    {
        return this.document;
    }

    @Override
    public DocumentReference getReporter()
    {
        return this.reporter;
    }

    @Override
    public Set<Phenotype> getPhenotypes()
    {
        return this.phenotypes;
    }

    @Override
    public String toString()
    {
        return toJSON().toString(2);
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();
        result.element("id", getDocument().getName());
        result.element("reporter", getReporter().getName());
        if (!this.phenotypes.isEmpty()) {
            JSONArray features = new JSONArray();
            for (Phenotype phenotype : this.phenotypes) {
                features.add(phenotype.toJSON());
            }
            result.element("features", features);
        }
        return result;
    }
}
