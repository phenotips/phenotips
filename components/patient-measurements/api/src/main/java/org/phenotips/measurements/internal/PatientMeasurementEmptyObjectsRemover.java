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
package org.phenotips.measurements.internal;

import org.phenotips.Constants;
import org.phenotips.data.events.PatientChangingEvent;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Removes measurements which do not contain the set of minimally useful data, i.e. value and one of [date, age].
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Component
@Named("empty-measurement-objects-remover")
@Singleton
public class PatientMeasurementEmptyObjectsRemover extends AbstractEventListener
{
    private static final EntityReference MEASUREMENT_CLASS_REFERENCE = new EntityReference("MeasurementClass",
            EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    @Inject
    private Execution execution;

    /**
     * Default constructor, sets up the listener name and the list of events to subscribe to.
     */
    public PatientMeasurementEmptyObjectsRemover()
    {
        super("empty-measurement-objects-remover", new PatientChangingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");

        Boolean changed = false;

        List<BaseObject> xWikiObjects = doc.getXObjects(MEASUREMENT_CLASS_REFERENCE);
        if (xWikiObjects != null && !xWikiObjects.isEmpty()) {
            for (BaseObject object : xWikiObjects) {
                if (object == null) {
                    continue;
                }

                if (objectFieldIsEmpty(object, "value")
                        || (objectFieldIsEmpty(object, "age") && objectFieldIsEmpty(object, "date")))
                {
                    doc.removeXObject(object);
                    changed = true;
                }
            }
        }

        try {
            if (changed) {
                context.getWiki().saveDocument(doc, "Removed empty object", true, context);
            }
        } catch (XWikiException e) {
            // This should not happen;
        }
    }

    /**
     * Check if a given field on the given object is "empty" for our purposes.
     *
     * @param obj object on the document
     * @param field name of the field to check
     * @return true if empty, false otherwise
     */
    private boolean objectFieldIsEmpty(BaseObject obj, String field)
    {
        BaseProperty prop = (BaseProperty) obj.getField(field);
        return (prop == null || prop.getValue() == null || StringUtils.isEmpty(prop.getValue().toString()));
    }
}
