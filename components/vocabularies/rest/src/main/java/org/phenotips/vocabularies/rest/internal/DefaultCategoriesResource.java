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
import org.phenotips.vocabularies.rest.CategoriesResource;
import org.phenotips.vocabularies.rest.CategoryResource;
import org.phenotips.vocabularies.rest.CategoryTermSuggestionsResource;
import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.model.Categories;
import org.phenotips.vocabularies.rest.model.Category;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Default implementation for {@link CategoriesResource} using XWiki's support for REST resources.
 *
 * @version $Id $
 * @since 1.4M1
 */
@Component
@Named("org.phenotips.vocabularies.rest.internal.DefaultCategoriesResource")
@Singleton
public class DefaultCategoriesResource extends XWikiResource implements CategoriesResource
{
    private static final String CATEGORY_LABEL = "category";

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
    public Categories getAllCategories()
    {
        final Categories result = new Categories();
        // A list of available category identifiers.
        final List<String> categoryNames = this.vm.getAvailableCategories();
        // A list of vocabulary category objects.
        final List<Category> categories = new ArrayList<>();
        // Add category data, such as associated vocabularies, links, and granted rights.
        for (final String categoryName : categoryNames) {
            final Set<Vocabulary> vocabularies = this.vm.getVocabularies(categoryName);
            final List<org.phenotips.vocabularies.rest.model.Vocabulary> vocabularyReps =
                this.objectFactory.createVocabulariesList(vocabularies, this.autolinker.get(), this.uriInfo,
                    userIsAdmin());
            final Category category = this.objectFactory.createCategoryRepresentation(categoryName);
            // Add links and other data to category.
            category.withVocabularies(vocabularyReps)
                .withLinks(this.autolinker.get().forSecondaryResource(CategoryResource.class, this.uriInfo)
                .withActionableResources(CategoryTermSuggestionsResource.class)
                .withExtraParameters(CATEGORY_LABEL, categoryName)
                .withGrantedRight(userIsAdmin() ? Right.ADMIN : Right.VIEW)
                .build());
            categories.add(category);
        }
        result.withCategories(categories);
        result.withLinks(this.autolinker.get().forResource(getClass(), this.uriInfo).build());
        return result;
    }

    /**
     * Returns true if the user has admin rights.
     *
     * @return true iff the user has admin rights, false otherwise
     */
    private boolean userIsAdmin()
    {
        final User user = this.users.getCurrentUser();
        return this.authorizationService.hasAccess(user, Right.ADMIN,
            this.resolver.resolve(Constants.XWIKI_SPACE_REFERENCE));
    }
}
