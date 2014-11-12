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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DAG
{
    public static final String PARENT_ID_REGEX = "^([A-Z]{2}\\:[0-9]{7})\\s*!\\s*.*";

    private static final String TERM_MARKER = "[Term]";

    private static final String FIELD_NAME_VALUE_SEPARATOR = "\\s*:\\s+";

    private final Map<String, Node> nodes = new TreeMap<String, Node>();

    private Logger logger = LoggerFactory.getLogger(getClass());

    public int load(File source)
    {
        // Make sure we can read the data
        if (source == null) {
            return -1;
        }
        // Load data
        this.nodes.clear();
        TermData data = new TermData();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(source));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().equalsIgnoreCase(TERM_MARKER)) {
                    if (data.getId() != null) {
                        this.nodes.put(data.getId(), new Node(data));
                        data.clear();
                    }
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
            if (data.getId() != null) {
                this.nodes.put(data.getId(), new Node(data));
            }
        } catch (NullPointerException ex) {
            this.logger.error("Source file [{}] does not exist", source.getAbsolutePath(), ex);
        } catch (FileNotFoundException ex) {
            this.logger.error("Could not locate source file [{}]", source.getAbsolutePath());
        } catch (IOException ex) {
            this.logger.error("Cannot read source file [{}]: {}", source.getAbsolutePath(), ex.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    // Closing a stream shouldn't fail
                }
            }
        }

        // Redo all links
        for (Node n : this.nodes.values()) {
            for (String parentId : n.getParents()) {
                Node p = this.nodes.get(parentId);
                if (p != null) {
                    p.addChild(n);
                } else {
                    this.logger.warn("Node with id [{}] has parent [{}], but no such node exists in the graph!",
                        n.getId(), parentId);
                }
            }
        }
        // How much did we load:
        return this.nodes.size();
    }

    public Map<String, Node> getNodesMap()
    {
        return this.nodes;
    }

    public Set<String> getNodesIds()
    {
        return this.nodes.keySet();
    }

    public Collection<Node> getNodes()
    {
        return this.nodes.values();
    }

    public Node getNode(String id)
    {
        return this.nodes.get(id);
    }
}
