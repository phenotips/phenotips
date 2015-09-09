package org.phenotips.vocabularies.rest;

import org.phenotips.vocabularies.rest.model.VocabulariesRep;
import org.phenotips.vocabularies.rest.model.VocabularyRep;
import org.phenotips.vocabularies.rest.model.VocabularyTermRep;
import org.phenotips.vocabularies.rest.model.VocabularyTermsRep;
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
    VocabularyRep createVocabularyRepresentation(Vocabulary vocabulary);

    VocabulariesRep createVocabulariesRepresentation(List<VocabularyRep> vocabularyRepList);

    VocabularyTermRep createVocabularyTermRepresentation(VocabularyTerm term);

    VocabularyTermsRep createVocabularyTermsRepresentation(List<VocabularyTermRep> vocabularyTermRepList);

}
