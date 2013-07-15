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
package edu.toronto.cs.phenotips.ctakes;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
import org.xwiki.component.annotation.Component;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;

import edu.toronto.cs.phenotips.ctakes.ExtractedTerm;

@Component
@Named("ctakes")
@Singleton
public class CTAKESScriptService extends AbstractScriptService 
{

    public Map<String,ExtractedTerm> extract(String input) throws ResourceInitializationException, AnalysisEngineProcessException  {

       
        JCas jCas  =  this.analysisEngine.newJCas();
        jCas.setDocumentText(input.toLowerCase());
        this.analysisEngine.process(jCas);


        FSIndex<?> eventIndex  =  jCas.getAnnotationIndex(org.apache.ctakes.typesystem.type.textsem.EventMention.type);
        Iterator<?> eventIter  =  eventIndex.iterator();   

        Map<String,ExtractedTerm> finalTerms  =  new HashMap<String, ExtractedTerm>();
        org.apache.ctakes.typesystem.type.textsem.EventMention firstevent  =  (org.apache.ctakes.typesystem.type.textsem.EventMention) eventIter.next();

        ExtractedTerm firstTerm  =  new ExtractedTerm();
        firstTerm.setExtractedText(firstevent.getCoveredText());
        firstTerm.setBeginIndex(firstevent.getBegin());
        firstTerm.setEndIndex(firstevent.getEnd());
       // firstTerm.setHPOTerm(search(firstevent.getCoveredText()));
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
    				//System.out.print(true);
    				flag=0;
    				ExtractedTerm temp = new ExtractedTerm();
    				temp.setEndIndex(event.getEnd());
    				temp.setBeginIndex(entry.getValue().getBeginIndex());
    				temp.setExtractedText(entry.getKey()+" "+event.getCoveredText());
    				//temp.setHPOTerm(Search(entry.getKey()+" "+event.getCoveredText()));
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
               // temp.setHPOTerm(search(event.getCoveredText()));
                finalTerms.put(event.getCoveredText(), temp);
            }
        }
        return finalTerms;
    }

    /*private static String search(String extractedText)
            throws CorruptIndexException, IOException, ParseException {

        String hpoTerm = ""; // HPOTerm
        Directory directory = FSDirectory.open(new File("ctakes/index-directory"));
        
        IndexReader indexReader = IndexReader.open(directory, true);
        Searcher searcher = new IndexSearcher(indexReader);
        
        QueryParser parser = new QueryParser(Version.LUCENE_CURRENT, "text",
                new StandardAnalyzer(Version.LUCENE_CURRENT));

        String search = extractedText;
        Query query = parser.parse(search);
        TopDocs topdocs = searcher.search(query, indexReader.maxDoc());
        ScoreDoc[] hits = topdocs.scoreDocs;
        for (ScoreDoc hit : hits) {
            Document documentFromSearcher = searcher.doc(hit.doc);
            hpoTerm = documentFromSearcher.get("code") + " "
                    + documentFromSearcher.get("text");
            break;
        }

        searcher.close();
        directory.close();
        return hpoTerm;
    }
*/
}
