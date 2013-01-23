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
package edu.toronto.cs.phenotips.hpoa.ontology;

import java.util.LinkedList;
import java.util.List;

public class Node {
	private String id;
	private String name;
	private List<String> parents;
	private List<String> children;
	
	public Node(String id){
		this(id, "");
	}
	public Node(String id, String name){
		this.id = id;
		this.name = name;
		this.parents = new LinkedList<String>();
		this.children = new LinkedList<String>();
	}
	
	public Node(TermData data) {
		this(data.getId() + "", data.getName() + "");
		for (String parentId : data.get(TermData.PARENT_FIELD_NAME)) {
			this.addParent(parentId);
		}
	}
	
	public boolean addParent(String parentId) {
		return this.parents.add(parentId);
	}
	
	public boolean addParent(Node parent) {
		return this.parents.add(parent.getId());
	}
	
	public boolean removeParent(String parentId) {
		return this.parents.remove(parentId);
	}
	
	public boolean removeParent(Node parent) {
		return this.parents.remove(parent.getId());
	}
	public boolean hasParent(String parentId) {
		return this.parents.contains(parentId);
	}
	
	public boolean hasParent(Node parent) {
		return this.parents.contains(parent.getId());
	}
	
	public boolean addChild(String childId) {
		return this.children.add( childId);
	}
	
	public boolean addChild(Node child) {
		return this.children.add(child.getId());
	}
	
	public boolean removeChild(String childId) {
		return this.children.remove(childId);
	}
	
	public boolean removeChild(Node child) {
		return this.children.remove(child.getId());
	}
	public boolean hasChild(String childId) {
		return this.children.contains(childId);
	}
	
	public boolean hasChild(Node child) {
		return this.children.contains(child.getId());
	}
	public boolean hasNeighbor(String neighborId) {
		return hasChild(neighborId) || hasParent(neighborId);
	}
	public boolean hasNeighbor(Node neighbor) {
		return hasChild(neighbor.getId()) || hasParent(neighbor.getId());
	}
	
	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}	
	public List<String> getParents() {
		return parents;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return id + " " + name + " | " + parents + " | " + children;
	}
}
