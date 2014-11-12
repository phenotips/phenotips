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
package org.phenotips.hpoa.ontology;

import org.phenotips.hpoa.utils.graph.DAG;
import org.phenotips.hpoa.utils.graph.DAGNode;
import org.phenotips.hpoa.utils.graph.IDAGNode;
import org.phenotips.solr.AbstractSolrScriptService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractOntology extends DAG<OntologyTerm> implements Ontology
{
    public static final String PARENT_ID_REGEX = "^([A-Z]{2}\\:[0-9]{7})\\s*!\\s*.*";

    private static final String TERM_MARKER = "[Term]";

    private static final String FIELD_NAME_VALUE_SEPARATOR = "\\s*:\\s+";

    private final Map<String, String> alternateIdMapping = Collections.synchronizedMap(new HashMap<String, String>());

    private final Map<String, Set<String>> ancestorCache =
        Collections.synchronizedMap(new HashMap<String, Set<String>>());

    private IDAGNode root;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public int load(AbstractSolrScriptService source)
    {
        // Make sure we can read the data
        if (source == null) {
            return -1;
        }
        // Load data
        clear();
        TermData data = new TermData();
        SolrDocumentList results = source.search("*:*");
        for (SolrDocument result : results) {
            for (String name : result.getFieldNames()) {
                Object val = result.get(name);
                if (val instanceof Collection<?>) {
                    @SuppressWarnings("unchecked")
                    Collection<String> stringVal = (Collection<String>) val;
                    data.put(name, stringVal);
                } else {
                    data.addTo(name, (String) val);
                }
            }
            if (data.isValid()) {
                this.createOntologyTerm(data);
            }
        }
        cleanArcs();
        // How much did we load:
        return size();
    }

    @Override
    public int load(File source)
    {
        // Make sure we can read the data
        if (source == null) {
            return -1;
        }
        // Load data
        clear();
        TermData data = new TermData();
        try {
            BufferedReader in = new BufferedReader(new FileReader(source));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().equalsIgnoreCase(TERM_MARKER)) {
                    if (data.isValid()) {
                        this.createOntologyTerm(data);
                    }
                    data.clear();
                    continue;
                }
                String[] pieces = line.split(FIELD_NAME_VALUE_SEPARATOR, 2);
                if (pieces.length != 2) {
                    continue;
                }
                String name = pieces[0];
                String value = pieces[1];
                data.addTo(name, value);
            }
            if (data.isValid()) {
                this.createOntologyTerm(data);
            }
            in.close();
        } catch (NullPointerException ex) {
            this.logger.error("Ontology source file [{}] does not exist", source.getAbsolutePath(), ex);
        } catch (FileNotFoundException ex) {
            this.logger.error("Could not locate ontology source file [{}]", source.getAbsolutePath());
        } catch (IOException ex) {
            this.logger.error("Cannot read ontology source file [{}]: {}", source.getAbsolutePath(), ex.getMessage());
        }
        cleanArcs();
        // How much did we load:
        return size();
    }

    private void cleanArcs()
    {
        Set<IDAGNode> roots = new HashSet<IDAGNode>();
        // Redo all links
        for (DAGNode n : getNodes()) {
            if (n.getParents().size() == 0) {
                roots.add(n);
                continue;
            }
            for (String parentId : n.getParents()) {
                DAGNode p = getTerm(parentId);
                if (p != null) {
                    p.addChild(n);
                } else {
                    this.logger.warn(
                        "[WARNING] Node with id [{}] has parent [{}], but no such node exists in the graph!",
                        n.getId(), parentId);
                }
            }
        }
        if (roots.size() == 0) {
            this.logger.warn("Something's wrong, this directed graph is DEFINITELY not acyclic!");
        } else if (roots.size() == 1) {
            for (IDAGNode n : roots) {
                this.root = n;
            }
        } else {
            this.root = new OntologyTerm("", "FAKE ROOT");
            for (IDAGNode n : roots) {
                this.root.addChild(n);
                n.addParent(this.root);
            }
        }
    }

    protected void createOntologyTerm(TermData data)
    {
        OntologyTerm term = new OntologyTerm(data);
        this.addNode(term);
        this.alternateIdMapping.put(term.getId(), term.getId());
        for (String altId : data.safeGet(TermData.ALT_ID_FIELD_NAME)) {
            this.alternateIdMapping.put(altId, term.getId());
        }
    }

    @Override
    public String getRealId(String id)
    {
        return this.alternateIdMapping.get(id);
    }

    @Override
    public OntologyTerm getTerm(String id)
    {
        String realId = this.getRealId(id);
        if (realId != null) {
            return (OntologyTerm) this.getNode(realId);
        }
        return null;
    }

    @Override
    public String getName(String id)
    {
        DAGNode node = this.getTerm(id);
        if (node != null) {
            return node.getName();
        }
        return id;
    }

    public void printAltMapping(PrintStream out)
    {
        printAltMapping(out, false);
    }

    public void printAltMapping(PrintStream out, boolean all)
    {
        for (String key : this.alternateIdMapping.keySet()) {
            if (all || !key.equals(this.alternateIdMapping.get(key))) {
                out.println(key + " -> " + this.alternateIdMapping.get(key));
            }

        }
    }

    @Override
    public String getRootId()
    {
        return this.root.getId();
    }

    @Override
    public IDAGNode getRoot()
    {
        return this.root;
    }

    protected Set<String> findAncestors(String id)
    {
        Set<String> result = new HashSet<String>();
        if (this.getTerm(id) == null) {
            return result;
        }
        Set<String> front = new HashSet<String>();
        Set<String> newFront = new HashSet<String>();
        front.add(this.getRealId(id));
        result.add(this.getRealId(id));
        while (!front.isEmpty()) {
            for (String nextTermId : front) {

                for (String parentTermId : this.getTerm(nextTermId).getParents()) {
                    if (!result.contains(parentTermId)) {
                        newFront.add(parentTermId);
                        result.add(parentTermId);
                    }
                }
            }
            front.clear();
            front.addAll(newFront);
            newFront.clear();
        }
        return result;
    }

    @Override
    public synchronized Set<String> getAncestors(String termId)
    {
        if (this.ancestorCache.get(termId) == null) {
            this.ancestorCache.put(termId, this.findAncestors(termId));
        }
        return this.ancestorCache.get(termId);
    }
}
