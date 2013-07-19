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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import org.apache.ctakes.dictionary.lookup.MetaDataHit;
import org.apache.ctakes.dictionary.lookup.ae.BaseLookupConsumerImpl;
import org.apache.ctakes.dictionary.lookup.ae.LookupConsumer;
import org.apache.ctakes.dictionary.lookup.vo.LookupHit;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.MedicationEventMention;
import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

/**
 * Implementation that takes Rxnorm dictionary lookup hits and stores only the
 * ones that are also present in the Orange Book.
 * 
 * @author Mayo Clinic
 */
public class OrangeBook4FilterConsumerImpl extends BaseLookupConsumerImpl
		implements LookupConsumer
{
	// LOG4J logger based on class name
	private Logger iv_logger = Logger.getLogger(getClass().getName());

	private final String CODE_MF_PRP_KEY = "codeMetaField";

	private final String CODING_SCHEME_PRP_KEY = "codingScheme";

	private final String LUCENE_FILTER_RESRC_KEY_PRP_KEY = "luceneFilterExtResrcKey";

	private Properties iv_props;

	private IndexSearcher iv_searcher;
	//ohnlp-Bugs-3296301 limits the search results to fixed 100 records.
	// Added 'MaxListSize'
	private int iv_maxHits;

	public OrangeBook4FilterConsumerImpl(UimaContext aCtx, Properties props, int maxListSize)
			throws Exception
	{
		// TODO property validation could be done here
		iv_props = props;
		iv_maxHits = maxListSize;
		String resrcName = iv_props.getProperty(LUCENE_FILTER_RESRC_KEY_PRP_KEY);
		Lucene4IndexReaderResource resrc = (Lucene4IndexReaderResource) aCtx.getResourceObject(resrcName);
		iv_searcher = new IndexSearcher(resrc.getIndexReader());
	}
	public OrangeBook4FilterConsumerImpl(UimaContext aCtx, Properties props)
	throws Exception
	{
		// TODO property validation could be done here
		iv_props = props;
		String resrcName = iv_props.getProperty(LUCENE_FILTER_RESRC_KEY_PRP_KEY);
		Lucene4IndexReaderResource resrc = (Lucene4IndexReaderResource) aCtx.getResourceObject(resrcName);
		iv_searcher = new IndexSearcher(resrc.getIndexReader());
		iv_maxHits = Integer.MAX_VALUE;
	}
	public void consumeHits(JCas jcas, Iterator lhItr)
			throws AnalysisEngineProcessException
	{
		Iterator hitsByOffsetItr = organizeByOffset(lhItr);
		while (hitsByOffsetItr.hasNext())
		{
			Collection hitsAtOffsetCol = (Collection) hitsByOffsetItr.next();

			// iterate over the LookupHit objects
			// code is only valid if the covered text is also present in the
			// filter
			Iterator lhAtOffsetItr = hitsAtOffsetCol.iterator();
			int neBegin = -1;
			int neEnd = -1;
			Collection<String> validCodeCol = new HashSet<String>();
			while (lhAtOffsetItr.hasNext())
			{
				LookupHit lh = (LookupHit) lhAtOffsetItr.next();
				neBegin = lh.getStartOffset();
				neEnd = lh.getEndOffset();

				String text = jcas.getDocumentText().substring(
						lh.getStartOffset(),
						lh.getEndOffset());
				text = text.trim().toLowerCase();

				MetaDataHit mdh = lh.getDictMetaDataHit();
				String code = mdh.getMetaFieldValue(iv_props.getProperty(CODE_MF_PRP_KEY));

				if (isValid("trade_name", text) || isValid("ingredient", text))
				{
					validCodeCol.add(code);
				}
				else
				{
					iv_logger.warn("Filtered out: "+text);
				}
			}

			if (validCodeCol.size() > 0)
			{
				FSArray ocArr = createOntologyConceptArr(jcas, validCodeCol);
				IdentifiedAnnotation neAnnot = new MedicationEventMention(jcas); // medication NEs are EventMention
				neAnnot.setTypeID(CONST.NE_TYPE_ID_DRUG);
				neAnnot.setBegin(neBegin);
				neAnnot.setEnd(neEnd);
				neAnnot.setDiscoveryTechnique(CONST.NE_DISCOVERY_TECH_DICT_LOOKUP);
				neAnnot.setOntologyConceptArr(ocArr);
				neAnnot.addToIndexes();
			}
		}
	}

	/**
	 * For each valid code, a corresponding JCas OntologyConcept object is
	 * created and stored in a FSArray.
	 * 
	 * @param jcas
	 * @param validCodeCol
	 * @return
	 */
	private FSArray createOntologyConceptArr(JCas jcas, Collection<String> validCodeCol)
	{
		FSArray ocArr = new FSArray(jcas, validCodeCol.size());
		int ocArrIdx = 0;
		Iterator<String> validCodeItr = validCodeCol.iterator();
		while (validCodeItr.hasNext())
		{
			String validCode = (String) validCodeItr.next();
			OntologyConcept oc = new OntologyConcept(jcas);
			oc.setCode(validCode);
			oc.setCodingScheme(iv_props.getProperty(CODING_SCHEME_PRP_KEY));

			ocArr.set(ocArrIdx, oc);
			ocArrIdx++;
		}
		return ocArr;
	}

	private boolean isValid(String fieldName, String str)
			throws AnalysisEngineProcessException
	{
		try
		{
			Query q = new TermQuery(new Term(fieldName, str));

            TopDocs topDoc = iv_searcher.search(q, iv_maxHits);
            ScoreDoc[] hits = topDoc.scoreDocs;
            if ((hits != null) && (hits.length > 0))
            {
                return true;
            }
            else
            {
                return false;
            }
		}
		catch (Exception e)
		{
			throw new AnalysisEngineProcessException(e);
		}
	}
}