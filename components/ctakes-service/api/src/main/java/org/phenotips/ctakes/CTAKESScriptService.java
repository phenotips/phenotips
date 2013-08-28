/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.phenotips.ctakes;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.ctakes.chunker.ae.Chunker;
import org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster;
import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.CopyAnnotator;
import org.apache.ctakes.core.ae.OverlapAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.resource.FileResource;
import org.apache.ctakes.core.resource.FileResourceImpl;
import org.apache.ctakes.core.resource.LuceneIndexReaderResourceImpl;
import org.apache.ctakes.dictionary.lookup.ae.DictionaryLookupAnnotator;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.ctakes.typesystem.type.textsem.EntityMention;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.impl.ExternalResourceDependency_impl;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.ConfigurationParameterFactory;
import org.uimafit.factory.ExternalResourceFactory;
import org.uimafit.factory.ResourceCreationSpecifierFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.script.service.ScriptService;

/** 
 * Provides access for handling Apache cTAKES API.
 * @version $Id$
 * */
@Component
@Named("ctakes")
@Singleton
public class CTAKESScriptService implements ScriptService, Initializable
{
    
    /** String lookup window for future multiple uses. */
    private static final String LOOKUP_WINDOW_PATH = 
            "org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation";

    /** String id for future multiple uses. */
    private static final String ID = "id";

    /** String term for future multiple uses. */
    private static final String TERM = "term";
    
    /** String text for future multiple uses. */
    private static final String TEXT = "text";
    
    /** String desc for future multiple uses. */
    private static final String DESC = "desc";
    
    /** String initial path for future multiple uses. */
    private static final String INIT_PATH = 
            "webapps/phenotips/resources/cTAKES/";
    
    /** Analysis Engine for extraction of terms from Doctor's text. */
    private AnalysisEngine analysisEng;
    
    /** File Resource Class. */
    private Class<FileResource> fileResClass = 
            org.apache.ctakes.core.resource.FileResource.class;
   
    /** Implementation of File Resource Class. */
    private Class<FileResourceImpl> fileResClassImpl = 
            org.apache.ctakes.core.resource.FileResourceImpl.class;

