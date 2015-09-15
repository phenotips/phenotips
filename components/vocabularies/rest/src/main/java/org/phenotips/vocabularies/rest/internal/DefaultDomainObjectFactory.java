package org.phenotips.vocabularies.rest.internal;

import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.model.Vocabularies;
import org.phenotips.vocabularies.rest.model.VocabularyTerms;
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
    @Override public org.phenotips.vocabularies.rest.model.Vocabulary createVocabularyRepresentation(Vocabulary vocabulary)
    {
        org.phenotips.vocabularies.rest.model.Vocabulary result = new org.phenotips.vocabularies.rest.model.Vocabulary();
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

    @Override public Vocabularies createVocabulariesRepresentation(List<org.phenotips.vocabularies.rest.model.Vocabulary> vocabularyRepList)
    {
        return new Vocabularies().withVocabularies(vocabularyRepList);
    }

    @Override public org.phenotips.vocabularies.rest.model.VocabularyTerm createVocabularyTermRepresentation(VocabularyTerm term)
    {
        org.phenotips.vocabularies.rest.model.VocabularyTerm rep = new org.phenotips.vocabularies.rest.model.VocabularyTerm();
        rep.withId(term.getId());
        rep.withName(term.getName());
        rep.withDescription(term.getDescription());
        return rep;
    }

    @Override
    public VocabularyTerms createVocabularyTermsRepresentation(List<org.phenotips.vocabularies.rest.model.VocabularyTerm> vocabularyTermRepList)
    {
        return new VocabularyTerms().withVocabularyTerms(vocabularyTermRepList);
    }
}
