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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.ctakes.dictionary.lookup.BaseDictionaryImpl;
import org.apache.ctakes.dictionary.lookup.Dictionary;
import org.apache.ctakes.dictionary.lookup.DictionaryException;
import org.apache.ctakes.dictionary.lookup.MetaDataHit;
import org.apache.ctakes.dictionary.lookup.lucene.LuceneDocumentMetaDataHitImpl;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;



/**
 *
 * @author Mayo Clinic
 */
public class Lucene4DictionaryImpl extends BaseDictionaryImpl implements Dictionary
{

	private IndexSearcher iv_searcher;
	private String iv_lookupFieldName;
	//ohnlp-Bugs-3296301 limits the search results to fixed 100 records.
	private int iv_maxHits;
	// LOG4J logger based on class name
	private Logger iv_logger = Logger.getLogger(getClass().getName());

	/**
	 * 
	 * Constructor
	 *
	 */
	public Lucene4DictionaryImpl(IndexSearcher searcher, String lookupFieldName)
	{
		this(searcher, lookupFieldName, Integer.MAX_VALUE);

		// TODO Only take perfect matches?
	}

	/**
	 * 
	 * Constructor
	 *
	 */
	public Lucene4DictionaryImpl(IndexSearcher searcher, String lookupFieldName, int maxListHits)
	{
		iv_searcher = searcher;
		iv_lookupFieldName = lookupFieldName;
		// Added 'maxListHits'
		iv_maxHits = maxListHits;
		// TODO Only take perfect matches?
	}

	public Collection<MetaDataHit> getEntries(String str) throws DictionaryException
	{
		Set<MetaDataHit> metaDataHitSet = new HashSet<MetaDataHit>();

		try
		{
			Query q = null; 
			TopDocs topDoc = null;
			if (str.indexOf('-') == -1) {
				q = new TermQuery(new Term(iv_lookupFieldName, str));
				topDoc = iv_searcher.search(q, iv_maxHits);
			}
			else {  // needed the KeyworkAnalyzer for situations where the hypen was included in the f-word
				QueryParser query = new QueryParser(Version.LUCENE_40, iv_lookupFieldName, new KeywordAnalyzer());
				try {
					topDoc = iv_searcher.search(query.parse(str.replace('-', ' ')), iv_maxHits);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (iv_maxHits==0) {
				iv_maxHits=Integer.MAX_VALUE;
				iv_logger.warn("iv_maxHits was 0, using Integer.MAX_VALUE instead");
			}

			ScoreDoc[] hits = topDoc.scoreDocs;
			if (hits.length == iv_maxHits) {
				iv_logger.warn("'iv_maxHits' equals the list length returned by the lucene query (" + hits.length+").");
				iv_logger.warn("You may want to consider setting a higher value, since there may be more entries not being returned in the event greater than " +iv_maxHits +" exist.");
			}
			for (int i = 0; i < hits.length; i++) {
				int docId = hits[i].doc;
				Document luceneDoc = iv_searcher.doc(docId);
				MetaDataHit mdh = new LuceneDocumentMetaDataHitImpl(luceneDoc);
				metaDataHitSet.add(mdh);
			}

			return metaDataHitSet;
		}
		catch (IOException ioe)
		{
			throw new DictionaryException(ioe);
		}
	}

	public boolean contains(String str) throws DictionaryException
	{
		try
		{
			Query q = new TermQuery(new Term(iv_lookupFieldName, str));

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
		catch (IOException ioe)
		{
			throw new DictionaryException(ioe);
		}

	}
}
