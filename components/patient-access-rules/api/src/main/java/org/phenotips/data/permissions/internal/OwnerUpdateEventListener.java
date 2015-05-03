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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.phenotips.data.permissions.internal;

import org.phenotips.data.events.PatientCreatingEvent;
import org.phenotips.data.permissions.Owner;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * This listener creates a {@code PhenoTips.OwnerClass} object when a new patient is created, with the creator set as
 * the owner.
 *
 * @version $Id$
 * @since 1.0M13
 */
@Component
@Named("phenotips-patient-owner-updater")
@Singleton
public class OwnerUpdateEventListener extends AbstractEventListener
{
    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public OwnerUpdateEventListener()
    {
        super("phenotips-patient-owner-updater", new PatientCreatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        try {
            BaseObject ownerObject = doc.newXObject(Owner.CLASS_REFERENCE, context);
            if (doc.getCreatorReference() != null) {
                ownerObject.setStringValue("owner", doc.getCreatorReference().toString());
            } else {
                ownerObject.setStringValue("owner", "");
            }
        } catch (XWikiException ex) {
            this.logger.error("Failed to set the initial owner for patient [{}]: {}", doc.getDocumentReference(),
                ex.getMessage(), ex);
        }
    }
}
