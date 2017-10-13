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
package org.phenotips.vocabulary.internal.solr;

import org.phenotips.Constants;
import org.phenotips.vocabulary.VocabularySourceRelocationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation for the {@link VocabularySourceRelocationService} component.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
public class DefaultVocabularySourceRelocationService implements VocabularySourceRelocationService
{
    /** The document where vocabulary relocation sources are stored. */
    private static final EntityReference VOCABULARY_RELOCATION_CLASS = new EntityReference(
        "VocabularySourceRelocationClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /** The document where preferences are stored. */
    private static final EntityReference PREFERENCES_DOCUMENT =
        new EntityReference("XWikiPreferences", EntityType.DOCUMENT, Constants.XWIKI_SPACE_REFERENCE);

    @Inject
    private Provider<XWikiContext> provider;

    @Inject
    private Logger logger;

    @Override
    public String getRelocation(String original)
    {
        try {
            XWikiContext context = this.provider.get();
            XWikiDocument prefsDoc = context.getWiki().getDocument(PREFERENCES_DOCUMENT, context);

            if (prefsDoc == null || prefsDoc.isNew()) {
                // Inaccessible or deleted document
                return original;
            }

            BaseObject object = prefsDoc.getXObject(VOCABULARY_RELOCATION_CLASS, "original", original, false);
            if (object != null) {
                String relocate = object.getStringValue("relocation");
                return StringUtils.defaultIfBlank(relocate, original);
            }
        } catch (XWikiException e) {
            this.logger.error("Failed to access the vocabulary relocations document: {}", e.getMessage(), e);
        }

        return original;
    }
}
