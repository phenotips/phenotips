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

import org.phenotips.vocabulary.MachineTranslator;
import org.phenotips.vocabulary.VocabularyExtension;
import org.phenotips.vocabulary.VocabularyInputTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocalizationContext;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
     * The name field.
     */
    private static final String NAME = "name";

    /**
     * The definition field.
     */
    private static final String DEF = "def";

    /**
     * The format to add a language to a solr field.
     */
    private static final String FIELD_FORMAT = "%s_%s";

    /**
     * A map going from the names of properties in the solr index to the names of properties
     * in the xliff file.
     */
    private static final Map<String, String> PROP_MAP;

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
     * The machine translator.
     */
    @Inject
    private MachineTranslator translator;

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

    /**
     * Whether this translation is working at all.
     */
    private boolean enabled;

    static {
        PROP_MAP = new HashMap<>(2);
        PROP_MAP.put(NAME, "label");
        PROP_MAP.put(DEF, "definition");
    }

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
        enabled = false;
        lang = localizationContext.getCurrentLocale().getLanguage();
        if (shouldMachineTranslate(vocabulary)) {
            translator.loadVocabulary(vocabulary);
        }
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
            logger.error("indexingStarted exception " + e.getMessage());
            return;
        }
        /* Everything worked out, enable it */
        enabled = true;
    }

    @Override
    public void indexingEnded(String vocabulary)
    {
        /* This thing holds a huge dictionary inside it, so we don't want java to have any qualms
         * about garbage collecting it. */
        xliff = null;
        if (shouldMachineTranslate(vocabulary)) {
            translator.unloadVocabulary(vocabulary);
        }
        enabled = false;
    }

    @Override
    public void extendTerm(VocabularyInputTerm term, String vocabulary)
    {
        if (!enabled) {
            return;
        }
        String id = term.getId();
        String label = xliff.getFirstString(id, PROP_MAP.get(NAME));
        String definition = xliff.getFirstString(id, PROP_MAP.get(DEF));
        Collection<String> fields = new ArrayList<>(2);
        if (label != null) {
            term.set(String.format(FIELD_FORMAT, NAME, lang), label);
        } else {
            /* This is not meant to be the PROP_MAP.get(NAME) because it's the field that
             * the machine translator (not the official HPO xliff sheet) knows this field by,
             * which has no reason not to be the same field that we use. */
            fields.add(NAME);
        }
        if (definition != null) {
            term.set(String.format(FIELD_FORMAT, DEF, lang), definition);
        } else {
            fields.add(DEF);
        }
        if (shouldMachineTranslate(vocabulary)) {
            translator.translate(vocabulary, term, fields);
        }
    }

    /**
     * Return whether we should run the vocabulary given through a machine tranlsator.
     *
     * @param vocabulary the vocabulary
     * @return whether it should be machine translated.
     */
    private boolean shouldMachineTranslate(String vocabulary)
    {
        return translator.getSupportedLanguages().contains(lang)
            && translator.getSupportedVocabularies().contains(vocabulary);
    }
}
