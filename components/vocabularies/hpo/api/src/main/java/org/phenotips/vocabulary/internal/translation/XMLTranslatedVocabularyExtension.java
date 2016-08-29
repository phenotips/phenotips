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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

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
     * The supported languages.
     * Will be set when we start indexing so the component supports dynamic switching.
     */
    private Set<String> languages;

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
     * The deserialized xliffs.
     */
    private XLiffMap translations = new XLiffMap();

    /**
     * The localization context.
     */
    @Inject
    private LocalizationContext localizationContext;

    /**
     * The context provider.
     */
    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * The xwiki object.
     */
    private XWiki xwiki;

    /**
     * The context.
     */
    private XWikiContext context;

    /**
     * An xml mapper.
     */
    private XmlMapper mapper = new XmlMapper();

    static {
        PROP_MAP = new HashMap<>(2);
        PROP_MAP.put(NAME, "label");
        PROP_MAP.put(DEF, "definition");
    }

    /**
     * Get the supported languages as a set.
     *
     * @return the languages
     */
    private Set<String> getLanguages()
    {
        String languageString = xwiki.getXWikiPreference("languages",
                localizationContext.getCurrentLocale().getLanguage(), context);
        String[] split = languageString.split(",");
        Set<String> retval = new HashSet<>(split.length);
        for (String lang : split) {
            if (lang == null) {
                continue;
            }
            String trimmed = lang.trim();
            if (!"".equals(trimmed) && !"en".equals(trimmed)) {
                retval.add(trimmed);
            }
        }
        return retval;
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
        context = contextProvider.get();
        xwiki = context.getWiki();
        languages = getLanguages();
        /* We're gonna go over the languages and if something goes wrong, disable
         * that language */
        Iterator<String> iterator = languages.iterator();
        while (iterator.hasNext()) {
            String lang = iterator.next();
            if (shouldMachineTranslate(vocabulary, lang)) {
                translator.loadVocabulary(vocabulary, lang);
            }
            String xml = String.format(TRANSLATION_XML_FORMAT, vocabulary, lang);
            try {
                InputStream inStream = this.getClass().getResourceAsStream(xml);
                if (inStream == null) {
                    /* parse will strangely throw a malformed url exception if this is null, which
                     * is impossible to distinguish from an actual malformed url exception,
                     * so check here and prevent going forward if there's no translation */
                    logger.warn(String.format("Could not find resource %s", xml));
                    iterator.remove();
                    continue;
                }
                translations.put(vocabulary, lang, mapper.readValue(inStream, XLiffFile.class));
                inStream.close();
            } catch (IOException e) {
                logger.error("indexingStarted exception " + e.getMessage());
                iterator.remove();
                continue;
            }
        }
    }

    @Override
    public void indexingEnded(String vocabulary)
    {
        for (String lang : languages) {
            if (shouldMachineTranslate(vocabulary, lang)) {
                translator.unloadVocabulary(vocabulary, lang);
            }
            translations.remove(vocabulary, lang);
        }
        languages.clear();
    }

    @Override
    public void extendTerm(VocabularyInputTerm term, String vocabulary)
    {
        for (String lang : languages) {
            XLiffFile xliff = translations.get(vocabulary, lang);
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
            if (shouldMachineTranslate(vocabulary, lang)) {
                translator.translate(vocabulary, lang, term, fields);
            }
        }
    }

    /**
     * Return whether we should run the vocabulary given through a machine tranlsator.
     *
     * @param vocabulary the vocabulary
     * @param lang the language
     * @return whether it should be machine translated.
     */
    private boolean shouldMachineTranslate(String vocabulary, String lang)
    {
        return translator.getSupportedLanguages().contains(lang)
            && translator.getSupportedVocabularies().contains(vocabulary);
    }
}
