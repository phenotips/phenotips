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

import org.phenotips.data.Patient;
import org.phenotips.data.events.PatientChangingEvent;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import java.util.ArrayList;
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
public class PatientExtendedPhenotypeUpdater extends AbstractEventListener
{
    /** Needed for accessing the feature ontologies. */
    @Inject
    private VocabularyManager vocabularyManager;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public PatientExtendedPhenotypeUpdater()
    {
        super("patient-extended-phenotype-updater", new PatientChangingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;

        BaseObject patientRecordObj = doc.getXObject(Patient.CLASS_REFERENCE);
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
            VocabularyTerm phenotypeTerm = this.vocabularyManager.resolveTerm(phenotype);
            if (phenotypeTerm != null) {
                for (VocabularyTerm term : phenotypeTerm.getAncestorsAndSelf()) {
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
