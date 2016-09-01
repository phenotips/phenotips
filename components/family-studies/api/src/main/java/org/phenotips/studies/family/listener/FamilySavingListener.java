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

import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.internal.PhenotipsFamily;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Adds a proband to the FamilyClass on family save event.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("familySavingListener")
@Singleton
public class FamilySavingListener extends AbstractEventListener
{
    /**
     * Default constructor, sets up the listener name and the list of events to subscribe to.
     */
    public FamilySavingListener()
    {
        super("familySavingListener", new DocumentCreatedEvent(), new DocumentUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        if (document == null) {
            return;
        }
        BaseObject familyClassObject = document.getXObject(Family.CLASS_REFERENCE);
        if (familyClassObject == null) {
            return;
        }
        Family family = new PhenotipsFamily(document);
        String probandId = family.getProbandId();
        familyClassObject.setStringValue("proband_id", (probandId == null) ? "" : probandId);
    }
}
