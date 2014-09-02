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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.Owner;

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import java.util.Collections;
import java.util.List;

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
public class OwnerUpdateEventListener implements EventListener
{
    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return "phenotips-patient-owner-updater";
    }

    @Override
    public List<Event> getEvents()
    {
        return Collections.<Event>singletonList(new DocumentCreatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;
        if (isPatient(doc)) {
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

    private boolean isPatient(XWikiDocument doc)
    {
        return (doc.getXObject(Patient.CLASS_REFERENCE) != null)
            && !"PatientTemplate".equals(doc.getDocumentReference().getName());
    }
}
