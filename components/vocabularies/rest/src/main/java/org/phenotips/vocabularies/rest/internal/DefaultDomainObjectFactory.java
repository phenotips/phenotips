package org.phenotips.vocabularies.rest.internal;

import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.model.VocabulariesRep;
import org.phenotips.vocabularies.rest.model.VocabularyRep;
import org.phenotips.vocabularies.rest.model.VocabularyTermRep;
import org.phenotips.vocabularies.rest.model.VocabularyTermsRep;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.stability.Unstable;

import java.util.List;

import javax.inject.Singleton;

/**
 * @version
 * @since
 */
@Unstable
@Component
@Singleton
public class DefaultDomainObjectFactory implements DomainObjectFactory
{
    @Override public VocabularyRep createVocabularyRepresentation(Vocabulary vocabulary)
    {
        VocabularyRep result = new VocabularyRep();
        result
            .withAliases(vocabulary.getAliases())
            .withSize(vocabulary.size())
            .withVersion(vocabulary.getVersion());
        try {
            result.withSource(vocabulary.getDefaultSourceLocation());
        } catch (UnsupportedOperationException e) {
            //Don't do anything and leave source empty
        }
        return result;
    }

    @Override public VocabulariesRep createVocabulariesRepresentation(List<VocabularyRep> vocabularyRepList)
    {
        return new VocabulariesRep().withVocabularies(vocabularyRepList);
    }

    @Override public VocabularyTermRep createVocabularyTermRepresentation(VocabularyTerm term)
    {
        VocabularyTermRep rep = new VocabularyTermRep();
        rep.withId(term.getId());
        rep.withName(term.getName());
        rep.withDescription(term.getDescription());
        return rep;
    }

    @Override
    public VocabularyTermsRep createVocabularyTermsRepresentation(List<VocabularyTermRep> vocabularyTermRepList)
    {
        return new VocabularyTermsRep().withVocabularyTerms(vocabularyTermRepList);
    }
}
