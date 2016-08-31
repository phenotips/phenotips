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
import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Feature;
import org.phenotips.data.FeatureMetadatum;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.StringProperty;

/**
 * Implementation of patient data based on the XWiki data model, where feature data is represented by properties in
 * objects of type {@code PhenoTips.PatientClass}.
 *
 * @version $Id$
 * @since 1.0M8
 */
public class PhenoTipsFeature extends AbstractPhenoTipsVocabularyProperty implements Feature
{
    static final String META_PROPERTY_NAME = "target_property_name";

    static final String META_PROPERTY_VALUE = "target_property_value";

    static final String META_PROPERTY_CATEGORIES = "target_property_category";

    /** The XClass used for storing category phenotype metadata. */
    static final EntityReference CATEGORY_CLASS_REFERENCE = new EntityReference("PhenotypeCategoryClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /**
     * Prefix marking negative feature.
     *
     * @see #isPresent()
     */
    private static final String NEGATIVE_PHENOTYPE_PREFIX = "negative_";

    private static final Pattern NEGATIVE_PREFIX = Pattern.compile("^" + NEGATIVE_PHENOTYPE_PREFIX);

    /** Used for reading and writing Features to JSON. */
    private static final String TYPE_JSON_KEY_NAME = "type";

    private static final String OBSERVED_JSON_KEY_NAME = "observed";

    private static final String NOTES_JSON_KEY_NAME = "notes";

    private static final String CATEGORIES_JSON_KEY_NAME = "categories";

    private static final String METADATA_JSON_KEY_NAME = "qualifiers";

    private static final String JSON_PRESENTSTATUS_YES = "yes";

    private static final String JSON_PRESENTSTATUS_NO = "no";

    /** Logging helper object. */
    private final Logger logger = LoggerFactory.getLogger(PhenoTipsFeature.class);

    /** The property name, the type optionally prefixed by "negative_". */
    private final String propertyName;

    /** @see #getType() */
    private final String type;

    /** @see #isPresent() */
    private final boolean present;

    /** @see #getNotes() */
    private final String notes;

    private final List<String> categories;

    /** @see #getMetadata() */
    private Map<String, FeatureMetadatum> metadata;

    /**
     * Constructor that copies the data from an XProperty value.
     *
     * @param doc the XDocument representing the described patient in XWiki
     * @param property the feature category XProperty
     * @param value the specific value from the property represented by this object
     */
    PhenoTipsFeature(XWikiDocument doc, ListProperty property, String value)
    {
        super(value);
        this.propertyName = property.getName();
        Matcher nameMatch = NEGATIVE_PREFIX.matcher(this.propertyName);
        this.present = !nameMatch.lookingAt();
        this.type = nameMatch.replaceFirst("");

        this.metadata = new TreeMap<>();
        String metadataNotes = "";
        try {
            BaseObject metadataObject = findMetadataObject(doc);
            if (metadataObject != null) {
                for (FeatureMetadatum.Type metadataType : FeatureMetadatum.Type.values()) {
                    StringProperty metadataProp = (StringProperty) metadataObject.get(metadataType.toString());
                    if (metadataProp != null && StringUtils.isNotBlank(metadataProp.getValue())) {
                        this.metadata.put(metadataType.toString(), new PhenoTipsFeatureMetadatum(metadataProp));
                    }
                }
                metadataNotes = metadataObject.getLargeStringValue("comments");
            }
        } catch (XWikiException ex) {
            // Cannot access metadata, simply ignore
            this.logger.info("Failed to retrieve phenotype metadata: {}", ex.getMessage());
        }
        this.notes = StringUtils.defaultIfBlank(metadataNotes, "");
        // Readonly from now on
        this.metadata = Collections.unmodifiableMap(this.metadata);

        List<String> categoriesList = Collections.emptyList();
        try {
            BaseObject categoriesObject = findCategoriesObject(doc);
            if (categoriesObject != null && categoriesObject.getListValue(META_PROPERTY_CATEGORIES) != null) {
                @SuppressWarnings("unchecked")
                List<String> originalCategories = categoriesObject.getListValue(META_PROPERTY_CATEGORIES);
                categoriesList = Collections.unmodifiableList(originalCategories);
            }
        } catch (XWikiException ex) {
            // Cannot access metadata, simply ignore
            this.logger.info("Failed to retrieve phenotype categories: {}", ex.getMessage());
        }
        this.categories = categoriesList;
    }

    /**
     * Constructor for initializing from a JSON Object.
     *
     * @param json JSON object describing this property
     */
    PhenoTipsFeature(JSONObject json)
    {
        super(json);
        this.present =
            JSON_PRESENTSTATUS_YES.equalsIgnoreCase(json.optString(OBSERVED_JSON_KEY_NAME, JSON_PRESENTSTATUS_YES));
        this.type = json.optString(TYPE_JSON_KEY_NAME, "phenotype");
        this.propertyName = (this.present) ? this.type : NEGATIVE_PHENOTYPE_PREFIX + this.type;
        this.metadata = new TreeMap<>();

        if (json.has(METADATA_JSON_KEY_NAME)) {
            JSONArray jsonMetadata = json.getJSONArray(METADATA_JSON_KEY_NAME);
            for (int i = 0; i < jsonMetadata.length(); ++i) {
                String metaType = jsonMetadata.getJSONObject(i).getString(TYPE_JSON_KEY_NAME);
                this.metadata.put(metaType, new PhenoTipsFeatureMetadatum(jsonMetadata.getJSONObject(i)));
            }
        }
        this.metadata = Collections.unmodifiableMap(this.metadata);

        this.notes = json.optString(NOTES_JSON_KEY_NAME);
        if (json.has(CATEGORIES_JSON_KEY_NAME)) {
            List<String> categoriesList = new ArrayList<>();
            JSONArray jsonCategories = json.getJSONArray(CATEGORIES_JSON_KEY_NAME);
            for (int i = 0; i < jsonCategories.length(); ++i) {
                categoriesList.add(jsonCategories.getJSONObject(i).getString(ID_JSON_KEY_NAME));
            }
            this.categories = Collections.unmodifiableList(categoriesList);
        } else {
            this.categories = Collections.emptyList();
        }
    }

    @Override
    public String getType()
    {
        return this.type;
    }

    @Override
    public boolean isPresent()
    {
        return this.present;
    }

    @Override
    public String getValue()
    {
        if (StringUtils.isEmpty(getId())) {
            return getName();
        }
        return getId();
    }

    @Override
    public Map<String, ? extends FeatureMetadatum> getMetadata()
    {
        return this.metadata;
    }

    @Override
    public String getNotes()
    {
        return this.notes;
    }

    @Override
    public String getPropertyName()
    {
        return this.propertyName;
    }

    @Override
    public List<String> getCategories()
    {
        return this.categories;
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject result = super.toJSON();
        result.put(TYPE_JSON_KEY_NAME, getType());
        result.put(OBSERVED_JSON_KEY_NAME, (this.present ? JSON_PRESENTSTATUS_YES : JSON_PRESENTSTATUS_NO));
        if (!this.metadata.isEmpty()) {
            JSONArray metadataList = new JSONArray();
            for (FeatureMetadatum metadatum : this.metadata.values()) {
                metadataList.put(metadatum.toJSON());
            }
            result.put(METADATA_JSON_KEY_NAME, metadataList);
        }
        if (StringUtils.isNotBlank(this.notes)) {
            result.put(NOTES_JSON_KEY_NAME, this.notes);
        }
        if (!this.categories.isEmpty()) {
            JSONArray categoriesList = new JSONArray();
            try {
                VocabularyManager vm =
                    ComponentManagerRegistry.getContextComponentManager().getInstance(VocabularyManager.class);
                for (String category : this.categories) {
                    VocabularyTerm term = vm.resolveTerm(category);
                    if (term != null && StringUtils.isNotEmpty(term.getName())) {
                        JSONObject categoryObject = new JSONObject();
                        categoryObject.put(ID_JSON_KEY_NAME, term.getId());
                        categoryObject.put(NAME_JSON_KEY_NAME, term.getName());
                        categoriesList.put(categoryObject);
                    }
                }
            } catch (ComponentLookupException ex) {
                // Shouldn't happen
            }
            result.put(CATEGORIES_JSON_KEY_NAME, categoriesList);
        }
        return result;
    }

    /**
     * Find the XObject that contains metadata for this feature, if any.
     *
     * @param doc the patient's XDocument, where metadata objects are stored
     * @return the found object, or {@code null} if one wasn't found
     * @throws XWikiException if accessing the data fails
     */
    private BaseObject findMetadataObject(XWikiDocument doc) throws XWikiException
    {
        List<BaseObject> objects = doc.getXObjects(FeatureMetadatum.CLASS_REFERENCE);
        if (objects != null && !objects.isEmpty()) {
            for (BaseObject o : objects) {
                if (o == null) {
                    continue;
                }
                StringProperty nameProperty = (StringProperty) o.get(META_PROPERTY_NAME);
                StringProperty valueProperty = (StringProperty) o.get(META_PROPERTY_VALUE);
                if (nameProperty != null && StringUtils.equals(nameProperty.getValue(), this.propertyName)
                    && valueProperty != null && StringUtils.equals(valueProperty.getValue(), this.getValue())) {
                    return o;
                }
            }
        }
        return null;
    }

    /**
     * Find the XObject that contains the custom categories for this non-standard feature, if any.
     *
     * @param doc the patient's XDocument, where objects are stored
     * @return the found object, or {@code null} if one wasn't found
     * @throws XWikiException if accessing the data fails
     */
    private BaseObject findCategoriesObject(XWikiDocument doc) throws XWikiException
    {
        List<BaseObject> objects =
            doc.getXObjects(CATEGORY_CLASS_REFERENCE);
        if (objects != null && !objects.isEmpty()) {
            for (BaseObject o : objects) {
                if (o == null) {
                    continue;
                }
                StringProperty nameProperty = (StringProperty) o.get(META_PROPERTY_NAME);
                StringProperty valueProperty = (StringProperty) o.get(META_PROPERTY_VALUE);
                if (nameProperty != null && StringUtils.equals(nameProperty.getValue(), this.propertyName)
                    && valueProperty != null && StringUtils.equals(valueProperty.getValue(), this.getValue())) {
                    return o;
                }
            }
        }
        return null;
    }
}
