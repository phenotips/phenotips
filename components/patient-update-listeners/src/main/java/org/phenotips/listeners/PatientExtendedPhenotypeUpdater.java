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
package org.phenotips.listeners;

import org.phenotips.Constants;
import org.phenotips.ontology.OntologyManager;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Update the extended_*_phenotype aggregated properties whenever the phenotypes change.
 *
 * @version $Id$
 */
@Component
@Named("patient-extended-phenotype-updater")
@Singleton
public class PatientExtendedPhenotypeUpdater implements EventListener, Initializable
{
    /**
     * Needed for accessing the HPO ontology though indirect means.
     */
    @Inject
    private OntologyManager ontologyManager;

    @Override
    public void initialize() throws InitializationException
    {
    }

    @Override
    public String getName()
    {
        return "patient-extended-phenotype-updater";
    }

    @Override
    public List<Event> getEvents()
    {
        // The list of events this listener listens to
        return Arrays.<Event>asList(new DocumentCreatingEvent(), new DocumentUpdatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;

        BaseObject patientRecordObj =
            doc.getXObject(new DocumentReference(doc.getDocumentReference().getRoot().getName(),
                Constants.CODE_SPACE, "PatientClass"));
        if (patientRecordObj == null) {
            return;
        }
        updateField("phenotype", "extended_phenotype", patientRecordObj);
        updateField("prenatal_phenotype", "extended_prenatal_phenotype", patientRecordObj);
        updateField("negative_phenotype", "extended_negative_phenotype", patientRecordObj);
    }

    /**
     * Fills in an extended phenotype field using the base phenotype field.
     *
     * @param baseFieldName the name of the field holding the basic list of phenotypes
     * @param extendedFieldName the name of the target field that will hold the extended list of phenotypes
     * @param patientRecordObj the object to update
     */
    private void updateField(String baseFieldName, String extendedFieldName, BaseObject patientRecordObj)
    {
        @SuppressWarnings("unchecked")
        List<String> phenotypes = patientRecordObj.getListValue(baseFieldName);
        Set<String> extendedPhenotypes = new TreeSet<String>();
        Set<String> sortedPhenotypes = new TreeSet<String>();
        for (String phenotype : phenotypes) {
            sortedPhenotypes.add(phenotype);
            OntologyTerm phenotypeTerm = this.ontologyManager.resolveTerm(phenotype);
            if (phenotypeTerm != null) {
                for (OntologyTerm term : phenotypeTerm.getAncestorsAndSelf()) {
                    extendedPhenotypes.add(term.getId());
                }
            } else {
                extendedPhenotypes.add(phenotype);
            }
        }
        patientRecordObj.setDBStringListValue(extendedFieldName, new ArrayList<String>(extendedPhenotypes));
        patientRecordObj.setDBStringListValue(baseFieldName, new ArrayList<String>(sortedPhenotypes));
    }
}
