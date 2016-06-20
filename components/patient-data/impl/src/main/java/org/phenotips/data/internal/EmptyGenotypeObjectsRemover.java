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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;

/**
 * Removes gene and variant objects from the document if key fields are empty, gene field for genes and cdna field for
 * variants.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Named("empty-genotype-objects-remover")
@Singleton
public class EmptyGenotypeObjectsRemover extends AbstractEventListener
{
    private static final EntityReference GENE_CLASS_REFERENCE = new EntityReference("GeneClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final EntityReference VARIANT_CLASS_REFERENCE = new EntityReference("GeneVariantClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String GENE_KEY = "gene";

    private static final String VARIANT_KEY = "cdna";

    private static Map<String, EntityReference> refs = new LinkedHashMap<String, EntityReference>();

    /**
     * Default constructor, sets up the listener name and the list of events to subscribe to.
     */
    public EmptyGenotypeObjectsRemover()
    {
        super("empty-genotype-objects-remover", new PatientChangingEvent());
        refs.put(GENE_KEY, GENE_CLASS_REFERENCE);
        refs.put(VARIANT_KEY, VARIANT_CLASS_REFERENCE);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;

        for (String key : refs.keySet()) {
            List<BaseObject> xWikiObjects = doc.getXObjects(refs.get(key));
            if (xWikiObjects == null || xWikiObjects.isEmpty()) {
                continue;
            }
            for (BaseObject object : xWikiObjects) {
                if (object == null) {
                    continue;
                }
                BaseStringProperty field = (BaseStringProperty) object.getField(key);
                if (field == null || StringUtils.isEmpty(field.getValue())) {
                    doc.removeXObject(object);
                }
            }
        }
    }
}
