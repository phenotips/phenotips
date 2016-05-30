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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.permissions.PermissionsConfiguration;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultPermissionsConfiguration implements PermissionsConfiguration
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> resolver;

    @Inject
    private DocumentAccessBridge dab;

    @Override
    public boolean isVisibilityDisabled(String visibilityName)
    {
        DocumentReference preferencesDocument = this.resolver.resolve(PREFERENCES_DOCUMENT);
        DocumentReference configurationClassDocument = this.resolver.resolve(VISIBILITY_CONFIGURATION_CLASS_REFERENCE);
        @SuppressWarnings("unchecked")
        List<String> disabledVisibilities =
            (List<String>) this.dab.getProperty(new ObjectPropertyReference("disabledLevels",
                new BaseObjectReference(configurationClassDocument, 0, preferencesDocument)));
        return disabledVisibilities != null && disabledVisibilities.contains(visibilityName);
    }
}
