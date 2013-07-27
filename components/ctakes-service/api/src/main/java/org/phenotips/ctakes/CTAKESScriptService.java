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
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.impl.ExternalResourceDependency_impl;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.ConfigurationParameterFactory;
import org.uimafit.factory.ExternalResourceFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.script.service.ScriptService;
@Component
@Named("ctakes")
@Singleton
public class CTAKESScriptService implements ScriptService, Initializable
{

    private AnalysisEngine analysisEng;
    private Class<FileResource> fileResClass = 
            org.apache.ctakes.core.resource.FileResource.class;
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
            String senturl = "file:webapps/phenotips/resources/cTAKES/"
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
            String posresfile = "webapps/phenotips/resources/cTAKES/"
                    + "POSTagger/mayo-pos.zip";
            AnalysisEngineDescription posTagdesc =
                    AnalysisEngineFactory.createPrimitiveDescription(
                            POSTagger.class, tsd,
                            POSTagger.POS_MODEL_FILE_PARAM, posresfile);


            //Chunker
            String chunkfileres = "webapps/phenotips/resources/cTAKES/"
                    + "Chunker/chunk-model-claims-1-5.zip";
            String chunkurl = "file:" + chunkfileres;
            String chunkp = "org.apache.ctakes.chunker.ae.DefaultChunkCreator";
            ExternalResourceDescription chunkererd =
                    ExternalResourceFactory.createExternalResourceDescription(
                            "ChunkerModelFile", fileResClassImpl, chunkurl);

            AnalysisEngineDescription chunkerDesc =
                    AnalysisEngineFactory.createPrimitiveDescription(
                            Chunker.class, tsd,
                            Chunker.CHUNKER_MODEL_FILE_PARAM, chunkfileres,
                            Chunker.CHUNKER_CREATOR_CLASS_PARAM, chunkp,
                            "ChunkerModel", chunkererd);

            ExternalResourceFactory.createDependency(
                    chunkerDesc, "ChunkerModel", fileResClass);

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
            analysisEng = AnalysisEngineFactory.createAggregate(
                    aedList, components, null, null, null);
            System.out.println("Analysis Engine Initialised");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    private AnalysisEngineDescription getDictlookupDesc()
        throws ResourceInitializationException, MalformedURLException {
        //Dictionary Lookup
        ConfigurationParameter[] dictconfParam = new ConfigurationParameter[1];
        ConfigurationParameter maxListSize =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "maxListSize", Integer.class, "desc", false);
        dictconfParam[0] = maxListSize;

        Object[] dictconfVals = new Object[1];
        dictconfVals[0] = 27;

