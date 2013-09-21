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

import org.phenotips.configuration.PatientRecordConfiguration;
import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Default implementation of the {@link PatientRecordConfiguration} role.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultPatientRecordConfiguration implements PatientRecordConfiguration
{
    /** The name of the UIX parameter used for specifying the order of fields and sections. */
    private static final String SORT_PARAMETER_NAME = "order";

    /** The name of the UIX parameter used for specifying which fields and sections are enabled. */
    private static final String ENABLED_PARAMETER_NAME = "enabled";

    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    @Inject
    private UIExtensionManager uixManager;

    @Inject
    @Named("sortByParameter")
    private UIExtensionFilter orderFilter;

    @Override
    public List<String> getEnabledFieldNames()
    {
        List<String> result = new LinkedList<String>();
        List<UIExtension> sections = this.uixManager.get("org.phenotips.patientSheet.content");
        sections = this.orderFilter.filter(sections, SORT_PARAMETER_NAME);
        for (UIExtension section : sections) {
            if (!isEnabled(section)) {
                continue;
            }
            List<UIExtension> fields = this.uixManager.get(section.getId());
            fields = this.orderFilter.filter(fields, SORT_PARAMETER_NAME);
            for (UIExtension field : fields) {
                String usedFields = field.getParameters().get("fields");
                if (StringUtils.isBlank(usedFields) || !isEnabled(field)) {
                    continue;
                }
                for (String usedField : StringUtils.split(usedFields, ",")) {
                    result.add(StringUtils.trim(usedField));
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<String> getAllFieldNames()
    {
        try {
            XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
            BaseClass patientClass = context.getWiki().getDocument(Patient.CLASS_REFERENCE, context).getXClass();
            return Collections.unmodifiableList(Arrays.asList(patientClass.getPropertyNames()));
        } catch (XWikiException ex) {
            this.logger.error("Failed to access the patient class: {}", ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    /**
     * Check if an extension is enabled. Extensions are disabled by adding a {@code enabled=false} parameter. By default
     * extensions are enabled, so this method returns {@code false } only if it is explicitly disabled.
     * 
     * @param extension the extension to check
     * @return {@code false} if this extension has a parameter named {@code enabled} with the value {@code false},
     *         {@code true} otherwise
     */
    private boolean isEnabled(UIExtension extension)
    {
        return !StringUtils.equals("false", extension.getParameters().get(ENABLED_PARAMETER_NAME));
    }
}
