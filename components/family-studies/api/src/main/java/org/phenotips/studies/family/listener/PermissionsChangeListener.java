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
package org.phenotips.studies.family.listener;

import org.phenotips.data.events.PatientChangedEvent;
import org.phenotips.data.events.PatientDeletedEvent;
import org.phenotips.studies.family.FamilyUtils;
import org.phenotips.studies.family.Processing;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * When a patient record's permissions are changed or a patient record is deleted, if that patient belongs to a family,
 * the family's permissions should change also.
 *
 * @version $Id$
 * @since 1.0M
 */
@Component
@Named("family-studies-permissions-listener")
@Singleton
public class PermissionsChangeListener extends AbstractEventListener
{
    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    @Inject
    private FamilyUtils utils;

    @Inject
    private Processing processingUtils;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public PermissionsChangeListener()
    {
        super("family-studies-permissions-listener", new PatientChangedEvent(), new PatientDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        try {
            XWikiDocument familyDoc = utils.getFamilyDoc(doc);
            if (familyDoc != null) {
                List<String> members = utils.getFamilyMembers(familyDoc);
                if (event instanceof PatientDeletedEvent) {
                    members.remove(doc.getDocumentReference().getName());
                    utils.setFamilyMembers(familyDoc, members);
                }
                processingUtils.setUnionOfUserPermissions(familyDoc, members);
                // todo. delete family doc if no members left
                context.getWiki().saveDocument(familyDoc, context);
            }
        } catch (XWikiException ex) {
            logger.warn("Could not update family permissions");
        }
    }
}

