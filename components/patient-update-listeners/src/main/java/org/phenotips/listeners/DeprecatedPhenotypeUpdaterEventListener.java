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

import org.phenotips.data.Patient;
import org.phenotips.ontology.OntologyManager;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;

/**
 * Listens for document being created or updated, and before the action takes place, iterates over all the HPO terms and
 * replaces the deprecated ones.
 *
 * @version $Id$
 */
@Component
@Named("deprecated-phenotype-updater")
@Singleton
public class DeprecatedPhenotypeUpdaterEventListener extends AbstractEventListener implements EventListener
{
    @Inject
    private OntologyManager ontologyManager;

    @Inject
    private Execution execution;

    /** The list of field names which might contain deprecated terms. */
    private final Set<String> fieldsToFix;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public DeprecatedPhenotypeUpdaterEventListener()
    {
        super("deprecated-phenotype-updater", new DocumentCreatingEvent(), new DocumentUpdatingEvent());

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

        if (patientObject != null && !StringUtils.equals("PatientTemplate", doc.getDocumentReference().getName())) {
            XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
            for (String field : this.fieldsToFix) {
                DBStringListProperty currentTermList = (DBStringListProperty) patientObject.getField(field);
                if (currentTermList == null) {
                    continue;
                }
                List<String> terms = currentTermList.getList();
                Set<String> correctSet = new LinkedHashSet<String>();
                for (String term : terms) {
                    OntologyTerm properTerm = this.ontologyManager.resolveTerm(term);
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
