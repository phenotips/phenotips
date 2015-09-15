package org.phenotips.vocabularies.rest;

import org.phenotips.vocabularies.rest.model.Vocabularies;
import org.phenotips.vocabularies.rest.model.VocabularyTerms;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.List;

/**
 *
 * @version
 * @since
 */
@Role
@Unstable
public interface DomainObjectFactory
{
    org.phenotips.vocabularies.rest.model.Vocabulary createVocabularyRepresentation(Vocabulary vocabulary);

    Vocabularies createVocabulariesRepresentation(List<org.phenotips.vocabularies.rest.model.Vocabulary> vocabularyRepList);

    org.phenotips.vocabularies.rest.model.VocabularyTerm createVocabularyTermRepresentation(VocabularyTerm term);

    VocabularyTerms createVocabularyTermsRepresentation(List<org.phenotips.vocabularies.rest.model.VocabularyTerm> vocabularyTermRepList);

}
