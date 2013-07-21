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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ctakes.core.resource.FileResource;
import org.apache.ctakes.dictionary.lookup.MetaDataHit;
import org.apache.ctakes.dictionary.lookup.ae.LookupInitializer;
import org.apache.ctakes.dictionary.lookup.ae.LookupSpec;
import org.apache.ctakes.dictionary.lookup.algorithms.LookupAlgorithm;
import org.apache.ctakes.dictionary.lookup.vo.LookupHit;
import org.apache.ctakes.dictionary.lookup.vo.LookupToken;
import org.apache.ctakes.dictionary.lookup.vo.LookupTokenComparator;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

@SuppressWarnings("rawtypes")
public class Dictionary4LookupAnnotator extends JCasAnnotator_ImplBase
{

	private Logger iv_logger = Logger.getLogger(getClass().getName());

	private UimaContext iv_context;

	
	private Set iv_lookupSpecSet = new HashSet();

	
	private Comparator<LookupToken> iv_lookupTokenComparator = new LookupTokenComparator();

	// used to prevent duplicate hits
	// key = hit begin,end key (java.lang.String)
	// val = Set of MetaDataHit objects
	private Map<String, Set> iv_dupMap = new HashMap<String, Set>();

		@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException
	{
		super.initialize(aContext);

		iv_context = aContext;
		configInit();

	}

	/**
	 * Reads configuration parameters.
	 */
	private void configInit() throws ResourceInitializationException
	{
		try {
		FileResource fResrc = (FileResource) iv_context.getResourceObject("LookupDescriptor");
		File descFile = fResrc.getFile();

			iv_logger.info("Parsing descriptor: " + descFile.getAbsolutePath());
			Lookup4ParseUtilities.parseDescriptor(descFile, iv_context);
		}
		catch (Exception e) {
			throw new ResourceInitializationException(e);
		}

	}
	
	/**
	 * Entry point for processing.
	 */
	public void process(JCas jcas)
			throws AnalysisEngineProcessException {
		
		iv_logger.info("process(JCas)");
		iv_dupMap.clear();
		
		try {

			Iterator lsItr = iv_lookupSpecSet.iterator();
			while (lsItr.hasNext()) {

				LookupSpec ls = (LookupSpec) lsItr.next();
				LookupInitializer lInit = ls.getLookupInitializer();

				Iterator windowItr = lInit.getLookupWindowIterator(jcas);
				while (windowItr.hasNext()) {

					Annotation window = (Annotation) windowItr.next();
					List<LookupToken> lookupTokensInWindow = constrainToWindow(
							window,
							lInit.getLookupTokenIterator(jcas));

					Map ctxMap = lInit.getContextMap(
							jcas,
							window.getBegin(),
							window.getEnd());
					performLookup(jcas, ls, lookupTokensInWindow, ctxMap);
				}
			}

		}
		catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

	/**
	 * Executes the lookup algorithm on the lookup tokens. Hits are stored to
	 * CAS.
	 */
	private void performLookup(JCas jcas, LookupSpec ls, List<LookupToken> lookupTokenList,
			Map ctxMap) throws Exception
	{
		// sort the lookup tokens
		Collections.sort(lookupTokenList, iv_lookupTokenComparator);

		// perform lookup
		Collection lookupHitCol = null;

		LookupAlgorithm la = (LookupAlgorithm) ls.getLookupAlgorithm();
		lookupHitCol = la.lookup(lookupTokenList, ctxMap);

		Collection uniqueHitCol = filterHitDups(lookupHitCol);

		// consume hits
		ls.getLookupConsumer().consumeHits(jcas, uniqueHitCol.iterator());
	}

	/**
	 * Filters out duplicate LookupHit objects.
	 * 
	 * @param lookupHitCol
	 * @return
	 */
	private Collection<LookupHit> filterHitDups(Collection lookupHitCol)
	{
		List<LookupHit> l = new ArrayList<LookupHit>();
		Iterator itr = lookupHitCol.iterator();
		while (itr.hasNext())
		{
			LookupHit lh = (LookupHit) itr.next();
			if (!isDuplicate(lh))
			{
				l.add(lh);
			}
		}
		return l;
	}

	/**
	 * Checks to see whether this hit is a duplicate.
	 * 
	 * @param lh
	 * @return
	 */
	private boolean isDuplicate(LookupHit lh)
	{
		MetaDataHit mdh = lh.getDictMetaDataHit();

		// iterate over MetaDataHits that have already been seen
		String offsetKey = getOffsetKey(lh);
		Set<MetaDataHit> mdhDuplicateSet = (Set<MetaDataHit>) iv_dupMap.get(offsetKey);
		if (mdhDuplicateSet != null)
		{
			Iterator<MetaDataHit> itr = mdhDuplicateSet.iterator();
			while (itr.hasNext())
			{
				MetaDataHit otherMdh = (MetaDataHit) itr.next();
				if (mdh.equals(otherMdh))
				{
					// current LookupHit is a duplicate
					return true;
				}
			}
		}
		else
		{
			mdhDuplicateSet = new HashSet<MetaDataHit>();
		}

		// current LookupHit is new, add it to the duplicate set
		// for future checks
		mdhDuplicateSet.add(mdh);
		iv_dupMap.put(offsetKey, mdhDuplicateSet);
		return false;
	}

	/**
	 * Gets a list of LookupToken objects within the specified window
	 * annotation.
	 * 
	 * @param window
	 * @param lookupTokenItr
	 * @return
	 * @throws Exception
	 */
	private List<LookupToken> constrainToWindow(Annotation window, Iterator lookupTokenItr)
			throws Exception
	{
		List<LookupToken> ltObjectList = new ArrayList<LookupToken>();

		while (lookupTokenItr.hasNext())
		{
			LookupToken lt = (LookupToken) lookupTokenItr.next();

			// only consider if it's within the window
			if ((lt.getStartOffset() >= window.getBegin())
					&& (lt.getEndOffset() <= window.getEnd()))
			{
				ltObjectList.add(lt);
			}
		}
		return ltObjectList;
	}

	private String getOffsetKey(LookupHit lh)
	{
		StringBuffer sb = new StringBuffer();
		sb.append(lh.getStartOffset());
		sb.append(',');
		sb.append(lh.getEndOffset());
		return sb.toString();
	}
}