    @Override
    public final void initialize() throws InitializationException {
        String tsdst = "org.apache.ctakes.typesystem.types.TypeSystem";
        TypeSystemDescription tsd =
                TypeSystemDescriptionFactory.createTypeSystemDescription(tsdst);

        try {
            //Segmentation
            Class<SimpleSegmentAnnotator> ssA = SimpleSegmentAnnotator.class;
            AnalysisEngineDescription simpleSegmentDesc =
                    AnalysisEngineFactory.createPrimitiveDescription(ssA);

            //Tokenizer
            Class<TokenizerAnnotatorPTB> taC = TokenizerAnnotatorPTB.class;
            AnalysisEngineDescription tokenizerDesc =
                    AnalysisEngineFactory.createPrimitiveDescription(taC, tsd);

            //Sentence Detection 
            String senturl = INIT_PATH 
                    + "SentenceDetection/sd-med-model.zip";
            AnalysisEngineDescription sentDetectDesc =
                    AnalysisEngineFactory.createPrimitiveDescription(
                            SentenceDetector.class, 
                            SentenceDetector.SD_MODEL_FILE_PARAM, senturl);


            //context dependent tokenizer
            Class<ContextDependentTokenizerAnnotator> cdta =
                    ContextDependentTokenizerAnnotator.class;
            AnalysisEngineDescription contextDependentTokenizerDesc =
                    AnalysisEngineFactory.createPrimitiveDescription(cdta);

            //POS Tagger
            String posresfile = INIT_PATH 
                    + "POSTagger/mayo-pos.zip";
            AnalysisEngineDescription posTagdesc =
                    AnalysisEngineFactory.createPrimitiveDescription(
                            POSTagger.class, tsd,
                            POSTagger.POS_MODEL_FILE_PARAM, posresfile);


            //Chunker
            String chunkfileres = INIT_PATH
                    + "Chunker/chunk-model-claims-1-5.zip";
            String chunkurl = new File(chunkfileres).toURI().toURL().toString();
            String chunkp = "org.apache.ctakes.chunker.ae.DefaultChunkCreator";
            ExternalResourceDescription chunkererd =
                    ExternalResourceFactory.createExternalResourceDescription(
                            "ChunkerModelFile", fileResClassImpl, chunkurl);
            String chunkerModel = "ChunkerModel";
            AnalysisEngineDescription chunkerDesc =
                    AnalysisEngineFactory.createPrimitiveDescription(
                            Chunker.class, tsd,
                            Chunker.CHUNKER_MODEL_FILE_PARAM, chunkfileres,
                            Chunker.CHUNKER_CREATOR_CLASS_PARAM, chunkp,
                            chunkerModel, chunkererd);

            ExternalResourceFactory.createDependency(
                    chunkerDesc, chunkerModel, fileResClass);

            //Chunk Adjuster NN
            String nn = "NN";
            String[] chunkPattern = new String[2];
            chunkPattern[0] = nn;
            chunkPattern[1] = nn;
            AnalysisEngineDescription chunkAdjusterDesc =
                    AnalysisEngineFactory.createPrimitiveDescription(
                            ChunkAdjuster.class, null,
                            ChunkAdjuster.PARAM_CHUNK_PATTERN, chunkPattern,
                            ChunkAdjuster.PARAM_EXTEND_TO_INCLUDE_TOKEN, 1);

            //chunk adjuster PN
            String[] chunkPatternPN = new String[3];
            chunkPatternPN[0] = nn;
            chunkPatternPN[1] = "PN";
            chunkPatternPN[2] = nn;
            AnalysisEngineDescription chunkAdjusterPNDesc =
                    AnalysisEngineFactory.createPrimitiveDescription(
                            ChunkAdjuster.class, null,
                            ChunkAdjuster.PARAM_EXTEND_TO_INCLUDE_TOKEN, 2,
                            ChunkAdjuster.PARAM_CHUNK_PATTERN, chunkPatternPN);

            //Lookup Annotators
            //Overlap Annotators
            AnalysisEngineDescription overlapdesc = getOverlapAnnotatorDesc();
            
            //Copy Annotator - Lookup
            AnalysisEngineDescription copyADesc = getCopyAnnotatorDesc();
            
            //Dictionary Lookup
            AnalysisEngineDescription dictlookupDesc = getDictlookupDesc();
            
            //Final Analysis Engine Description
            List<AnalysisEngineDescription> aedList =
                    new ArrayList<AnalysisEngineDescription>();
            List<String> components = new ArrayList<String>();

            aedList.add(simpleSegmentDesc);
            components.add("SimpleSegmentAnnotator");
            aedList.add(sentDetectDesc);
            components.add("SentenceDetector");
            aedList.add(tokenizerDesc);
            components.add("TokenizerAnnotator");
            aedList.add(contextDependentTokenizerDesc);
            components.add("ContextDependentTokenizer");
            aedList.add(posTagdesc);
            components.add("POSTagger");
            aedList.add(chunkerDesc);
            components.add("Chunker");
            aedList.add(chunkAdjusterDesc);
            components.add("AdjustNounPhraseToIncludeFollowingNP");
            aedList.add(chunkAdjusterPNDesc);
            components.add("AdjustNounPhraseToIncludeFollowingPPNP");
            aedList.add(overlapdesc);
            components.add("OverlapAnnotator-Lookup");
            aedList.add(copyADesc);
            components.add("CopyAnnotator-Lookup");
            aedList.add(dictlookupDesc);
            components.add("Dictionary Lookup");

            // Create the Analysis Engine
            AnalysisEngineDescription analysisEngdesc = 
                    AnalysisEngineFactory.createAggregateDescription(
                            aedList, components, 
                            null, null, null, null);
            ConfigurationParameter[] finalDescConfParam = 
                    new ConfigurationParameter[1];
            ConfigurationParameter chunkCreatorClass = 
                    ConfigurationParameterFactory.createPrimitiveParameter(
                            "ChunkCreatorClass", String.class, DESC, true);
            finalDescConfParam[0] = chunkCreatorClass;
            Object[] finalDescConfVals = new Object[1];
            finalDescConfVals[0] = "org.apache.ctakes.chunker.ae.PhraseTypeChunkCreator";

            ResourceCreationSpecifierFactory.setConfigurationParameters(
                    analysisEngdesc, finalDescConfParam, finalDescConfVals);
                        
            analysisEng = AnalysisEngineFactory.createAggregate(
                    analysisEngdesc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /** 
     * Method to get Dictionary Lookup Annotator.
     * @return DictionaryLookupDescription
     * @throws ResourceInitializationException on unavailability of resource
     * @throws MalformedURLException on unavailability of file resource
     * */
    private AnalysisEngineDescription getDictlookupDesc()
        throws ResourceInitializationException, MalformedURLException {
        final String useMemoryIndex = "UseMemoryIndex";
        final String indexDirectory = "IndexDirectory";
        ConfigurationParameter[] dictconfParam = new ConfigurationParameter[1];
        ConfigurationParameter maxListSize =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "maxListSize", Integer.class, DESC, false);
        dictconfParam[0] = maxListSize;

        Object[] dictconfVals = new Object[1];
        dictconfVals[0] = 2147483647;

        String fileurl = new File(INIT_PATH
                + "DictionaryLookup/LookupDesc_csv_sample.xml")
                   .toURI().toURL().toString();
        String dicturl = new File(INIT_PATH
                + "DictionaryLookup/dictionary1.csv")
                   .toURI().toURL().toString();
        String drugurl = INIT_PATH
                + "DictionaryLookup/drug_index";
        String orangeurl = INIT_PATH
                + "DictionaryLookup/OrangeBook";
        
        Class<LuceneIndexReaderResourceImpl> luceneResClassImpl =
                org.apache.ctakes.core.resource.LuceneIndexReaderResourceImpl.class;
        String luceneResClassStr =
                "org.apache.ctakes.core.resource.LuceneIndexReaderResource";
        String fileResClassStr =
                "org.apache.ctakes.core.resource.FileResource";

        ExternalResourceDescription dictERD1 =
                ExternalResourceFactory.createExternalResourceDescription(
                        "LookupDescriptorFile", fileResClassImpl, fileurl);
        ExternalResourceDescription dictionaryERD2 =
                ExternalResourceFactory.createExternalResourceDescription(
                        "DictionaryFileResource", fileResClassImpl, dicturl);
        ExternalResourceDescription dictERD3 =
                ExternalResourceFactory.createExternalResourceDescription(
                        "RxnormIndex", luceneResClassImpl, "",
                        useMemoryIndex, true, indexDirectory, drugurl);
        ExternalResourceDescription dictERD4 =
                ExternalResourceFactory.createExternalResourceDescription(
                        "OrangeBookIndex", luceneResClassImpl, "" ,
                        useMemoryIndex, true, indexDirectory, orangeurl);
        String lookupDesc = "LookupDescriptor";
        String dictFile = "DictionaryFile";
        String rxIndexReader = "RxnormIndexReader";
        String orangeIndexReader = "OrangeBookIndexReader";
        Map<String, ExternalResourceDescription> dictMap =
                new HashMap<String, ExternalResourceDescription>();
        dictMap.put(lookupDesc, dictERD1);
        dictMap.put(dictFile, dictionaryERD2);
        dictMap.put(rxIndexReader, dictERD3);
        dictMap.put(orangeIndexReader, dictERD4);

        AnalysisEngineDescription dictAED =
                AnalysisEngineFactory.createPrimitiveDescription(
                        DictionaryLookupAnnotator.class, null, null, null, null,
                        dictconfParam, dictconfVals, dictMap);

        ExternalResourceDependency[] dictD = new ExternalResourceDependency[4];
        ExternalResourceDependency dicterd1 =
                new ExternalResourceDependency_impl();
        dicterd1.setKey(lookupDesc); dicterd1.setOptional(false);
        dicterd1.setInterfaceName(fileResClassStr);
        ExternalResourceDependency dicterd2 =
                new ExternalResourceDependency_impl();
        dicterd2.setKey(dictFile); dicterd2.setOptional(false);
        dicterd2.setInterfaceName(fileResClassStr);
        ExternalResourceDependency dicterd3 =
                new ExternalResourceDependency_impl();
        dicterd3.setKey(rxIndexReader); dicterd3.setOptional(false);
        dicterd3.setInterfaceName(luceneResClassStr);
        ExternalResourceDependency dicterd4 =
                new ExternalResourceDependency_impl();
        dicterd4.setKey(orangeIndexReader); dicterd4.setOptional(false);
        dicterd4.setInterfaceName(luceneResClassStr);
        dictD[0] = dicterd1; dictD[1] = dicterd2;
        dictD[2] = dicterd3; dictD[3] = dicterd4;
        dictAED.setExternalResourceDependencies(dictD);

        return dictAED;

    }

    /** 
     * Method to get Copy Annotator.
     * @return CopyAnnotatorDescription
     * @throws ResourceInitializationException on unavailability of resource
     * */
    private AnalysisEngineDescription getCopyAnnotatorDesc()
        throws ResourceInitializationException {

        String uimaAnnotation = "uima.tcas.Annotation";
        TypeSystemDescription copyATsd = new TypeSystemDescription_impl();
        copyATsd.addType("org.apache.ctakes.typesystem.type.CopySrcAnnotation",
                null, uimaAnnotation);
        copyATsd.addType("org.apache.ctakes.typesystem.type.CopyDestAnnotation",
                null, uimaAnnotation);
        ConfigurationParameter[] copyAconfParam = new ConfigurationParameter[3];
        ConfigurationParameter srcObjClass =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "srcObjClass", String.class, DESC, true);
        ConfigurationParameter destObjClass =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "destObjClass", String.class, DESC, true);
        ConfigurationParameter dataBindMap =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "dataBindMap", String.class, DESC, true);
        dataBindMap.setMultiValued(true);
        copyAconfParam[0] = srcObjClass;
        copyAconfParam[1] = destObjClass;
        copyAconfParam[2] = dataBindMap;

