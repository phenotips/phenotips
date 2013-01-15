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
package edu.toronto.cs.phenotips.hpoa.utils.graph;

import java.util.LinkedList;
import java.util.List;

public class DAGNode extends AbstractNode implements IDAGNode {
	private List<String> parents;
	private List<String> children;

	public DAGNode(String id) {
		super(id, "");
	}

	public DAGNode(String id, String name) {
		super(id, name);
		this.parents = new LinkedList<String>();
		this.children = new LinkedList<String>();
	}

	public boolean addParent(String parentId) {
		return this.parents.add(parentId);
	}

	public boolean addParent(IDAGNode parent) {
		return this.parents.add(parent.getId());
	}

	public boolean removeParent(String parentId) {
		return this.parents.remove(parentId);
	}

	public boolean removeParent(IDAGNode parent) {
		return this.parents.remove(parent.getId());
	}

	public boolean hasParent(String parentId) {
		return this.parents.contains(parentId);
	}

	public boolean hasParent(IDAGNode parent) {
		return this.parents.contains(parent.getId());
	}

	public boolean addChild(String childId) {
		return this.children.add(childId);
	}

	public boolean addChild(IDAGNode child) {
		return this.children.add(child.getId());
	}

	public boolean removeChild(String childId) {
		return this.children.remove(childId);
	}

	public boolean removeChild(IDAGNode child) {
		return this.children.remove(child.getId());
	}

	public boolean hasChild(String childId) {
		return this.children.contains(childId);
	}

	public boolean hasChild(IDAGNode child) {
		return this.children.contains(child.getId());
	}

	public boolean addNeighbor(IDAGNode neighbor) {
		return this.addChild(neighbor);
	}

	public boolean removeNeighbor(String neighborId) {
		return this.removeChild(neighborId) || removeParent(neighborId);
	}

	public boolean removeNeighbor(IDAGNode neighbor) {
		return this.removeChild(neighbor) || removeParent(neighbor);
	}

	public boolean hasNeighbor(String neighborId) {
		return hasChild(neighborId) || hasParent(neighborId);
	}

	public boolean hasNeighbor(IDAGNode neighbor) {
		return hasChild(neighbor.getId()) || hasParent(neighbor.getId());
	}

	public List<String> getParents() {
		return this.parents;
	}

	public List<String> getChildren() {
		return this.children;
	}

	@Override
	public String toString() {
		return this.id + " " + this.name + " | " + this.parents + " | "
				+ this.children;
	}

	@Override
	public List<String> getNeighbors() {
		List<String> result = new LinkedList<String>();
		result.addAll(this.parents);
		result.addAll(this.children);
		return result;
	}

	@Override
	public int getNeighborsCount() {
		return this.parents.size() + this.children.size();
	}
}
