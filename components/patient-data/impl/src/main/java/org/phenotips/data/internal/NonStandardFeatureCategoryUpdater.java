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
import org.phenotips.data.Patient;
import org.phenotips.data.events.PatientChangingEvent;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Store the target category specified for free text phenotypes.
 *
 * @version $Id$
 */
@Component
@Named("phenotype-category-updater")
@Singleton
public class NonStandardFeatureCategoryUpdater extends AbstractEventListener
{
    /** The name of the class where the mapping between phenotypes and categories is stored. */
    private static final EntityReference CATEGORY_CLASS_REFERENCE = new EntityReference("PhenotypeCategoryClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /** The name of the mapping class property where the phenotype property name is stored. */
    private static final String NAME_PROPETY_NAME = "target_property_name";

    /** The name of the mapping class property where the phenotype value is stored. */
    private static final String VALUE_PROPETY_NAME = "target_property_value";

    /** The name of the mapping class property where the target category is stored. */
    private static final String CATEGORY_PROPETY_NAME = "target_property_category";

    @Inject
    private Execution execution;

    /** Needed for getting access to the request. */
    @Inject
    private Container container;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public NonStandardFeatureCategoryUpdater()
    {
        super("phenotype-category-updater", new PatientChangingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (this.container.getRequest() == null) {
            // Not a browser request, no custom categories, nothing to do
            return;
        }
        XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        XWikiDocument doc = (XWikiDocument) source;

        BaseObject patientRecordObj = doc.getXObject(Patient.CLASS_REFERENCE);
        if (patientRecordObj == null) {
            return;
        }
        BaseClass patientRecordClass = patientRecordObj.getXClass(context);

        for (String targetPropertyName : patientRecordClass.getPropertyList()) {
            if (!targetPropertyName.contains("phenotype")) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<String> phenotypes = patientRecordObj.getListValue(targetPropertyName);
            for (String phenotype : phenotypes) {
                if (!phenotype.matches("HP:[0-9]+")) {
                    List<String> category =
                        getParameter(targetPropertyName + "__suggested__" + phenotype.replaceAll("[^a-zA-Z0-9_]+", "_")
                            + "__category", patientRecordObj.getNumber());
                    if (category != null && !category.isEmpty()) {
                        storeCategory(phenotype, category, targetPropertyName, doc, context);
                    }
                }
            }
        }
    }

    /**
     * Store the category specified for a free-text phenotype in an object attached to the patient sheet.
     *
     * @param phenotype the free-text phenotype value found in the request
     * @param category the specified category where the phenotype belongs
     * @param targetPropertyName the name of the phenotype property where the {@code phenotype} was specified
     * @param doc the patient sheet
     * @param context the current execution context
     */
    private void storeCategory(String phenotype, List<String> category, String targetPropertyName, XWikiDocument doc,
        XWikiContext context)
    {
        BaseObject targetMappingObject = findCategoryObject(phenotype, targetPropertyName, doc, context);
        targetMappingObject.setStringValue(NAME_PROPETY_NAME, targetPropertyName);
        targetMappingObject.setStringValue(VALUE_PROPETY_NAME, phenotype);
        targetMappingObject.setDBStringListValue(CATEGORY_PROPETY_NAME, category);
    }

    /**
     * Find an XObject for storing the category for a phenotype. This method first searches for an existing object for
     * that phenotype, which will be updated, or if one isn't found, then a new object will be created.
     *
     * @param phenotype the free-text phenotype value found in the request
     * @param targetPropertyName the name of the phenotype property where the {@code phenotype} was specified
     * @param doc the patient sheet
     * @param context the current execution context
     * @return the target XObject
     */
    private BaseObject findCategoryObject(String phenotype, String targetPropertyName, XWikiDocument doc,
        XWikiContext context)
    {
        BaseObject targetMappingObject = null;
        try {
            List<BaseObject> existingObjects = doc.getXObjects(CATEGORY_CLASS_REFERENCE);
            if (existingObjects != null) {
                for (BaseObject mappingObject : existingObjects) {
                    if (mappingObject != null
                        && targetPropertyName.equals(mappingObject.getStringValue(NAME_PROPETY_NAME))
                        && phenotype.equals(mappingObject.getStringValue(VALUE_PROPETY_NAME))) {
                        targetMappingObject = mappingObject;
                        break;
                    }
                }
            }
            if (targetMappingObject == null) {
                targetMappingObject = doc.newXObject(CATEGORY_CLASS_REFERENCE, context);
            }
        } catch (XWikiException ex) {
            // Storage error, shouldn't happen
        }
        return targetMappingObject;
    }

    /**
     * Read a property from the request.
     *
     * @param propertyName the name of the property as it would appear in the class, for example
     *            {@code age_of_onset_years}
     * @param objectNumber the object's number
     * @return the value sent in the request, {@code null} if the property is missing
     */
    private List<String> getParameter(String propertyName, int objectNumber)
    {
        String parameterName =
            MessageFormat.format("{0}.PatientClass_{1}_{2}", Constants.CODE_SPACE, objectNumber, propertyName);
        String[] parameters =
            ((ServletRequest) this.container.getRequest()).getHttpServletRequest().getParameterValues(parameterName);
        if (parameters == null) {
            return null;
        }
        return Arrays.asList(parameters);
    }
}
