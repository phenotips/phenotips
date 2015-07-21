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
import org.xwiki.context.Execution;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;

/**
 * Listens for patient records being changed, and before the action takes place, iterates over all the HPO terms and
 * replaces the deprecated ones with their updated ID.
 *
 * @version $Id$
 */
@Component
@Named("deprecated-phenotype-updater")
@Singleton
public class DeprecatedPhenotypeUpdaterEventListener extends AbstractEventListener
{
    @Inject
    private VocabularyManager vocabularyManager;

    @Inject
    private Execution execution;

    /** The list of field names which might contain deprecated terms. */
    private final Set<String> fieldsToFix;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public DeprecatedPhenotypeUpdaterEventListener()
    {
        super("deprecated-phenotype-updater", new PatientChangingEvent());

        this.fieldsToFix = new HashSet<String>();
        this.fieldsToFix.add("phenotype");
        this.fieldsToFix.add("extended_phenotype");
        this.fieldsToFix.add("negative_phenotype");
        this.fieldsToFix.add("extended_negative_phenotype");
        this.fieldsToFix.add("prenatal_phenotype");
        this.fieldsToFix.add("extended_prenatal_phenotype");
        this.fieldsToFix.add("negative_prenatal_phenotype");
        this.fieldsToFix.add("extended_negative_prenatal_phenotype");
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        BaseObject patientObject = doc.getXObject(Patient.CLASS_REFERENCE);

        if (patientObject != null) {
            XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
            for (String field : this.fieldsToFix) {
                DBStringListProperty currentTermList = (DBStringListProperty) patientObject.getField(field);
                if (currentTermList == null) {
                    continue;
                }
                List<String> terms = currentTermList.getList();
                Set<String> correctSet = new LinkedHashSet<String>();
                for (String term : terms) {
                    VocabularyTerm properTerm = this.vocabularyManager.resolveTerm(term);
                    if (properTerm != null) {
                        correctSet.add(properTerm.getId());
                    } else {
                        correctSet.add(term);
                    }
                }
                List<String> correctList = new LinkedList<String>();
                correctList.addAll(correctSet);
                patientObject.set(field, correctList, context);
            }
        }
    }
}
