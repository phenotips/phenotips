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
import org.phenotips.data.rest.Relations;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.VocabularyResource;
import org.phenotips.vocabularies.rest.VocabularyTermsResource;
import org.phenotips.vocabularies.rest.model.Link;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;

/**
 * Default implementation of {@link VocabularyResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Named("org.phenotips.vocabularies.rest.internal.DefaultVocabularyResource")
@Singleton
@Unstable
public class DefaultVocabularyResource extends XWikiResource implements VocabularyResource
{
    /**
     * An entity reference used to check for wiki admin rights.
     */
    public static final EntityReference MAIN_WIKI_REFERENCE = new EntityReference("WebHome",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    @Inject
    private VocabularyManager vm;

    @Inject
    private DomainObjectFactory objectFactory;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private Execution execution;

    @Inject
    private UserManager users;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> resolver;

    @Override
    public Response reindex(String url, String vocabularyId)
    {
        Response result;

        if (StringUtils.isEmpty(url) || StringUtils.isEmpty(vocabularyId)) {
            result = Response.status(Response.Status.BAD_REQUEST).build();
        }
        // check permissions, User must have admin rights on the entire wiki
        if (!this.userIsAdmin()) {
            result = Response.status(Response.Status.FORBIDDEN).build();
        }
        Vocabulary vocabulary = this.vm.getVocabulary(vocabularyId);
        if (vocabulary == null) {
            result = Response.status(Response.Status.NOT_FOUND).build();
        }
        try {
            int reindexStatus = vocabulary.reindex(url);

            if (reindexStatus == 0) {
                result = Response.ok().build();
            } else if (reindexStatus == 1) {
                result = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } else {
                result = Response.status(Response.Status.BAD_REQUEST).build();
            }
        } catch (UnsupportedOperationException e) {
            result = Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }
        return result;
    }

    @Override
    public org.phenotips.vocabularies.rest.model.Vocabulary getVocabulary(String vocabularyId)
    {
        Vocabulary vocabulary = this.vm.getVocabulary(vocabularyId);
        if (vocabulary == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        org.phenotips.vocabularies.rest.model.Vocabulary rep =
            this.objectFactory.createVocabularyRepresentation(vocabulary);
        // create links
        Collection<Link> links = new ArrayList<>();
        links.add(new Link().withRel(Relations.SEARCH)
            .withHref(UriBuilder.fromUri(this.uriInfo.getBaseUri())
                .path(VocabularyTermsResource.class)
                .build(vocabularyId).toString()));
        rep.withLinks(links);
        return rep;
    }

    private boolean userIsAdmin()
    {
        User user = this.users.getCurrentUser();
        return this.authorizationService.hasAccess(user, Right.ADMIN, this.resolver.resolve(MAIN_WIKI_REFERENCE));
    }
}
