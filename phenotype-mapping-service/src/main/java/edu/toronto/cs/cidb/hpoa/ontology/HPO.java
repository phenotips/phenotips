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
package edu.toronto.cs.cidb.hpoa.ontology;

import javax.inject.Inject;
import javax.inject.Named;

import edu.toronto.cs.cidb.hpoa.utils.io.IOUtils;
import edu.toronto.cs.cidb.solr.SolrScriptService;

public class HPO extends Ontology {
	private static Ontology instance;

	@Inject
	@Named("solr")
	private SolrScriptService service;

	private HPO() {
		super();
		if (this.service != null) {
			this.load(this.service);
		} else {
			this
					.load(IOUtils
							.getInputFileHandler(
									"http://compbio.charite.de/svn/hpo/trunk/src/ontology/human-phenotype-ontology.obo",
									false));
		}
	}

	public static Ontology getInstance() {
		if (instance == null) {
			instance = new HPO();
		}
		return instance;
	}
}
