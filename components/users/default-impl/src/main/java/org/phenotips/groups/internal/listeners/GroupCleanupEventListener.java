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
package org.phenotips.groups.internal.listeners;

import org.phenotips.groups.Group;

import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Event listener that also deletes the corresponding "Group Administrators" group when deleting a PhenoTips group.
 *
 * @version $Id$
 */
@Component
@Named("phenotips-group-cleanup")
@Singleton
public class GroupCleanupEventListener implements EventListener
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public String getName()
    {
        return "phenotips-group-cleanup";
    }

    @Override
    public List<Event> getEvents()
    {
        return Collections.<Event>singletonList(new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = ((XWikiDocument) source).getOriginalDocument();
        if (doc == null || doc.getXObject(Group.CLASS_REFERENCE) == null) {
            return;
        }
        XWikiContext context = this.xcontextProvider.get();
        DocumentReference docReference = doc.getDocumentReference();
        DocumentReference adminsReference =
            new DocumentReference(docReference.getName() + " Administrators", docReference.getLastSpaceReference());
        XWiki xwiki = context.getWiki();
        try {
            // Delete the administrative group
            XWikiDocument adminsDoc = xwiki.getDocument(adminsReference, context);
            xwiki.deleteDocument(adminsDoc, context);
        } catch (XWikiException ex) {
            this.logger.error("Failed to delete administrative group for [{}]: {}", docReference, ex.getMessage(), ex);
        }
    }
}
