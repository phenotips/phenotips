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
package org.phenotips.data.internal.serializer;

import org.phenotips.Constants;
import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.PatientDataSerializer;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;

/**
 * Handles metadata associated with the patient record.
 * Such as ontologies and PhenoTips versioning, record creation and modification date, authorship, etc.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component
@Named("meta-serializer")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class MetaSerializer implements PatientDataSerializer
{
    /** The XClass used for storing version data of different ontologies. */
    private static final EntityReference VERSION_REFERENCE =
        new EntityReference("OntologyVersionClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String JSON_ELEMENT_NAME = "meta";

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private Logger logger;

    private Map<String, String> ontologyVersions = new HashMap<String, String>();

    private Map<String, String> phenotipsVersion = new HashMap<String, String>();

    @Override
    public void readDocument(DocumentReference documentReference)
    {
        try {
            XWikiDocument doc = (XWikiDocument) documentAccessBridge.getDocument(documentReference);
            readOntologyVersionClass(doc);
            readPhenoTipsVerison();
        } catch (Exception e) {
            logger.error("Could not find requested document");
        }
    }

    /**
     * Reads all the OntologyVersionClass objects from the XWiki patient document.
     *
     * @param doc the document which should be looked in for OntologyVersionClass objects
     */
    private void readOntologyVersionClass(XWikiDocument doc)
    {
        List<BaseObject> ontologyVersionObjects = doc.getXObjects(VERSION_REFERENCE);
        if (ontologyVersionObjects == null) {
            return;
        }

        for (BaseObject versionObject : ontologyVersionObjects) {
            String versionType = versionObject.getStringValue("name");
            String versionString = versionObject.getStringValue("version");
            if (StringUtils.isNotEmpty(versionType) && StringUtils.isNotEmpty(versionString)) {
                this.ontologyVersions.put(versionType, versionString);
            }
        }
    }

    /**
     * Gets the phenotips version from the XWiki Distribution Manager.
     */
    private void readPhenoTipsVerison()
    {
        try {
            DistributionManager distribution =
                ComponentManagerRegistry.getContextComponentManager().getInstance(DistributionManager.class);
            phenotipsVersion.put("phenotips_version",
                distribution.getDistributionExtension().getId().getVersion().toString());
        } catch (ComponentLookupException ex) {
            //Shouldn't happen
        }
    }

    @Override
    public void writeJSON(JSONObject json)
    {
        JSONObject metaJSON = new JSONObject();
        metaJSON.putAll(phenotipsVersion);
        metaJSON.putAll(ontologyVersions);
        json.element(JSON_ELEMENT_NAME, metaJSON);
    }

    @Override
    public void readJSON(JSONObject json)
    {
        throw new UnsupportedOperationException();
    }
}
