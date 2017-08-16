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

import org.phenotips.Constants;
import org.phenotips.rest.Autolinker;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.vocabularies.rest.CategoryResource;
import org.phenotips.vocabularies.rest.CategoryTermSuggestionsResource;
import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.VocabulariesResource;
import org.phenotips.vocabularies.rest.VocabularyResource;
import org.phenotips.vocabularies.rest.VocabularyTermSuggestionsResource;
import org.phenotips.vocabularies.rest.model.Vocabularies;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Default implementation for {@link VocabulariesResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Named("org.phenotips.vocabularies.rest.internal.DefaultVocabulariesResource")
@Singleton
public class DefaultVocabulariesResource extends XWikiResource implements VocabulariesResource
{
    @Inject
    private VocabularyManager vm;

    @Inject
    private DomainObjectFactory objectFactory;

    @Inject
    private Provider<Autolinker> autolinker;

    @Inject
    private UserManager users;

    @Inject
    private AuthorizationService authorizationService;

    @Inject

    @Named("default")
    private DocumentReferenceResolver<EntityReference> resolver;

    @Override
    public Vocabularies getAllVocabularies()
    {
        Vocabularies result = new Vocabularies();
        List<String> vocabularyIDs = this.vm.getAvailableVocabularies();
        List<org.phenotips.vocabularies.rest.model.Vocabulary> availableVocabs =
            vocabularyIDs.stream()
                .map(vocabularyID -> this.vm.getVocabulary(vocabularyID))
                .map(vocab -> this.objectFactory.createLinkedVocabularyRepresentation(vocab, getVocabularyLinks(),
                    this::getCategoriesForVocabulary))
                .collect(Collectors.toList());
        result.withVocabularies(availableVocabs);
        result.withLinks(this.autolinker.get().forResource(getClass(), this.uriInfo).build());
        return result;
    }

    /**
     * Returns a list of {@link org.phenotips.vocabularies.rest.model.Category} for a specified vocabulary.
     *
     * @param vocab a {@link Vocabulary} of interest
     * @return a list of {@link org.phenotips.vocabularies.rest.model.Category} associated with the {@code vocab}
     */
    private List<org.phenotips.vocabularies.rest.model.Category> getCategoriesForVocabulary(final Vocabulary vocab)
    {
        final Collection<String> categories = vocab.getSupportedCategories();
        return this.objectFactory.createCategoriesRepresentation(categories, getCategoryLinks(), null);
    }

    /**
     * Returns the autolinker with all resources common to all vocabularies set.
     *
     * @return an {@link Autolinker}
     */
    private Autolinker getVocabularyLinks()
    {
        return this.autolinker.get()
            .forSecondaryResource(VocabularyResource.class, uriInfo)
            .withActionableResources(VocabularyTermSuggestionsResource.class)
            .withGrantedRight(userIsAdmin() ? Right.ADMIN : Right.VIEW);
    }

    /**
     * Returns the autolinker with all resources common to all categories set.
     *
     * @return an {@link Autolinker}
     */
    private Autolinker getCategoryLinks()
    {
        return this.autolinker.get()
            .forSecondaryResource(CategoryResource.class, this.uriInfo)
            .withActionableResources(CategoryTermSuggestionsResource.class)
            .withGrantedRight(userIsAdmin() ? Right.ADMIN : Right.VIEW);
    }

    private boolean userIsAdmin()
    {
        User user = this.users.getCurrentUser();
        return this.authorizationService.hasAccess(user, Right.ADMIN,
            this.resolver.resolve(Constants.XWIKI_SPACE_REFERENCE));
    }
}
