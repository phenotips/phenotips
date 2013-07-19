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
package org.phenotips.data.internal;

import org.phenotips.data.Phenotype;
import org.phenotips.data.PhenotypeMetadatum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.StringProperty;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Implementation of patient data based on the XWiki data model, where phenotype data is represented by properties in
 * objects of type {@code PhenoTips.PatientClass}.
 * 
 * @version $Id$
 * @since 1.0M8
 */
public class PhenoTipsPhenotype extends AbstractPhenoTipsOntologyProperty implements Phenotype
{
    /**
     * Prefix marking negative phenotypes.
     * 
     * @see #isPresent()
     */
    private static final Pattern NEGATIVE_PREFIX = Pattern.compile("^negative_");

    /** Logging helper object. */
    private final Logger logger = LoggerFactory.getLogger(PhenoTipsPhenotype.class);

    /** The property name, the type eventually prefixed by "negative_". */
    private final String propertyName;

    /** @see #getType() */
    private final String type;

    /** @see #isPresent() */
    private final boolean present;

    /** @see #getMetadata() */
    private Map<String, PhenotypeMetadatum> metadata;

    /**
     * Constructor that copies the data from an XProperty value.
     * 
     * @param doc the XDocument representing the described patient in XWiki
     * @param property the phenotype category XProperty
     * @param value the specific value from the property represented by this object
     */
    PhenoTipsPhenotype(XWikiDocument doc, DBStringListProperty property, String value)
    {
        super(value);
        this.propertyName = property.getName();
        Matcher nameMatch = NEGATIVE_PREFIX.matcher(this.propertyName);
        this.present = !nameMatch.lookingAt();
        this.type = nameMatch.replaceFirst("");
        this.metadata = new HashMap<String, PhenotypeMetadatum>();
        try {
            BaseObject metadataObject = findMetadataObject(doc);
            if (metadataObject != null) {
                for (PhenotypeMetadatum.Type metadataType : PhenotypeMetadatum.Type.values()) {
                    if (metadataObject.get(metadataType.toString()) != null) {
                        this.metadata.put(metadataType.toString(), new PhenoTipsPhenotypeMetadatum(
                            (StringProperty) metadataObject.get(metadataType.toString())));
                    }
                }
            }
        } catch (XWikiException ex) {
            // Cannot access metadata, simply ignore
            this.logger.info("Failed to retrieve phenotype metadata: {}", ex.getMessage());
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
    public Map<String, ? extends PhenotypeMetadatum> getMetadata()
    {
        return this.metadata;
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject result = super.toJSON();
        result.element("type", getType());
        result.element("isPresent", this.present);
        if (!this.metadata.isEmpty()) {
            JSONArray metadataList = new JSONArray();
            for (PhenotypeMetadatum metadatum : this.metadata.values()) {
                metadataList.add(metadatum.toJSON());
            }
            result.element("metadata", metadataList);
        }
        return result;
    }

    /**
     * Find the XObject that contains metadata for this phenotype, if any.
     * 
     * @param doc the patient's XDocument, where metadata objects are stored
     * @return the found object, or {@code null} if one wasn't found
     * @throws XWikiException if accessing the data fails
     */
    private BaseObject findMetadataObject(XWikiDocument doc) throws XWikiException
    {
        List<BaseObject> objects = doc.getXObjects(PhenoTipsPhenotypeMetadatum.CLASS_REFERENCE);
        if (objects != null) {
            for (BaseObject o : objects) {
                StringProperty nameProperty = (StringProperty) o.get("target_property_name");
                StringProperty valueProperty = (StringProperty) o.get("target_property_value");
                if (nameProperty != null && StringUtils.equals(nameProperty.getValue(), this.propertyName)
                    && valueProperty != null && StringUtils.equals(valueProperty.getValue(), this.id)) {
                    return o;
                }
            }
        }
        return null;
    }
}
