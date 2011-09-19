package ut.cb.sv.go;

import java.util.HashSet;
import java.util.TreeSet;

/**
 * @author Nirvana Nursimulu
 */
public class Node implements Comparable<Node>
{

    /**
     * Children of this node.
     */
    private TreeSet<Node> children;

    /**
     * Identifier of this node. (Acts as some kind of key)
     */
    private String identifier;

    /**
     * Description of this node.
     */
    private String description;

    /**
     * Keep track of the genome locations associated with this node, if any.
     */
    private HashSet<String> locs;

    /**
     * Useful for the GUI implementation; says if this node has already been selected earlier.
     */
    private boolean hasBeenSelected;

    /**
     * Total description of a node. Incorporates the actual description and any additional description. Note that the
     * total description is not actually used in comparisons, and is merely utilised for aesthetic purposes.
     */
    private String totalDescription;

    /**
     * Special node associated with this node. It has the same info as the node to which it is special, is that node's
     * child, but has (identifier of parent)_SPECIAL as identifier name, and does not contain the same children as the
     * parent child. Note that the parent needs to be declared to have a special child (and this is done when
     * instantiating the node in the first place). Also, this node is not actually registered to be in the tree, so,
     * special care must be taken then.
     */
    private Node specialCopyNode;

    /**
     * The description of the special node.
     */
    private String descriptionSpecialNode;

    /**
     * Is this node special to a certain node?
     */
    private boolean isSpecial;

    /**
     * Says whether this node has been discovered while performing a tree traversal.
     */
    private boolean hasBeenDiscovered;

    /**
     * To showcase GO and HPO IDs. If not set when constructing node, will be taken to be false.
     */
    private boolean includeIdentifierInDescr = false;

    /**
     * Constructor of this node given an identifier.
     * 
     * @param identifier "key" of this node.
     */
    public Node(String identifier, String descriptionSpecialNode, boolean includeIdentifierInDescr)
    {

        this.children = new TreeSet<Node>();
        this.identifier = identifier;
        this.description = null;
        this.totalDescription = "";
        this.locs = null;
        this.hasBeenSelected = false;
        this.specialCopyNode = null;
        this.descriptionSpecialNode = descriptionSpecialNode;
        this.setSpecialNode();
        // By default, no node is special.
        this.isSpecial = false;
        this.hasBeenDiscovered = false;
        this.includeIdentifierInDescr = includeIdentifierInDescr;
    }

    public Node(String identifier, String descriptionSpecialNode)
    {
        this(identifier, descriptionSpecialNode, false);
    }

    /**
     * Use only for setting a special node. Use only and specially for special nodes. Note that special Nodes do not
     * formally form part of the tree.
     * 
     * @param nodeToWhichIamSpecial
     */
    private Node(Node nodeToWhichIamSpecial)
    {
        this.identifier = nodeToWhichIamSpecial.identifier + "_SPECIAL";
        this.locs = nodeToWhichIamSpecial.locs;
        this.description = nodeToWhichIamSpecial.descriptionSpecialNode;
        this.totalDescription = this.description;
        this.isSpecial = true;
        this.children = new TreeSet<Node>();
        this.hasBeenDiscovered = false;
    }

    /**
     * Get a copy of this node. Use this with great care. All nodes created in this way have isSpecial being false.
     * 
     * @return a copy of this node.
     */
    public Node getCopy()
    {

        Node copyNode = new Node(this.identifier, this.descriptionSpecialNode, this.includeIdentifierInDescr);
        copyNode.isSpecial = this.isSpecial;
        copyNode.description = this.description;
        copyNode.hasBeenDiscovered = this.hasBeenDiscovered;
        copyNode.hasBeenSelected = this.hasBeenSelected;
        copyNode.locs = this.locs;
        if (this.specialCopyNode != null) {
            copyNode.specialCopyNode = this.specialCopyNode.getCopy();
        }
        copyNode.totalDescription = this.totalDescription;

        copyNode.children = new TreeSet<Node>();
        for (Node node : this.children) {
            copyNode.children.add(node.getCopy());
        }
        return copyNode;
    }

    /**
     * Set this node as the special node of this node. As a default, a node does not have any children.
     * 
     * @param descriptionSpecialNode the description of this special node. If that is null, do not set special node.
     */
    private void setSpecialNode()
    {

        if (this.descriptionSpecialNode == null) {
            this.removeChild(this.specialCopyNode);
            this.specialCopyNode = null;
        }

        // If this node does not already exist, then create the special node.
        else if (this.specialCopyNode == null) {
            this.specialCopyNode = new Node(this);
            this.addChild(this.specialCopyNode);
        }
        // Otherwise, just reformat info in this node.
        else {
            this.specialCopyNode.description = this.descriptionSpecialNode;
            this.specialCopyNode.totalDescription = this.specialCopyNode.description;
            this.specialCopyNode.identifier = this.identifier + "_SPECIAL";
            this.specialCopyNode.locs = this.locs;
        }
    }

    public boolean isSpecialNode()
    {
        return this.isSpecial;
    }

