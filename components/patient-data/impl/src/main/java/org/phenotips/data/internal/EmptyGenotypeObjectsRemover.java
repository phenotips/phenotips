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
import org.phenotips.data.Gene;
import org.phenotips.data.events.PatientChangingEvent;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private static final EntityReference VARIANT_CLASS_REFERENCE = new EntityReference("GeneVariantClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String GENE_KEY = "gene";

    private static final String VARIANT_KEY = "cdna";

    /**
     * Default constructor, sets up the listener name and the list of events to subscribe to.
     */
    public EmptyGenotypeObjectsRemover()
    {
        super("empty-genotype-objects-remover", new PatientChangingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        List<BaseObject> geneXWikiObjects = doc.getXObjects(Gene.GENE_CLASS);
        List<BaseObject> variantXWikiObjects = doc.getXObjects(VARIANT_CLASS_REFERENCE);

        if (geneXWikiObjects == null || geneXWikiObjects.isEmpty()) {
            if (variantXWikiObjects != null && !variantXWikiObjects.isEmpty()) {
                // delete all variants
                doc.removeXObjects(VARIANT_CLASS_REFERENCE);
            }
            return;
        }

        // get list of patient genes
        Set<String> genes = new HashSet<>();
        for (BaseObject geneObject : geneXWikiObjects) {
            if (geneObject == null) {
                continue;
            }

            BaseStringProperty field = (BaseStringProperty) geneObject.getField(GENE_KEY);
            // remove gene object from the document if key "gene" field is empty
            if (field == null || StringUtils.isEmpty(field.getValue())) {
                doc.removeXObject(geneObject);
                continue;
            }

            genes.add(field.getValue());
        }

        if (variantXWikiObjects == null || variantXWikiObjects.isEmpty()) {
            // nothing to remove
            return;
        }

        // loop through variants to detect those who do not correspond to any gene and delete them
        for (BaseObject variantObject : variantXWikiObjects) {
            if (variantObject == null) {
                continue;
            }

            BaseStringProperty variantField = (BaseStringProperty) variantObject.getField(VARIANT_KEY);
            // remove variant object from the document if key "cdna" field is empty
            if (variantField == null || StringUtils.isEmpty(variantField.getValue())) {
                doc.removeXObject(variantObject);
                continue;
            }

            // remove variant object from the document if its gene is not in patient genes
            BaseStringProperty geneField = (BaseStringProperty) variantObject.getField(GENE_KEY);
            if (!genes.contains(geneField.getValue())) {
                doc.removeXObject(variantObject);
            }
        }

    }
}
