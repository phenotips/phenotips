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
package org.phenotips.data.internal.controller;

import org.phenotips.Constants;
import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;

/**
 * Exposes the version of the ontologies used for creating the patient record, as well as the current PhenoTips version.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component(roles = { PatientDataController.class })
@Named("versions")
@Singleton
public class VersionsController extends AbstractSimpleController
    implements PatientDataController<String>
{
    /** The XClass used for storing version data of different ontologies. */
    private static final EntityReference ONTOLOGY_VERSION_CLASS_REFERENCE =
        new EntityReference("OntologyVersionClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String CONTROLLER_NAME = "versions";

    @Inject
    private Logger logger;

    @Override
    public PatientData<String> load(Patient patient)
    {
        Map<String, String> versions = new LinkedHashMap<>();

        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            addOntologyVersions(doc, versions);
            addPhenoTipsVersion(versions);
        } catch (Exception ex) {
            this.logger.error("Could not find requested document: {}", ex.getMessage());
        }
        return new DictionaryPatientData<String>(getName(), versions);
    }

    /**
     * Reads all the {@code PhenoTips.OntologyVersionClass} objects from the patient document.
     *
     * @param doc the document storing the patient data
     */
    private void addOntologyVersions(XWikiDocument doc, Map<String, String> versions)
    {
        List<BaseObject> ontologyVersionObjects = doc.getXObjects(ONTOLOGY_VERSION_CLASS_REFERENCE);
        if (ontologyVersionObjects == null) {
            return;
        }

        for (BaseObject versionObject : ontologyVersionObjects) {
            String versionType = versionObject.getStringValue("name");
            String versionString = versionObject.getStringValue("version");
            if (StringUtils.isNotEmpty(versionType) && StringUtils.isNotEmpty(versionString)) {
                versions.put(versionType + "_version", versionString);
            }
        }
    }

    /**
     * Gets the PhenoTips version from the XWiki Distribution Manager and adds it to the patient data being loaded.
     *
     * @param versions the list of version data being constructed
     */
    private void addPhenoTipsVersion(Map<String, String> versions)
    {
        try {
            DistributionManager distribution =
                ComponentManagerRegistry.getContextComponentManager().getInstance(DistributionManager.class);
            versions.put("phenotips_version", distribution.getDistributionExtension().getId().getVersion().toString());
        } catch (ComponentLookupException ex) {
            // Shouldn't happen
            this.logger.error("Could not find DistributionManager component");
        }
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        // unlike all other controllers, there is no field name controlling version information
        // so for this particular controller a special getEnablingFieldName() property is used
        // which, if included in the list of fields, enables version data to be dumped into JSON
        // (note that we may sometimes want to omit this data when presenting results for the end users)
        if (selectedFieldNames == null || selectedFieldNames.contains(getEnablingFieldName())) {
            super.writeJSON(patient, json, null);
        }
    }

    @Override
    protected List<String> getProperties()
    {
        // Not used, since there's a custom load method
        return null;
    }

    @Override
    public String getName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    protected String getJsonPropertyName()
    {
        return "meta";
    }

    /**
     * Unlike all other controllers, there is no field name controlling presence of version information in JSON output.
     * This method returns a name which can be used instead.
     *
     * @return a name which can be included in the list of enabled fields to enable version info in JSON output
     */
    public static String getEnablingFieldName()
    {
        return CONTROLLER_NAME;
    }
}
