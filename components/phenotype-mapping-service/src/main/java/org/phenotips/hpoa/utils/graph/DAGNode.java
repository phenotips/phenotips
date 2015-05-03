/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.phenotips.hpoa.utils.graph;

import java.util.LinkedList;
import java.util.List;

public class DAGNode extends AbstractNode implements IDAGNode
{
    private List<String> parents;

    private List<String> children;

    public DAGNode(String id)
    {
        super(id, "");
    }

    public DAGNode(String id, String name)
    {
        super(id, name);
        this.parents = new LinkedList<String>();
        this.children = new LinkedList<String>();
    }

    @Override
    public boolean addParent(String parentId)
    {
        return this.parents.add(parentId);
    }

    @Override
    public boolean addParent(IDAGNode parent)
    {
        return this.parents.add(parent.getId());
    }

    @Override
    public boolean removeParent(String parentId)
    {
        return this.parents.remove(parentId);
    }

    @Override
    public boolean removeParent(IDAGNode parent)
    {
        return this.parents.remove(parent.getId());
    }

    @Override
    public boolean hasParent(String parentId)
    {
        return this.parents.contains(parentId);
    }

    @Override
    public boolean hasParent(IDAGNode parent)
    {
        return this.parents.contains(parent.getId());
    }

    @Override
    public boolean addChild(String childId)
    {
        return this.children.add(childId);
    }

    @Override
    public boolean addChild(IDAGNode child)
    {
        return this.children.add(child.getId());
    }

    @Override
    public boolean removeChild(String childId)
    {
        return this.children.remove(childId);
    }

    @Override
    public boolean removeChild(IDAGNode child)
    {
        return this.children.remove(child.getId());
    }

    @Override
    public boolean hasChild(String childId)
    {
        return this.children.contains(childId);
    }

    @Override
    public boolean hasChild(IDAGNode child)
    {
        return this.children.contains(child.getId());
    }

    public boolean addNeighbor(IDAGNode neighbor)
    {
        return this.addChild(neighbor);
    }

    public boolean removeNeighbor(String neighborId)
    {
        return this.removeChild(neighborId) || removeParent(neighborId);
    }

    public boolean removeNeighbor(IDAGNode neighbor)
    {
        return this.removeChild(neighbor) || removeParent(neighbor);
    }

    public boolean hasNeighbor(String neighborId)
    {
        return hasChild(neighborId) || hasParent(neighborId);
    }

    public boolean hasNeighbor(IDAGNode neighbor)
    {
        return hasChild(neighbor.getId()) || hasParent(neighbor.getId());
    }

    @Override
    public List<String> getParents()
    {
        return this.parents;
    }

    @Override
    public List<String> getChildren()
    {
        return this.children;
    }

    @Override
    public String toString()
    {
        return this.id + " " + this.name + " | " + this.parents + " | " + this.children;
    }

    @Override
    public List<String> getNeighbors()
    {
        List<String> result = new LinkedList<String>();
        result.addAll(this.parents);
        result.addAll(this.children);
        return result;
    }

    @Override
    public int getNeighborsCount()
    {
        return this.parents.size() + this.children.size();
    }
}