        Object[] copyAconfVals = new Object[3];
        copyAconfVals[0] = "org.apache.ctakes.typesystem.type.syntax.NP";
        copyAconfVals[1] = LOOKUP_WINDOW_PATH;
        String[] dataBindArray = new String[2];
        dataBindArray[0] = "getBegin|setBegin";
        dataBindArray[1] = "getEnd|setEnd";
        copyAconfVals[2] = dataBindArray;

        Class<CopyAnnotator> copyAclass = CopyAnnotator.class;
        AnalysisEngineDescription copyAnnotatorDesc =
                AnalysisEngineFactory.createPrimitiveDescription(
                        copyAclass, copyATsd, null, null, null,
                        copyAconfParam, copyAconfVals);
        return copyAnnotatorDesc;
    }

    /** 
     * Method to get Overlap Annotator.
     * @return OverlapAnnotatorDescription
     * @throws ResourceInitializationException on unavailability of resource
     * */
    private AnalysisEngineDescription getOverlapAnnotatorDesc()
        throws ResourceInitializationException {

        ConfigurationParameter[] configurationParameters =
                new ConfigurationParameter[5];
        ConfigurationParameter actionType =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "ActionType", String.class, DESC, true);
        ConfigurationParameter aObjectClass =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "A_ObjectClass", String.class, DESC, true);
        ConfigurationParameter bObjectClass =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "B_ObjectClass", String.class, DESC, true);
        ConfigurationParameter overlaptype =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "OverlapType", String.class, DESC, true);
        ConfigurationParameter deleteaction =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "DeleteAction", String.class, DESC, true);

        deleteaction.setMultiValued(true);
        configurationParameters[0] = aObjectClass;
        configurationParameters[1] = bObjectClass;
        configurationParameters[2] = overlaptype;
        configurationParameters[3] = actionType;
        configurationParameters[4] = deleteaction;

        Object[] configVals = new Object[5];

        configVals[0] =
                LOOKUP_WINDOW_PATH;
        configVals[1] =
                LOOKUP_WINDOW_PATH;
        configVals[2] = "A_ENV_B";
        configVals[3] = "DELETE";
        String[] deleteActionArray = new String[1];
        deleteActionArray[0] = "selector=B";
        configVals[4] = deleteActionArray;

        Class<OverlapAnnotator> overlapAnnot = OverlapAnnotator.class;
        AnalysisEngineDescription overlapdesc =
                AnalysisEngineFactory.createPrimitiveDescription(overlapAnnot,
                        null, null, null, null,
                        configurationParameters, configVals);

        return overlapdesc;
    }

    /** 
     * Method to get list of extracted terms from the textual notes.
     * @param input the text entered 
     * @return list of Extracted Terms
     * @throws ResourceInitializationException if any of the resource for Annotators is missing
     * @throws AnalysisEngineProcessException if unable to process the textual input
     * @throws IOException if unable to open the index-directory
     * @throws ParseException if unable to parse the lucene index
     * */
    public final List<Map<String, Object>> extract(String input)
        throws ResourceInitializationException, AnalysisEngineProcessException, IOException, ParseException {

        Map<String, ExtractedTerm> finalTerms =
                new HashMap<String, ExtractedTerm>();
        
        List<Map<String, Object>> tobeReturned = 
                new ArrayList<Map<String, Object>>();
        
        Map<String, String> hpoTerm = new HashMap<String, String>();
        
        
        JCas jCas  =  analysisEng.newJCas();
        jCas.setDocumentText(input.toLowerCase());
        analysisEng.process(jCas);

        FSIndex<Annotation> entityIndex  =
                jCas.getAnnotationIndex(EntityMention.type);
        Iterator<Annotation> entityIter  =  entityIndex.iterator();
        
        EntityMention firstentity = (EntityMention) entityIter.next();
        ExtractedTerm firstTerm  =  new ExtractedTerm();
        firstTerm.setExtractedText(firstentity.getCoveredText());
        firstTerm.setBeginIndex(firstentity.getBegin());
        firstTerm.setEndIndex(firstentity.getEnd());
        hpoTerm = getHPOTerm(firstentity.getCoveredText());
        firstTerm.setId(hpoTerm.get(ID));
        firstTerm.setTerm(hpoTerm.get(TERM));
        finalTerms.put(firstentity.getCoveredText(), firstTerm);

        while (entityIter.hasNext())  {
            int flag = 1;
            EntityMention entity = (EntityMention) entityIter.next();
            int tempBegin  =  entity.getBegin();
            for (Entry<String, ExtractedTerm> entry : finalTerms.entrySet()) {
                if (tempBegin >=  entry.getValue().getBeginIndex()
                        && tempBegin <= entry.getValue().getEndIndex()) {
                    flag = 0;
                    break;
                }
                if (entity.getBegin() == entry.getValue().getEndIndex() + 1) {
                    flag = 0;
                    ExtractedTerm temp = new ExtractedTerm();
                    temp.setEndIndex(entity.getEnd());
                    temp.setBeginIndex(entry.getValue().getBeginIndex());
                    temp.setExtractedText(entry.getKey()
                            + " " + entity.getCoveredText());
                    hpoTerm = getHPOTerm(entry.getKey() 
                            + " " + entity.getCoveredText());
                    temp.setId(hpoTerm.get(ID));
                    temp.setTerm(hpoTerm.get(TERM));
                    finalTerms.remove(entry.getKey());
                    finalTerms.put(entry.getKey()
                            + " " + entity.getCoveredText(), temp);
                    break;
                }
            }
            if (flag == 1) {
                ExtractedTerm temp  =  new ExtractedTerm();
                temp.setEndIndex(entity.getEnd());
                temp.setBeginIndex(entity.getBegin());
                temp.setExtractedText(entity.getCoveredText());
                hpoTerm = getHPOTerm(entity.getCoveredText());
                temp.setId(hpoTerm.get(ID));
                temp.setTerm(hpoTerm.get(TERM));
                finalTerms.put(entity.getCoveredText(), temp);
            }
        }
        
        for (Entry<String, ExtractedTerm> entry : finalTerms.entrySet()) {
            Map<String, Object> temp = new HashMap<String, Object>();
            temp.put(ID, entry.getValue().getId());
            temp.put(TERM, entry.getValue().getTerm());
            temp.put(TEXT, entry.getValue().getExtractedText());
            temp.put("begin", entry.getValue().getBeginIndex());
            temp.put("end", entry.getValue().getEndIndex());
            
            tobeReturned.add(temp);
        }
        return tobeReturned;
    }

    /** 
     * Method to get the HPO term ID and actual term for the extracted text.
     * @param coveredText the extracted text
     * @return Map of HPO ID and HPO TERM
     * @throws IOException if directory not present
     * @throws ParseException if unable to parse the file
     * */
    private Map<String, String> getHPOTerm(String coveredText) throws IOException, ParseException {
        Map<String, String> hpoTerm = new HashMap<String, String>();
        Directory directory = FSDirectory.open(new File(INIT_PATH
                + "index-directory"));
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
       
        QueryParser parser = new QueryParser(
                Version.LUCENE_40, TEXT,
                new StandardAnalyzer(Version.LUCENE_40));
        
        String search = coveredText;
        Query query = parser.parse(search);
        TopDocs topdocs = searcher.search(query, 2);
        ScoreDoc[] hits = topdocs.scoreDocs; 
        for (ScoreDoc hit: hits) {
            Document documentFromSearcher = searcher.doc(hit.doc);
            hpoTerm.put(ID, documentFromSearcher.get("code"));
            hpoTerm.put(TERM, documentFromSearcher.get(TEXT));
            break;
        }

        directory.close();
        return hpoTerm;
    }
}
