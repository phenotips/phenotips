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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.xwiki.component.annotation.Component;

@Component
@Named("ctakes")
@Singleton
public class CTAKESScriptService extends AbstractScriptService 
{

    public Map<String,ExtractedTerm> extract(String input) throws ResourceInitializationException, AnalysisEngineProcessException  {

       
        JCas jCas  =  this.analysisEngine.newJCas();
        jCas.setDocumentText(input.toLowerCase());
        this.analysisEngine.process(jCas);


        FSIndex<Annotation> eventIndex  =  jCas.getAnnotationIndex(org.apache.ctakes.typesystem.type.textsem.EventMention.type);
        Iterator<Annotation> eventIter  =  eventIndex.iterator();   

        Map<String,ExtractedTerm> finalTerms  =  new HashMap<String, ExtractedTerm>();
        org.apache.ctakes.typesystem.type.textsem.EventMention firstevent  =  (org.apache.ctakes.typesystem.type.textsem.EventMention) eventIter.next();

        ExtractedTerm firstTerm  =  new ExtractedTerm();
        firstTerm.setExtractedText(firstevent.getCoveredText());
        firstTerm.setBeginIndex(firstevent.getBegin());
        firstTerm.setEndIndex(firstevent.getEnd());
        finalTerms.put(firstevent.getCoveredText(), firstTerm);

        while (eventIter.hasNext())  {
            int flag = 1;
            org.apache.ctakes.typesystem.type.textsem.EventMention event  =  (org.apache.ctakes.typesystem.type.textsem.EventMention) eventIter.next();

            int tempBegin  =  event.getBegin();    
            for(Entry<String, ExtractedTerm> entry : finalTerms.entrySet()) {
                if(tempBegin >=  entry.getValue().getBeginIndex() && tempBegin <= entry.getValue().getEndIndex()) {
                    flag = 0;
                    break;
                }
                if(event.getBegin()==entry.getValue().getEndIndex()+1){
    				flag=0;
    				ExtractedTerm temp = new ExtractedTerm();
    				temp.setEndIndex(event.getEnd());
    				temp.setBeginIndex(entry.getValue().getBeginIndex());
    				temp.setExtractedText(entry.getKey()+" "+event.getCoveredText());    				
    				finalTerms.remove(entry.getKey());
    				finalTerms.put(entry.getKey()+" "+event.getCoveredText(), temp);
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
