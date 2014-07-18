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
package org.phenotips.data.internal.controller;

import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.component.annotation.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Handles the patients genes.
 */
@Component(roles = {PatientDataController.class})
@Named("gene")
@Singleton
public class GeneController extends AbstractComplexController<PatientData<String>>
{
    @Override
    public String getName()
    {
        return "genes";
    }

    @Override
    protected String getJsonPropertyName()
    {
        return "genes";
    }

    @Override
    protected List<String> getProperties()
    {
        return Arrays.asList("gene", "comments");
    }

    @Override
    protected List<String> getBooleanFields()
    {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getCodeFields()
    {
        return Collections.emptyList();
    }

    @Override
    public PatientData<PatientData<String>> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            List<BaseObject> geneXWikiObjects = doc.getXObjects(Patient.GENE_CLASS_REFERENCE);
            if (geneXWikiObjects == null) {
                throw new NullPointerException("The patient does not have any gene information");
            }

            List<PatientData<String>> allGenes = new LinkedList<PatientData<String>>();
            for (BaseObject geneObject : geneXWikiObjects) {
                Map<String, String> singleGene = new HashMap<String, String>();
                for (String property : getProperties()) {
                    BaseProperty field = (BaseProperty) geneObject.getField(property);
                    if (field != null) {
                        singleGene.put(property, (String) field.getValue());
                    }
                }
                /* The DictionaryPatientData does not need a name, as it is used solely as a Map */
                allGenes.add(new DictionaryPatientData<String>("", singleGene));
            }
            return new IndexedPatientData<PatientData<String>>(getName(), allGenes);
        } catch (Exception e) {
            super.logger.error(
                "Could not find requested document or some unforeseen error has occurred during controller loading");
        }
        return null;
    }
}
