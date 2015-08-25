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
package org.phenotips.tools;

import org.phenotips.configuration.RecordConfigurationManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.event.AbstractDocumentEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.script.service.ScriptService;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.output.NullWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;

import net.sf.json.JSONObject;

/**
 * Provides access to the phenotype mappings configured for the current space. The field mappings are defined as a JSON
 * object contained in a document. The name of that document must be configured in the "phenotypeMapping" field of a
 * "DBConfigurationClass" object attached to the homepage (WebHome) of the current space.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = ScriptService.class)
@Named("phenotypeMapping")
@Singleton
public class PhenotypeMappingService implements ScriptService, EventListener, Initializable
{
    /**
     * Logging helper object.
     */
    @Inject
    private Logger logger;

    /**
     * Cached mappings for faster responses.
     */
    private Map<String, Map<String, Object>> cache = new HashMap<String, Map<String, Object>>();

    /**
     * Reference serializer used for converting entities into strings.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Reference resolver used for converting strings into entities.
     */
    @Inject
    private EntityReferenceResolver<String> resolver;

    /**
     * Velocity engine manager used for running the script containing the mapping.
     */
    @Inject
    private VelocityManager velocityManager;

    /**
     * Provides access to documents.
     */
    @Inject
    private DocumentAccessBridge bridge;

    /** Used for getting the configured mapping. */
    @Inject
    private RecordConfigurationManager configurationManager;

    /**
     * Allows registering this object as an event listener.
     */
    @Inject
    private ObservationManager observationManager;

    @Override
    public void initialize() throws InitializationException
    {
        this.observationManager.addListener(this);
    }

    @Override
    public String getName()
    {
        return "phenotype-mapping-cache";
    }

    @Override
    public List<Event> getEvents()
    {
        return Collections.emptyList();
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.cache.remove(((AbstractDocumentEvent) event).getEventFilter().getFilter());
    }

    /**
     * Get the configuration for the "phenotype" field.
     *
     * @return configuration object, should be a Map
     */
    public Object getPhenotype()
    {
        return getMapping("phenotype");
    }

    /**
     * Get the configuration for the "prenatal_phenotype" field.
     *
     * @return configuration object, should be a Map
     */
    public Object getPrenatalPhenotype()
    {
        return getMapping("prenatal_phenotype");
    }

    /**
     * Get the configuration for the "negative_phenotype" field.
     *
     * @return configuration object, should be a Map
     */
    public Object getNegativePhenotype()
    {
        return getMapping("negative_phenotype");
    }

    /**
     * Get the configuration for the "family_history" field.
     *
     * @return configuration object, should be a Map
     */
    public Object getFamilyHistory()
    {
        return getMapping("family_history");
    }

    /**
     * Get the configuration for the "extraMessages" pseudo-property.
     *
     * @return configuration object, should be a Map
     */
    public Object getExtraMessages()
    {
        return getMapping("extraMessages");
    }

    /**
     * Generic configuration getter.
     *
     * @param name the name of the configuration to return
     * @return configuration object, should be a Map
     */
    public Object get(String name)
    {
        return getMapping(name);
    }

    /**
     * Get the configuration for a specific property, taking into account the configuration for the current user and
     * current space.
     *
     * @param mappingName the name of the configuration to return
     * @return configuration object, should be a Map
     */
    private Object getMapping(String mappingName)
    {
        DocumentReference mappingDoc = getMappingDocument();
        Object result = getMapping(mappingDoc, mappingName);
        if (result == null) {
            try {
                String mappingContent = this.bridge.getDocumentContentForDefaultLanguage(mappingDoc);
                if (mappingContent.startsWith("{{velocity")) {
                    result = parseVelocityMapping(mappingDoc).get(mappingName);
                } else {
                    result = JSONObject.fromObject(mappingContent).get(mappingName);
                }
            } catch (Exception ex) {
                this.logger.warn("Failed to access mapping: {}", ex.getMessage());
            }
        }
        return result;
    }

    private Map<String, Object> parseVelocityMapping(DocumentReference mappingDoc)
    {
        try {
            VelocityEngine e = this.velocityManager.getVelocityEngine();
            VelocityContext c = this.velocityManager.getVelocityContext();
            e.evaluate(c, new NullWriter(), mappingDoc.getName(),
                this.bridge.getDocumentContentForDefaultLanguage(mappingDoc));
            this.observationManager.addEvent(this.getName(), new DocumentUpdatedEvent(mappingDoc));
            this.observationManager.addEvent(this.getName(), new DocumentDeletedEvent(mappingDoc));
            @SuppressWarnings("unchecked")
            Map<String, Object> mappings = (Map<String, Object>) c.get("mappings");
            setMappings(mappingDoc, mappings);
            return mappings;
        } catch (XWikiVelocityException ex) {
            this.logger.error("Failed to get a VelocityEngine instance", ex);
        } catch (Exception ex) {
            this.logger.error("Failed to parse mapping document [{}]", mappingDoc, ex);
        }
        return null;
    }

    /**
     * Get the configuration for a specific property, taking the configuration from a specified document.
     *
     * @param doc the reference of the document containing the mapping
     * @param mappingName the name of the configuration to return
     * @return configuration object, should be a Map
     */
    private Object getMapping(DocumentReference doc, String mappingName)
    {
        String docName = this.serializer.serialize(doc);
        if (!this.cache.containsKey(docName)) {
            return null;
        }
        return this.cache.get(docName).get(mappingName);
    }

    /**
     * Store all the mappings defined in the specified document in the cache.
     *
     * @param doc the reference of the document containing the mapping
     * @param mappings defined mappings, a map of maps
     */
    private void setMappings(DocumentReference doc, Map<String, Object> mappings)
    {
        this.cache.put(this.serializer.serialize(doc), mappings);
    }

    /**
     * Determine which document was configured as the mapping source in the current user's preferences, or, if missing,
     * in the current space's preferences.
     *
     * @return a document reference, as configured in the space preferences
     */
    private DocumentReference getMappingDocument()
    {
        DocumentReference mapping = getCurrentUserConfiguration();
        if (mapping == null) {
            mapping = this.configurationManager.getActiveConfiguration().getPhenotypeMapping();
        }
        return mapping;
    }

    private DocumentReference getCurrentUserConfiguration()
    {
        DocumentReference currentUserRef = this.bridge.getCurrentUserReference();
        if (currentUserRef == null) {
            return null;
        }
        DocumentReference classDocRef =
            new DocumentReference(currentUserRef.getWikiReference().getName(), "XWiki", "ConfigurationClass");
        int settingsObject = this.bridge.getObjectNumber(currentUserRef, classDocRef, "property", "phenotips_mapping");
        if (settingsObject != -1) {
            String targetMappingName =
                (String) this.bridge.getProperty(currentUserRef, classDocRef, settingsObject, "value");
            if (StringUtils.isNotEmpty(targetMappingName)) {
                return new DocumentReference(this.resolver.resolve(targetMappingName, EntityType.DOCUMENT));
            }
        }
        return null;
    }
}