        String fileurl = "file:webapps/phenotips/resources/cTAKES/"
                + "DictionaryLookup/LookupDesc_csv_sample.xml";
        String dicturl = "file:webapps/phenotips/resources/cTAKES/"
                + "DictionaryLookup/dictionary1.csv";
        String drugurl = "webapps/phenotips/resources/cTAKES/"
                + "DictionaryLookup/drug_index";
        String orangeurl = "webapps/phenotips/resources/cTAKES/"
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
                        "UseMemoryIndex", true, "IndexDirectory", drugurl);
        ExternalResourceDescription dictERD4 =
                ExternalResourceFactory.createExternalResourceDescription(
                        "OrangeBookIndex", luceneResClassImpl, "" ,
                        "UseMemoryIndex", true, "IndexDirectory", orangeurl);

        Map<String, ExternalResourceDescription> dictMap =
                new HashMap<String, ExternalResourceDescription>();
        dictMap.put("LookupDescriptor", dictERD1);
        dictMap.put("DictionaryFile", dictionaryERD2);
        dictMap.put("RxnormIndexReader", dictERD3);
        dictMap.put("OrangeBookIndexReader", dictERD4);

        AnalysisEngineDescription dictAED =
                AnalysisEngineFactory.createPrimitiveDescription(
                        DictionaryLookupAnnotator.class, null, null, null, null,
                        dictconfParam, dictconfVals, dictMap);

        ExternalResourceDependency[] dictD = new ExternalResourceDependency[4];
        ExternalResourceDependency dicterd1 =
                new ExternalResourceDependency_impl();
        dicterd1.setKey("LookupDescriptor"); dicterd1.setOptional(false);
        dicterd1.setInterfaceName(fileResClassStr);
        ExternalResourceDependency dicterd2 =
                new ExternalResourceDependency_impl();
        dicterd2.setKey("DictionaryFile"); dicterd2.setOptional(false);
        dicterd2.setInterfaceName(fileResClassStr);
        ExternalResourceDependency dicterd3 =
                new ExternalResourceDependency_impl();
        dicterd3.setKey("RxnormIndexReader"); dicterd3.setOptional(false);
        dicterd3.setInterfaceName(luceneResClassStr);
        ExternalResourceDependency dicterd4 =
                new ExternalResourceDependency_impl();
        dicterd4.setKey("OrangeBookIndexReader"); dicterd4.setOptional(false);
        dicterd4.setInterfaceName(luceneResClassStr);
        dictD[0] = dicterd1; dictD[1] = dicterd2;
        dictD[2] = dicterd3; dictD[3] = dicterd4;
        dictAED.setExternalResourceDependencies(dictD);

        return dictAED;

    }

    private AnalysisEngineDescription getCopyAnnotatorDesc()
        throws ResourceInitializationException {

        //Copy Annotator - Lookup
        TypeSystemDescription copyATsd = new TypeSystemDescription_impl();
        copyATsd.addType("org.apache.ctakes.typesystem.type.CopySrcAnnotation",
                null, "uima.tcas.Annotation");
        copyATsd.addType("org.apache.ctakes.typesystem.type.CopyDestAnnotation",
                null, "uima.tcas.Annotation");
        ConfigurationParameter[] copyAconfParam = new ConfigurationParameter[3];
        ConfigurationParameter srcObjClass =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "srcObjClass", String.class, "desc", true);
        ConfigurationParameter destObjClass =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "destObjClass", String.class, "desc", true);
        ConfigurationParameter dataBindMap =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "dataBindMap", String.class, "desc", true);
        dataBindMap.setMultiValued(true);
        copyAconfParam[0] = srcObjClass;
        copyAconfParam[1] = destObjClass;
        copyAconfParam[2] = dataBindMap;

        Object[] copyAconfVals = new Object[3];
        copyAconfVals[0] = "org.apache.ctakes.typesystem.type.syntax.NP";
        copyAconfVals[1] =
                "org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation";
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

    private AnalysisEngineDescription getOverlapAnnotatorDesc()
        throws ResourceInitializationException {

        ConfigurationParameter[] configurationParameters =
                new ConfigurationParameter[5];
        ConfigurationParameter actionType =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "ActionType", String.class, "desc", true);
        ConfigurationParameter aObjectClass =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "A_ObjectClass", String.class, "desc", true);
        ConfigurationParameter bObjectClass =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "B_ObjectClass", String.class, "desc", true);
        ConfigurationParameter overlaptype =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "OverlapType", String.class, "desc", true);
        ConfigurationParameter deleteaction =
                ConfigurationParameterFactory.createPrimitiveParameter(
                        "DeleteAction", String.class, "desc", true);

        deleteaction.setMultiValued(true);
        configurationParameters[0] = aObjectClass;
        configurationParameters[1] = bObjectClass;
        configurationParameters[2] = overlaptype;
        configurationParameters[3] = actionType;
        configurationParameters[4] = deleteaction;

        Object[] configVals = new Object[5];

        configVals[0] =
                "org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation";
        configVals[1] =
                "org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation";
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

    public final Map<String, ExtractedTerm> extract(String input)
        throws ResourceInitializationException, AnalysisEngineProcessException {

        Map<String, ExtractedTerm> finalTerms =
                new HashMap<String, ExtractedTerm>();
        JCas jCas  =  analysisEng.newJCas();
        jCas.setDocumentText(input.toLowerCase());
        analysisEng.process(jCas);

        FSIndex<Annotation> eventIndex  =
                jCas.getAnnotationIndex(org.apache.ctakes.typesystem.type.textsem.MedicationEventMention.type);
        Iterator<Annotation> eventIter  =  eventIndex.iterator();

        EventMention firstevent  =  (EventMention) eventIter.next();
        ExtractedTerm firstTerm  =  new ExtractedTerm();
        firstTerm.setExtractedText(firstevent.getCoveredText());
        firstTerm.setBeginIndex(firstevent.getBegin());
        firstTerm.setEndIndex(firstevent.getEnd());
        finalTerms.put(firstevent.getCoveredText(), firstTerm);

        while (eventIter.hasNext())  {
            int flag = 1;
            EventMention event = (EventMention) eventIter.next();
            int tempBegin  =  event.getBegin();
            for (Entry<String, ExtractedTerm> entry : finalTerms.entrySet()) {
                if (tempBegin >=  entry.getValue().getBeginIndex()
                        && tempBegin <= entry.getValue().getEndIndex()) {
                    flag = 0;
                    break;
                }
                if (event.getBegin() == entry.getValue().getEndIndex() + 1) {
                    flag = 0;
                    ExtractedTerm temp = new ExtractedTerm();
                    temp.setEndIndex(event.getEnd());
                    temp.setBeginIndex(entry.getValue().getBeginIndex());
                    temp.setExtractedText(entry.getKey()
                            + " " + event.getCoveredText());
                    finalTerms.remove(entry.getKey());
                    finalTerms.put(entry.getKey()
                            + " " + event.getCoveredText(), temp);
                    break;
                }
            }
            if (flag == 1) {
                ExtractedTerm temp  =  new ExtractedTerm();
                temp.setEndIndex(event.getEnd());
                temp.setBeginIndex(event.getBegin());
                temp.setExtractedText(event.getCoveredText());
                finalTerms.put(event.getCoveredText(), temp);
            }
        }
        return finalTerms;
    }
}
