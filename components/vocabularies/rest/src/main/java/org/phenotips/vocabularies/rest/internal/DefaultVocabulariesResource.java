package org.phenotips.vocabularies.rest.internal;
import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.Relations;
import org.phenotips.vocabularies.rest.VocabulariesResource;
import org.phenotips.vocabularies.rest.VocabularyResource;
import org.phenotips.vocabularies.rest.VocabularyTermsResource;
import org.phenotips.vocabularies.rest.model.Link;
import org.phenotips.vocabularies.rest.model.VocabulariesRep;
import org.phenotips.vocabularies.rest.model.VocabularyRep;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;


/**
 * Default implementation for {@link VocabulariesResource} using XWiki's support for REST resources.
 * @version
 * @since
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

    @Override public VocabulariesRep getAllVocabularies()
    {
        VocabulariesRep result = new VocabulariesRep();
        List<String> vocabularyIDs = this.vm.getAvailableVocabularies();
        List<VocabularyRep> availableVocabs = new ArrayList<>();
        for (String vocabularyID : vocabularyIDs) {
            Vocabulary vocab = vm.getVocabulary(vocabularyID);
            VocabularyRep rep = this.objectFactory.createVocabularyRepresentation(vocab);
            List<Link> linkList = new ArrayList<>();
            linkList.add(new Link().withHref(
                    UriBuilder.fromUri(this.uriInfo.getBaseUri()).path(VocabularyResource.class).build(vocabularyID)
                        .toString())
                    .withRel(Relations.SELF)
            );
            linkList.add(new Link().withRel("suggest")
                    .withHref(UriBuilder.fromResource(VocabularyTermsResource.class)
                        .path(VocabularyTermsResource.class)
                        .path(VocabularyTermsResource.class, "suggest")
                        .build(vocabularyID)
                        .toString())
            );
            rep.withLinks(linkList);
            availableVocabs.add(rep);
        }
        result.withVocabularies(availableVocabs);
        result.withLinks(new Link().withRel(Relations.SELF)
            .withHref(UriBuilder.fromUri(this.uriInfo.getBaseUri()).path(VocabulariesResource.class).toString())
        );
        return result;
    }
}
