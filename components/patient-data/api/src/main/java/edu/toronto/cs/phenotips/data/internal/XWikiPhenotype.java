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
package edu.toronto.cs.phenotips.data.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.StringProperty;

import edu.toronto.cs.phenotips.data.Phenotype;
import edu.toronto.cs.phenotips.data.PhenotypeMetadatum;

/**
 * @version $Id$
 */
public class XWikiPhenotype implements Phenotype
{
    private final Logger logger = LoggerFactory.getLogger(XWikiPhenotype.class);

    private static final Pattern NEGATIVE_PREFIX = Pattern.compile("^negative_");

    private static final EntityReference METADATA_CLASS = new EntityReference("PhenotypeMetaClass",
        EntityType.DOCUMENT, new EntityReference("PhenoTips", EntityType.SPACE));

    private final XWikiDocument doc;

    private final String name;

    private final String value;

    private final String type;

    private final boolean present;

    private Map<String, PhenotypeMetadatum> metadata;

    XWikiPhenotype(XWikiDocument doc, DBStringListProperty property, String value)
    {
        this.doc = doc;
        this.value = value;
        this.name = property.getName();
        Matcher nameMatch = NEGATIVE_PREFIX.matcher(this.name);
        this.present = !nameMatch.lookingAt();
        this.type = nameMatch.replaceFirst("");
        this.metadata = new HashMap<String, PhenotypeMetadatum>();
        try {
            BaseObject metadata = findMetadataObject();
            if (metadata != null) {
                if (metadata.get(PhenotypeMetadatum.AGE_OF_ONSET) != null) {
                    this.metadata.put(PhenotypeMetadatum.AGE_OF_ONSET, new XWikiPhenotypeMetadatum(
                        (StringProperty) metadata.get(PhenotypeMetadatum.AGE_OF_ONSET)));
                }
                if (metadata.get(PhenotypeMetadatum.SPEED_OF_ONSET) != null) {
                    this.metadata.put(PhenotypeMetadatum.SPEED_OF_ONSET, new XWikiPhenotypeMetadatum(
                        (StringProperty) metadata.get(PhenotypeMetadatum.SPEED_OF_ONSET)));
                }
                if (metadata.get(PhenotypeMetadatum.PACE_OF_PROGRESSION) != null) {
                    this.metadata.put(PhenotypeMetadatum.PACE_OF_PROGRESSION, new XWikiPhenotypeMetadatum(
                        (StringProperty) metadata.get(PhenotypeMetadatum.PACE_OF_PROGRESSION)));
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
    public String getId()
    {
        return this.value;
    }

    @Override
    public String getName()
    {
        // FIXME implementation missing
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPresent()
    {
        return this.present;
    }

    @Override
    public Map<String, PhenotypeMetadatum> getMetadata()
    {
        return this.metadata;
    }

    @Override
    public String toString()
    {
        return toJSON().toString(2);
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();
        result.element("type", getType());
        result.element("id", getId());
        if (!this.metadata.isEmpty()) {
            JSONArray metadata = new JSONArray();
            for (PhenotypeMetadatum meta : this.metadata.values()) {
                metadata.add(meta.toJSON());
            }
            result.element("metadata", metadata);
        }
        return result;
    }

    private BaseObject findMetadataObject() throws XWikiException
    {
        List<BaseObject> objects = this.doc.getXObjects(METADATA_CLASS);
        if (objects != null) {
            for (BaseObject o : objects) {
                StringProperty nameProperty = (StringProperty) o.get("target_property_name");
                StringProperty valueProperty = (StringProperty) o.get("target_property_value");
                if (nameProperty != null && StringUtils.equals(nameProperty.getValue(), this.name)
                    && valueProperty != null && StringUtils.equals(valueProperty.getValue(), this.value)) {
                    return o;
                }
            }
        }
        return null;
    }
}
