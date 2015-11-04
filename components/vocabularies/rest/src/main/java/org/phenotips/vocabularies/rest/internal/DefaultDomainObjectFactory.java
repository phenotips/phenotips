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
package org.phenotips.vocabularies.rest.internal;

import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.model.VocabularyTermSummary;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.stability.Unstable;

import javax.inject.Singleton;

import net.sf.json.JSONObject;

/**
 * @version $Id$
 * @since 1.3M1
 */
@Unstable
@Component
@Singleton
public class DefaultDomainObjectFactory implements DomainObjectFactory
{
    @Override
    public org.phenotips.vocabularies.rest.model.Vocabulary createVocabularyRepresentation(Vocabulary vocabulary)
    {
        org.phenotips.vocabularies.rest.model.Vocabulary result =
            new org.phenotips.vocabularies.rest.model.Vocabulary();
        result
            .withIdentifier(vocabulary.getIdentifier())
            .withName(vocabulary.getName())
            .withAliases(vocabulary.getAliases())
            .withSize(vocabulary.size())
            .withVersion(vocabulary.getVersion());
        try {
            result.withDefaultSourceLocation(vocabulary.getDefaultSourceLocation());
        } catch (UnsupportedOperationException e) {
            // Don't do anything and leave source empty
        }
        return result;
    }

    @Override
    public VocabularyTermSummary createVocabularyTermRepresentation(VocabularyTerm term)
    {
        VocabularyTermSummary rep = new VocabularyTermSummary();
        rep.withId(term.getId());
        rep.withName(term.getName());
        JSONObject jsonObject = (JSONObject) term.toJSON();
        String symbolKey = "symbol";
        if (jsonObject != null && jsonObject.get(symbolKey) != null) {
            rep.withSymbol(jsonObject.get(symbolKey).toString());
        }
        rep.withDescription(term.getDescription());
        return rep;
    }
}
