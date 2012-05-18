package edu.toronto.cs.cidb.hpoa;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import edu.toronto.cs.cidb.solr.SolrScriptService;

@Component
@Named("hpoa")
@Singleton
public class PhenotypeMappingScriptService implements ScriptService {

	@Inject
	@Named("solr")
	private SolrScriptService service;
	
	public void doStuff()
	{
		this.service.get("");
	}
}
