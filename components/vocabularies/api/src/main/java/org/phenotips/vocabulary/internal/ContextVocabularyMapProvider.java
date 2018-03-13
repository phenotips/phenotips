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
package org.phenotips.vocabulary.internal;

import org.phenotips.vocabulary.Vocabulary;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * This provides a map of all the {@link Vocabulary} instances visible in the current context. This class shouldn't be
 * needed, but the generic provider from the XWiki core only looks up in the global context, which doesn't take into
 * account vocabularies installed in only one wiki instance.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
public class ContextVocabularyMapProvider implements Provider<Map<String, Vocabulary>>
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> cm;

    @Override
    public Map<String, Vocabulary> get()
    {
        try {
            return this.cm.get().getInstanceMap(Vocabulary.class);
        } catch (ComponentLookupException e) {
            return Collections.emptyMap();
        }
    }
}
