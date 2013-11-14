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
package org.phenotips.configuration.internal;

import org.phenotips.Constants;
import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.configuration.internal.configured.ConfiguredRecordConfiguration;
import org.phenotips.configuration.internal.configured.CustomConfiguration;
import org.phenotips.configuration.internal.global.GlobalRecordConfiguration;
import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;
import org.xwiki.users.UserManager;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation for the {@link RecordConfigurationManager} component.
 * 
 * @version $Id$
 * @since 1.0M9
 */
@Component
@Singleton
public class DefaultRecordConfigurationManager implements RecordConfigurationManager
{
    /** Reference to the xclass which allows to bind a specific form customization to a patient record. */
    public static final EntityReference CUSTOMIZATION_BINDING_CLASS_REFERENCE = new EntityReference(
        "FormCustomizationBindingClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /** Logging helper. */
    @Inject
    private Logger logger;

    /** Provides access to the current request context. */
    @Inject
    private Execution execution;

    /** Lists the patient form sections and fields. */
    @Inject
    private UIExtensionManager uixManager;

    /** Sorts fields by their declared order. */
    @Inject
    @Named("sortByParameter")
    private UIExtensionFilter orderFilter;

    /** Lists the groups of a user. */
    @Inject
    private GroupManager groupManager;

    /** Provides the current user. */
    @Inject
    private UserManager userManager;

    /** Provides access to the data. */
    @Inject
    private DocumentAccessBridge dab;

    /** Completes xclass references with the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> resolver;

    /** Parses serialized document references into proper references. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceParser;

    @Override
    public RecordConfiguration getActiveConfiguration()
    {
        RecordConfiguration boundConfig = getBoundConfiguration();
        if (boundConfig != null) {
            return boundConfig;
        }
        Group configurationGroup = findConfigurationGroup();
        if (configurationGroup != null) {
            try {
                XWikiContext context = getXContext();
                XWikiDocument doc = context.getWiki().getDocument(configurationGroup.getReference(), context);
                CustomConfiguration configuration =
                    new CustomConfiguration(doc.getXObject(RecordConfiguration.CUSTOM_PREFERENCES_CLASS));
                return new ConfiguredRecordConfiguration(configuration, this.execution, this.uixManager,
                    this.orderFilter);
            } catch (Exception ex) {
                this.logger.warn("Failed to read the group configuration for [{}]: {}",
                    configurationGroup.getReference(), ex.getMessage());
            }
        }
        return new GlobalRecordConfiguration(this.execution, this.uixManager, this.orderFilter);
    }

    /**
     * If the current document is a patient record, and it has a valid specific form configuration binding specified,
     * then return that configuration.
     * 
     * @return a form configuration, if one is bound to the current document, or {@code null} otherwise
     */
    private RecordConfiguration getBoundConfiguration()
    {
        String boundConfig = (String) this.dab.getProperty(this.dab.getCurrentDocumentReference(),
            this.resolver.resolve(CUSTOMIZATION_BINDING_CLASS_REFERENCE), "configReference");
        if (StringUtils.isNotBlank(boundConfig)) {
            try {
                XWikiContext context = getXContext();
                XWikiDocument doc = context.getWiki().getDocument(this.referenceParser.resolve(boundConfig), context);
                CustomConfiguration configuration =
                    new CustomConfiguration(doc.getXObject(RecordConfiguration.CUSTOM_PREFERENCES_CLASS));
                return new ConfiguredRecordConfiguration(configuration, this.execution, this.uixManager,
                    this.orderFilter);
            } catch (Exception ex) {
                this.logger.warn("Failed to read the bound configuration [{}] for [{}]: {}",
                    boundConfig, this.dab.getCurrentDocumentReference(), ex.getMessage());
            }
        }
        return null;
    }

    /**
     * From the list of groups that the current user belongs to, pick the first that has a non-empty custom record
     * configuration.
     * 
     * @return a group that has a custom configuration, or {@code null} if the current user is not authenticated,
     *         doesn't belong to any groups, or none of the groups have a custom configuration.
     */
    private Group findConfigurationGroup()
    {
        Set<Group> groups = this.groupManager.getGroupsForUser(this.userManager.getCurrentUser());
        if (groups != null && !groups.isEmpty()) {
            XWikiContext context = getXContext();
            for (Group group : groups) {
                try {
                    XWikiDocument doc = context.getWiki().getDocument(group.getReference(), context);
                    BaseObject configuration = doc.getXObject(RecordConfiguration.CUSTOM_PREFERENCES_CLASS);
                    if (configuration != null && !configuration.getListValue("sections").isEmpty()) {
                        return group;
                    }
                } catch (Exception ex) {
                    this.logger.warn("Failed to access group [{}]: {}", group.getReference(), ex.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * Get the current request context from the execution context manager.
     * 
     * @return the current request context
     */
    private XWikiContext getXContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }
}
