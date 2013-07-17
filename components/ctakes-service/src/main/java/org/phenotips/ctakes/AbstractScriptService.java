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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ctakes.chunker.ae.Chunker;
import org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster;
import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.CopyAnnotator;
import org.apache.ctakes.core.ae.OverlapAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.dictionary.lookup.ae.DictionaryLookupAnnotator;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.impl.ExternalResourceDependency_impl;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.ConfigurationParameterFactory;
import org.uimafit.factory.ExternalResourceFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.script.service.ScriptService;


public abstract class AbstractScriptService implements ScriptService, Initializable
{
    /**Analysis Engine Used. */
    protected AnalysisEngine analysisEngine;
    

    public void initialize() throws InitializationException
    {
        TypeSystemDescription maintypeSystem  =  TypeSystemDescriptionFactory.createTypeSystemDescription("org.apache.ctakes.typesystem.types.TypeSystem");

        try{
        	
            //Segmentation 
            AnalysisEngineDescription simpleSegmentDesc  =  AnalysisEngineFactory.createPrimitiveDescription(SimpleSegmentAnnotator.class);       


            //Tokenizer 
            AnalysisEngineDescription tokenizerDesc  =  AnalysisEngineFactory.createPrimitiveDescription(TokenizerAnnotatorPTB.class, maintypeSystem);

            //Sentence Detection
            String url = new File("webapps/phenotips/resources/cTAKES/SentenceDetection/sdmed.mod").toURI().toURL().toString();
            System.out.println(url);
            ExternalResourceDescription erd  =  ExternalResourceFactory.createExternalResourceDescription("MaxentModelFile", org.apache.ctakes.core.resource.SuffixMaxentModelResourceImpl.class, url);     
            AnalysisEngineDescription sentenceDetectorDesc  =  AnalysisEngineFactory.createPrimitiveDescription(SentenceDetector.class, "MaxentModel", erd);
            ExternalResourceFactory.createDependency(sentenceDetectorDesc, "MaxentModel", org.apache.ctakes.core.resource.MaxentModelResource.class);


            //context dependent tokenizer 
            AnalysisEngineDescription contextDependentTokenizerDesc  =  AnalysisEngineFactory.createPrimitiveDescription(ContextDependentTokenizerAnnotator.class);


            //POS Tagger
            AnalysisEngineDescription posTaggerdesc  =  AnalysisEngineFactory.createPrimitiveDescription(POSTagger.class,maintypeSystem,POSTagger.POS_MODEL_FILE_PARAM, "webapps/phenotips/resources/cTAKES/POSTagger/mayo-pos.zip", POSTagger.CASE_SENSITIVE_PARAM, true);


           //Chunker
            url = new File("webapps/phenotips/resources/cTAKES/Chunker/chunk-model-claims-1-5.zip").toURI().toURL().toString();
            System.out.println(url);
            ExternalResourceDescription chunkererd  =  ExternalResourceFactory.createExternalResourceDescription("ChunkerModelFile", org.apache.ctakes.core.resource.FileResourceImpl.class, url);     
            AnalysisEngineDescription chunkerDesc  =  AnalysisEngineFactory.createPrimitiveDescription(Chunker.class, maintypeSystem,Chunker.CHUNKER_MODEL_FILE_PARAM,"webapps/phenotips/resources/cTAKES/Chunker/chunk-model-claims-1-5.zip",Chunker.CHUNKER_CREATOR_CLASS_PARAM,"org.apache.ctakes.chunker.ae.DefaultChunkCreator","ChunkerModel",chunkererd);
            ExternalResourceFactory.createDependency(chunkerDesc, "ChunkerModel", org.apache.ctakes.core.resource.FileResource.class);


            //Chunk Adjuster NN
            String[] chunk_pattern  =  new String[2];
            chunk_pattern[0] = "NN";
            chunk_pattern[1] = "NN";
            AnalysisEngineDescription chunkAdjusterDesc  =  AnalysisEngineFactory.createPrimitiveDescription(ChunkAdjuster.class, null,ChunkAdjuster.PARAM_CHUNK_PATTERN, chunk_pattern, ChunkAdjuster.PARAM_EXTEND_TO_INCLUDE_TOKEN, 1);


            //chunk adjuster PN
            String[] chunk_patternPN  =  new String[3];
            chunk_patternPN[0] = "NN";
            chunk_patternPN[1] = "PN";
            chunk_patternPN[2] = "NN";
            AnalysisEngineDescription chunkAdjusterPNDesc  =  AnalysisEngineFactory.createPrimitiveDescription(ChunkAdjuster.class, null, ChunkAdjuster.PARAM_EXTEND_TO_INCLUDE_TOKEN, 2, ChunkAdjuster.PARAM_CHUNK_PATTERN, chunk_patternPN);



            //Lookup Annotators
            //Overlap Annotators        
            ConfigurationParameter[] configurationParameters  =  new ConfigurationParameter[5];
            ConfigurationParameter actionType  =  ConfigurationParameterFactory.createPrimitiveParameter("ActionType", String.class, "desc", true);
            ConfigurationParameter A_OBJECT_CLASS  =  ConfigurationParameterFactory.createPrimitiveParameter("A_ObjectClass", String.class, "desc", true);
            ConfigurationParameter B_OBJECT_CLASS  =  ConfigurationParameterFactory.createPrimitiveParameter("B_ObjectClass", String.class, "desc", true);
            ConfigurationParameter Overlaptype  =  ConfigurationParameterFactory.createPrimitiveParameter("OverlapType", String.class, "desc", true);
            ConfigurationParameter Deleteaction  =  ConfigurationParameterFactory.createPrimitiveParameter("DeleteAction", String.class, "desc", true);      
            Deleteaction.setMultiValued(true);
            configurationParameters[0] = A_OBJECT_CLASS;
            configurationParameters[1] = B_OBJECT_CLASS;
            configurationParameters[2] = Overlaptype;
            configurationParameters[3] = actionType;
            configurationParameters[4] = Deleteaction;

            Object[] configVals  =  new Object[5];
            
            configVals[0] = "org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation";
            configVals[1] = "org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation";
            configVals[2] = "A_ENV_B";
            configVals[3] = "DELETE";
            String[] deleteActionArray  =  new String[1];
            deleteActionArray[0] = "selector=B";
            configVals[4] = deleteActionArray;
            
            AnalysisEngineDescription overlapdesc  =  AnalysisEngineFactory.createPrimitiveDescription(OverlapAnnotator.class, null, null, null, null, configurationParameters, configVals);


            //Copy Annotator - Lookup 
            TypeSystemDescription copyAnnotatorTypeSystemDesc  =  new TypeSystemDescription_impl();
            copyAnnotatorTypeSystemDesc.addType("org.apache.ctakes.typesystem.type.CopySrcAnnotation", null, "uima.tcas.Annotation");
            copyAnnotatorTypeSystemDesc.addType("org.apache.ctakes.typesystem.type.CopyDestAnnotation", null, "uima.tcas.Annotation");
            ConfigurationParameter[] copyAnnotatorconfigurationParameters  =  new ConfigurationParameter[3];
            ConfigurationParameter srcObjClass  =  ConfigurationParameterFactory.createPrimitiveParameter("srcObjClass", String.class, "desc", true);
            ConfigurationParameter destObjClass  =  ConfigurationParameterFactory.createPrimitiveParameter("destObjClass", String.class, "desc", true);
            ConfigurationParameter dataBindMap  =  ConfigurationParameterFactory.createPrimitiveParameter("dataBindMap", String.class, "desc", true);
            dataBindMap.setMultiValued(true);
            copyAnnotatorconfigurationParameters[0] = srcObjClass;
            copyAnnotatorconfigurationParameters[1] = destObjClass;
            copyAnnotatorconfigurationParameters[2] = dataBindMap;

            Object[] copyAnnotatorconfigVals  =  new Object[3];
            copyAnnotatorconfigVals[0] = "org.apache.ctakes.typesystem.type.syntax.NP";
            copyAnnotatorconfigVals[1] = "org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation";
            String[] dataBindArray  =  new String[2];
            dataBindArray[0] = "getBegin|setBegin";
            dataBindArray[1] = "getEnd|setEnd";
            copyAnnotatorconfigVals[2] = dataBindArray;

            AnalysisEngineDescription copyAnnotatorDesc  =  AnalysisEngineFactory.createPrimitiveDescription(CopyAnnotator.class, copyAnnotatorTypeSystemDesc,null,null,null, copyAnnotatorconfigurationParameters,copyAnnotatorconfigVals);


            //Dictionary Lookup
            ConfigurationParameter[] dictionaryconfigurationParameters = new ConfigurationParameter[1];
            ConfigurationParameter maxListSize = ConfigurationParameterFactory.createPrimitiveParameter("maxListSize", Integer.class, "desc", false);
            dictionaryconfigurationParameters[0]=maxListSize;
            
            Object[] dictionaryconfigVals = new Object[1];
            dictionaryconfigVals[0]=27;
        	
            ExternalResourceDescription dictionaryERD1 = ExternalResourceFactory.createExternalResourceDescription("LookupDescriptorFile", org.apache.ctakes.core.resource.FileResourceImpl.class, new File("webapps/phenotips/resources/cTAKES/DictionaryLookup/LookupDesc_csv_sample.xml").toURI().toURL().toString());
        	ExternalResourceDescription dictionaryERD2 = ExternalResourceFactory.createExternalResourceDescription("DictionaryFileResource", org.apache.ctakes.core.resource.FileResourceImpl.class, new File("webapps/phenotips/resources/cTAKES/DictionaryLookup/dictionary1.csv").toURI().toURL().toString());
        	ExternalResourceDescription dictionaryERD3 = ExternalResourceFactory.createExternalResourceDescription("RxnormIndex", org.phenotips.ctakes.Lucene4IndexReaderResourceImpl.class,"", "UseMemoryIndex",true,"IndexDirectory","webapps/phenotips/resources/cTAKES/DictionaryLookup/drug_index");
        	ExternalResourceDescription dictionaryERD4 = ExternalResourceFactory.createExternalResourceDescription("OrangeBookIndex", org.phenotips.ctakes.Lucene4IndexReaderResourceImpl.class,"" ,"UseMemoryIndex",true,"IndexDirectory","webapps/phenotips/resources/cTAKES/DictionaryLookup/OrangeBook");
        	Map<String,ExternalResourceDescription> dictionaryMap = new HashMap<String, ExternalResourceDescription>();
        	dictionaryMap.put("LookupDescriptor", dictionaryERD1);dictionaryMap.put("DictionaryFile", dictionaryERD2);
        	dictionaryMap.put("RxnormIndexReader", dictionaryERD3);dictionaryMap.put("OrangeBookIndexReader", dictionaryERD4);
            
        	AnalysisEngineDescription dictionarylookupDesc = AnalysisEngineFactory.createPrimitiveDescription(DictionaryLookupAnnotator.class,null,null,null,null,dictionaryconfigurationParameters,dictionaryconfigVals,dictionaryMap);
           
            ExternalResourceDependency[] dictDependencies = new ExternalResourceDependency[4];
        	ExternalResourceDependency dicterd1= new ExternalResourceDependency_impl();
        	dicterd1.setKey("LookupDescriptor");dicterd1.setOptional(false);dicterd1.setInterfaceName("org.apache.ctakes.core.resource.FileResource");
        	ExternalResourceDependency dicterd2= new ExternalResourceDependency_impl();
        	dicterd2.setKey("DictionaryFile");dicterd2.setOptional(false);dicterd2.setInterfaceName("org.apache.ctakes.core.resource.FileResource");
        	ExternalResourceDependency dicterd3= new ExternalResourceDependency_impl();
        	dicterd3.setKey("RxnormIndexReader");dicterd3.setOptional(false);dicterd3.setInterfaceName("org.phenotips.ctakes.Lucene4IndexReaderResource");
        	ExternalResourceDependency dicterd4= new ExternalResourceDependency_impl();
        	dicterd4.setKey("OrangeBookIndexReader");dicterd4.setOptional(false);dicterd4.setInterfaceName("org.phenotips.ctakes.Lucene4IndexReaderResource");
        	dictDependencies[0]=dicterd1;dictDependencies[1]=dicterd2;dictDependencies[2]=dicterd3;dictDependencies[3]=dicterd4;
        	dictionarylookupDesc.setExternalResourceDependencies(dictDependencies);
        
        	
            //Final Analysis Engine Description
            List<AnalysisEngineDescription> analysisEngineDescriptionList  =  new ArrayList<AnalysisEngineDescription>();
            List<String> componentNames  =  new ArrayList<String>();

            analysisEngineDescriptionList.add(simpleSegmentDesc);
            componentNames.add("SimpleSegmentAnnotator");
            analysisEngineDescriptionList.add(sentenceDetectorDesc);
            componentNames.add("SentenceDetector");
            analysisEngineDescriptionList.add(tokenizerDesc);
            componentNames.add("TokenizerAnnotator");
            analysisEngineDescriptionList.add(contextDependentTokenizerDesc);
            componentNames.add("ContextDependentTokenizer");
            analysisEngineDescriptionList.add(posTaggerdesc);
            componentNames.add("POSTagger");
            analysisEngineDescriptionList.add(chunkerDesc);
            componentNames.add("Chunker");
            analysisEngineDescriptionList.add(chunkAdjusterDesc);
            componentNames.add("AdjustNounPhraseToIncludeFollowingNP");
            analysisEngineDescriptionList.add(chunkAdjusterPNDesc);
            componentNames.add("AdjustNounPhraseToIncludeFollowingPPNP");
            analysisEngineDescriptionList.add(overlapdesc);
            componentNames.add("OverlapAnnotator-Lookup");
            analysisEngineDescriptionList.add(copyAnnotatorDesc);
            componentNames.add("CopyAnnotator-Lookup"); 
            analysisEngineDescriptionList.add(dictionarylookupDesc);
            componentNames.add("Dictionary Lookup");
         

            // Create the Analysis Engine
           
            System.out.println(Runtime.getRuntime().freeMemory());
            analysisEngine = AnalysisEngineFactory.createAggregate(analysisEngineDescriptionList,componentNames, null, null, null);
            System.out.println(Runtime.getRuntime().freeMemory());
            System.out.println("Analysis Engine Initialised");
        }catch(Exception e){
            System.out.println("Error Initialising Analysis Engine");
            e.printStackTrace();            
            System.out.println(e.getMessage());
        }
    }

}
