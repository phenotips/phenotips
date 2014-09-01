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
package org.phenotips.groups.internal.listeners;

import org.phenotips.groups.Group;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Event listener that sets up newly created PhenoTips groups, by also creating the corresponding "Group Administrators"
 * group, and setting up access rights.
 *
 * @version $Id$
 */
@Component
@Named("phenotips-group-setup")
@Singleton
public class GroupSetupEventListener implements EventListener
{
    /** The space where XWiki's system classes are stored. */
    private static final EntityReference SYSTEM_SPACE_REFERENCE = new EntityReference(XWiki.SYSTEM_SPACE,
        EntityType.SPACE);

    /** The XClass used for defining groups in XWiki. */
    private static final EntityReference GROUP_CLASS_REFERENCE = new EntityReference("XWikiGroups",
        EntityType.DOCUMENT, SYSTEM_SPACE_REFERENCE);

    /** The XClass used for setting document access rights in XWiki. */
    private static final EntityReference RIGHTS_CLASS_REFERENCE = new EntityReference("XWikiRights",
        EntityType.DOCUMENT, SYSTEM_SPACE_REFERENCE);

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the data. */
    @Inject
    private DocumentAccessBridge dab;

    @Override
    public String getName()
    {
        return "phenotips-group-setup";
    }

    @Override
    public List<Event> getEvents()
    {
        return Collections.<Event>singletonList(new DocumentCreatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // FIXME: Use components to access the context and the current document
        XWikiDocument doc = (XWikiDocument) source;
        if (doc.getXObject(Group.CLASS_REFERENCE) == null) {
            return;
        }
        DocumentReference docReference = doc.getDocumentReference();
        if ("PhenoTipsGroupTemplate".equals(docReference.getName())) {
            return;
        }
        XWikiContext context = (XWikiContext) data;
        DocumentReference adminsReference =
            new DocumentReference(docReference.getName() + " Administrators", docReference.getLastSpaceReference());
        XWiki xwiki = context.getWiki();
        try {
            // Create the administrative group
            XWikiDocument adminsDoc = xwiki.getDocument(adminsReference, context);
            setMembers(this.dab.getCurrentUserReference(), adminsDoc, context);
            setRights(adminsReference, adminsDoc, context);
            xwiki.saveDocument(adminsDoc, "Automatically created administrative group", true, context);

            // Setup the new group
            setMembers(adminsReference, doc, context);
            setRights(adminsReference, doc, context);
            // This is a pre-save notification, the document will be saved afterwards
        } catch (XWikiException ex) {
            this.logger.error("Failed to set up the new group [{}]: {}", docReference, ex.getMessage(), ex);
        }
    }

    private void setMembers(DocumentReference member, XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        BaseObject memberObject = doc.newXObject(GROUP_CLASS_REFERENCE, context);
        memberObject.setStringValue("member", member.toString());
    }

    private void setRights(DocumentReference editor, XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        BaseObject rights = doc.newXObject(RIGHTS_CLASS_REFERENCE, context);
        rights.setIntValue("allow", 1);
        rights.setStringValue("levels", "edit");
        rights.setLargeStringValue("groups", editor.toString());
    }
}
