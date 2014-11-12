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
package org.phenotips.hpoa.utils.graph;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DAG<T extends DAGNode>
{
    private Map<String, T> nodes = new TreeMap<String, T>();

    public void clear()
    {
        this.nodes.clear();
    }

    public void addNode(T n)
    {
        this.nodes.put(n.getId(), n);
    }

    public Map<String, T> getNodesMap()
    {
        return this.nodes;
    }

    public Set<String> getNodesIds()
    {
        return this.nodes.keySet();
    }

    public Collection<T> getNodes()
    {
        return this.nodes.values();
    }

    public DAGNode getNode(String id)
    {
        return this.nodes.get(id);
    }

    public int size()
    {
        return this.nodes.size();
    }
}
