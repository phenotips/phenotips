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
package org.phenotips.data.internal;

import org.phenotips.Constants;
import org.phenotips.data.events.PatientChangingEvent;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import java.util.List;
import java.util.Objects;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Cleans up leading and trailing spaces and ensures the prefix "c." is lowercase in variants' cDNA field.
 *
 * @version $Id$
 * @since 1.4.5
 */
@Component
@Named("cdna-cleanup-event-listener")
@Singleton
public class VariantCdnaCleanupEventListener extends AbstractEventListener
{
    private static final EntityReference VARIANT_CLASS_REFERENCE = new EntityReference("GeneVariantClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String VARIANT_KEY = "cdna";

    /**
     * Default constructor, sets up the listener name and the list of events to subscribe to.
     */
    public VariantCdnaCleanupEventListener()
    {
        super("cdna-cleanup-event-listener", new PatientChangingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        List<BaseObject> variantXWikiObjects = doc.getXObjects(VARIANT_CLASS_REFERENCE);

        if (variantXWikiObjects == null || variantXWikiObjects.isEmpty()) {
            return;
        }

        variantXWikiObjects.stream().filter(Objects::nonNull)
            .filter(variant -> !StringUtils.isBlank(variant.getStringValue(VARIANT_KEY)))
            .forEach(variant -> {
                String cdna = variant.getStringValue(VARIANT_KEY).trim().replaceFirst("^C\\.", "c.");
                variant.setStringValue(VARIANT_KEY, cdna);
            });
    }
}