    /**
     * Set a special node for this node
     * 
     * @param descriptionSpecialNode the description of the special node of this node.
     */
    public void setSpecialNode(String descriptionSpecialNode)
    {
        this.descriptionSpecialNode = descriptionSpecialNode;
        this.setSpecialNode();
    }

    /**
     * Set the locations of this node.
     * 
     * @param locs the locations to be associated with this node.
     * @param changeSpecial true iff we want to change the value of the special node too.
     */
    public void setLocs(HashSet<String> locs, boolean changeSpecial)
    {

        this.locs = locs;
        if (this.specialCopyNode != null && changeSpecial) {
            this.specialCopyNode.setLocs(locs, false);
        }
    }

    /**
     * Return the locations for this node. (Maybe should try to return a copy instead?)
     * 
     * @return
     */
    public HashSet<String> getLocs()
    {

        return this.getCopyLocs();
    }

    /**
     * Get a copy of the locations for this node.
     * 
     * @return that copy.
     */
    private HashSet<String> getCopyLocs()
    {

        if (this.locs == null) {
            return null;
        }
        HashSet<String> newCopy = new HashSet<String>();
        for (String part : this.locs) {
            newCopy.add(part);
        }
        return newCopy;
    }

    /**
     * Adds a child to this node.
     * 
     * @param child
     */
    public void addChild(Node child)
    {

        this.children.add(child);
        // child.addParent(this);
    }

    /**
     * Remove this from me: it's not my child. Use this with many precautions. Really, don't make this public.
     * 
     * @param child
     */
    private void removeChild(Node child)
    {
        if (child != null) {
            this.children.remove(child);
        }
    }

    /**
     * Returns the child of this node.
     * 
     * @return
     */
    public TreeSet<Node> getChildren()
    {

        return this.children;
    }

    /**
     * Copy info from this node into the designated node (except for children info)
     * 
     * @param node node to copy info to.
     */
    public void copyInfoExceptChildrenTo(Node node)
    {
        node.setDescription(this.description);
        node.setIdentifier(this.identifier);
        node.setLocs(this.locs, true);
    }

    /**
     * Get the identifier of this node.
     * 
     * @return the identifier of this node.
     */
    public String getIdentifier()
    {

        return this.identifier;
    }

    /**
     * Sets the identifier of this node.
     * 
     * @param identifier
     */
    public void setIdentifier(String identifier)
    {

        this.identifier = identifier;
        // change of identifier affects special node.
        setSpecialNode();
    }

    /**
     * Set the description of this node.
     * 
     * @param description
     */
    public void setDescription(String description)
    {

        this.description = description;
        setTotalDescription("");
        // change of description does not affect special node.
    }

    /**
     * Set the total description of this node. This does not modify the actual description of the node, but modifies the
     * total description. This is an optional operation.
     * 
     * @param additionalDescription
     */
    public void setTotalDescription(String additionalDescription)
    {

        if (this.includeIdentifierInDescr) {
            this.totalDescription = this.description +
                " [" + this.identifier + "]" + additionalDescription;
        } else {
            this.totalDescription = this.description + additionalDescription;
        }
    }

    /**
     * Get the actual description of this node.
     * 
     * @return description.
     */
    public String getDescription()
    {

        return this.description;
    }

    @Override
    /**
     * Returns entire description.
     */
    public String toString()
    {

        if (this.includeIdentifierInDescr) {
            return this.description + " [" + this.identifier + "]";
        } else {
            return this.description;
        }
    }

    /**
     * Returns only the original description, WITHOUT the additional description
     * 
     * @return
     */
    public String toValue()
    {

        return this.totalDescription;
    }

    /**
     * Select this node; useful in the GUI implementation
     */
    public void select()
    {

        this.hasBeenSelected = true;
    }

    /**
     * Says if this node has been selected.
     * 
     * @return true iff this node has been selected.
     */
    public boolean isSelected()
    {

        return this.hasBeenSelected;
    }

    /**
     * How does this node compare to this other object, which is potentially a node. Note that a special node ranks
     * higher than any other node.
     */
    // @Override
    public int compareTo(Node node)
    {

        try {
            if (!node.isSpecial && !this.isSpecial) {
                return this.description.compareTo(node.description);
            } else {
                if (this.isSpecial && !node.isSpecial) {
                    return -1;
                } else if (!this.isSpecial && node.isSpecial) {
                    return +1;
                }
                // Both are special:
                // this case is actually not expected to be ever encountered.
                else {
                    return 0;
                }
            }
        } catch (Exception e) {

            return 0;
        }
    }

    /**
     * Says whether this node has any children.
     * 
     * @return true iff this node has any children.
     */
    public boolean hasChildren()
    {
        return !this.children.isEmpty();
    }

    /**
     * Says whether this node has any parent.
     * 
     * @return true iff this node has any parents.
     */
    // public boolean hasParents(){
    // return !this.parents.isEmpty();
    // }

    /**
     * Says that we have discovered this node.
     */
    public void discover()
    {
        this.hasBeenDiscovered = true;
    }

    /**
     * Returns true iff this node has been discovered during a tree traversal.
     * 
     * @return
     */
    public boolean isDiscovered()
    {
        return this.hasBeenDiscovered;
    }

}
