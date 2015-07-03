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

import org.phenotips.studies.family.FamilyUtils;
import org.phenotips.studies.family.Processing;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.component.annotation.Component;
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
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Detects the deletion of a family and modifies the members' records accordingly.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component
@Named("familyDeletingListener")
@Singleton
public class FamilyDeletingListener implements EventListener
{
    @Inject
    private Provider<XWikiContext> provider;

    @Inject
    private DocumentAccessBridge dab;

    @Inject
    private FamilyUtils familyUtils;

    @Inject
    private Processing processing;

    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return "familyDeletingListener";
    }

    @Override
    public List<Event> getEvents()
    {
        return Collections.<Event>singletonList(new DocumentDeletingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        if (doc == null || "FamilyTemplate".equals(doc.getDocumentReference().getName())) {
            return;
        }
        XWikiDocument odoc;
        try {
            odoc = (XWikiDocument) this.dab.getDocument(doc.getDocumentReference());
        } catch (Exception e) {
            return;
        }

        BaseObject familyObj = odoc.getXObject(FamilyUtils.FAMILY_CLASS);
        if (familyObj == null) {
            return;
        }

        XWikiContext context = provider.get();
        XWiki wiki = context.getWiki();
        try {
            for (String id : familyUtils.getFamilyMembers(familyObj)) {
                processing.removeMember(id, wiki, context);
            }
        } catch (Exception ex) {
            logger.error("Could not delete patient from a family when deleting the family. {}", ex.getMessage());
        }
    }
}
