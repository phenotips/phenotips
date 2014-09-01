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
package org.phenotips.listeners;

import org.phenotips.Constants;

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.EventListener;
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
public class FreePhenotypeCategoryUpdater implements EventListener
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

    /** Needed for getting access to the request. */
    @Inject
    private Container container;

    @Override
    public String getName()
    {
        return "phenotype-category-updater";
    }

    @Override
    public List<Event> getEvents()
    {
        // The list of events this listener listens to
        return Arrays.<Event>asList(new DocumentCreatingEvent(), new DocumentUpdatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiContext context = (XWikiContext) data;
        XWikiDocument doc = (XWikiDocument) source;

        BaseObject patientRecordObj =
            doc.getXObject(new DocumentReference(doc.getDocumentReference().getWikiReference().getName(),
                Constants.CODE_SPACE, "PatientClass"));
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
                        getParameter(targetPropertyName + "__" + phenotype.replaceAll("[^a-zA-Z0-9_]+", "_")
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
                targetMappingObject =
                    doc.getXObject(CATEGORY_CLASS_REFERENCE, doc.createXObject(CATEGORY_CLASS_REFERENCE, context));
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
