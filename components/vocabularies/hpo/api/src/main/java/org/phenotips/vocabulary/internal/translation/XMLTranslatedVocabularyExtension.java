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
package org.phenotips.vocabulary.internal.translation;

import org.phenotips.vocabulary.VocabularyExtension;
import org.phenotips.vocabulary.VocabularyInputTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocalizationContext;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Implements {@link VocabularyExtension} to provide translation services.
 * Works with the xml files up at https://github.com/Human-Phenotype-Ontology/HPO-translations
 *
 * @version $Id$
 */
@Component
@Singleton
public class XMLTranslatedVocabularyExtension implements VocabularyExtension
{
    /**
     * The format for the file containing the translation - where the first string is the ontology
     * name and the second is the language code.
     */
    private static final String TRANSLATION_XML_FORMAT = "%s_%s.xliff";

    /* TODO: I don't like having these field names down here, I'd rather they were
     * somehow set by the vocabulary input term, but that'd require coupling the VocabularyInputTerm
     * to the concept of a language which makes me uncomfortable too */

    /**
     * The format for the name field.
     */
    private static final String NAME_FORMAT = "name_%s";

    /**
     * The format for the definition field.
     */
    private static final String DEF_FORMAT = "def_%s";

    /**
     * The format for the synonym field.
     */
    private static final String SYNONYM_FORMAT = "synonym_%s";

    /**
     * The current language. Will be set when we start indexing so that
     * the component supports dynamically switching without restarting phenotips.
     */
    private String lang;

    /**
     * The logger.
     */
    @Inject
    private Logger logger;

    /**
     * The deserialized xliff.
     */
    private XLiffFile xliff;

    /**
     * The localization context.
     */
    @Inject
    private LocalizationContext localizationContext;

    /**
     * An xml mapper.
     */
    private XmlMapper mapper = new XmlMapper();

    @Override
    public Collection<String> getSupportedVocabularies()
    {
        Collection<String> retval = new ArrayList<>();
        retval.add("hpo");
        return retval;
    }

    @Override
    public void indexingStarted(String vocabulary)
    {
        lang = localizationContext.getCurrentLocale().getLanguage();
        String xml = String.format(TRANSLATION_XML_FORMAT, vocabulary, lang);
        try {
            InputStream inStream = this.getClass().getResourceAsStream(xml);
            if (inStream == null) {
                /* parse will strangely throw a malformed url exception if this is null, which
                 * is impossible to distinguish from an actual malformed url exception,
                 * so check here and prevent going forward if there's no translation */
                logger.warn(String.format("Could not find resource %s", xml));
                return;
            }
            xliff = mapper.readValue(inStream, XLiffFile.class);
            inStream.close();
        } catch (IOException e) {
            throw new RuntimeException("indexingStarted exception", e);
        }
    }

    @Override
    public void indexingEnded(String vocabulary)
    {
        /* This thing holds a huge dictionary inside it, so we don't want java to have any qualms
         * about garbage collecting it. */
        xliff = null;
    }

    @Override
    public void extendTerm(VocabularyInputTerm term, String vocabulary)
    {
        String id = term.getId();
        String label = xliff.getString(id, "label");
        String definition = xliff.getString(id, "definition");
        if (label != null) {
            term.set(String.format(NAME_FORMAT, lang), label);
        }
        if (definition != null) {
            term.set(String.format(DEF_FORMAT, lang), definition);
        }
        /* TODO Else clauses that dynamically machine-translate the missing stuff (or get it from
         * a cache so we don't spend our lives translating).
         */
    }
}
