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
package org.phenotips.tools.listeners;

import org.phenotips.data.Patient;
import org.phenotips.ontology.OntologyManager;

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.EventFilter;
import org.xwiki.observation.event.filter.RegexEventFilter;

import java.util.Arrays;
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
 * Wrong DOC
 *
 * @version $Id$
 */
@Component
@Named("phenotype-saver")
@Singleton
public class PhenotypeSaveListener implements EventListener
{
    /** The XClass used for storing patient data. */
    EntityReference CLASS_REFERENCE = Patient.CLASS_REFERENCE;

    @Inject
    OntologyManager ontologyManager;

    @Inject
    Execution execution;

    @Override
    public String getName()
    {
        return "phenotype-saver";
    }

    @Override
    public List<Event> getEvents()
    {
        // The list of events this listener listens to
        EventFilter eventFilter = new RegexEventFilter("xwiki:data\\.P[0-9]+");
        return Arrays.<Event>asList(new DocumentUpdatingEvent(eventFilter), new DocumentCreatingEvent(eventFilter));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        //Most likely missing some sources of HPO terms
        Set<String> fieldsToFix = new HashSet<String>();
        fieldsToFix.add("extended_prenatal_phenotype");
        fieldsToFix.add("extended_negative_prenatal_phenotype");
        fieldsToFix.add("prenatal_phenotype");
        fieldsToFix.add("negative_prenatal_phenotype");
        fieldsToFix.add("extended_phenotype");
        fieldsToFix.add("extended_negative_phenotype");
        fieldsToFix.add("phenotype");
        fieldsToFix.add("negative_phenotype");

        XWikiDocument doc = (XWikiDocument) source;
        BaseObject patientObject = doc.getXObject(CLASS_REFERENCE);

        if (patientObject != null) {
            XWikiContext context = (XWikiContext) execution.getContext().getProperty("xwikicontext");
            for (String field : fieldsToFix) {
                DBStringListProperty currentTermList = (DBStringListProperty) patientObject.getField(field);
                List<String> terms = currentTermList.getList();
                Set<String> correctSet = new LinkedHashSet<String>();
                for (String term : terms) {
                    correctSet.add(ontologyManager.resolveTerm(term).getId());
                }
                List<String> correctList = new LinkedList<String>();
                correctList.addAll(correctSet);
                patientObject.set(field, correctList, context);
            }
        }
    }
}
