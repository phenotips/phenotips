package ut.cb.sv.go;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Nirvana Nursimulu
 */
public class Tree
{

    /**
     * Fake root of the tree. Useful since this is actually a forest of trees.
     */
    protected Node fakeRoot;

    /**
     * Dictionary from an identifier to a node.
     */
    protected HashMap<String, Node> identifierToNode;

    /**
     * Map from ID to locations. This is customizable, therefore, has protected access.
     */
    protected HashMap<String, HashSet<String>> idToLocs;

    public static final String NAME_OF_ROOT = "ROOT";

    /**
     * Constructor
     * 
     * @param fileGenLocation the location of the file mapping GO IDs to genome locations.
     */
    public Tree()
    {

        // no special node for the fake root.
        this.fakeRoot = new Node(NAME_OF_ROOT, null);
        this.fakeRoot.setLocs(new HashSet<String>(), false);
        this.identifierToNode = new HashMap<String, Node>();
        this.identifierToNode.put(NAME_OF_ROOT, this.fakeRoot);
        this.idToLocs = new HashMap<String, HashSet<String>>();
    }

    public int getSize()
    {
        return this.identifierToNode.keySet().size();
    }

    /**
     * Get the root nodes of this tree.
     * 
     * @return the actual roots of this forest (XNode objects).
     */
    public Set<Node> getRootNodes()
    {

        return this.fakeRoot.getChildren();
    }

    public HashSet<String> getLocsOfFakeRoot()
    {
        return this.fakeRoot.getLocs();
    }

    /**
     * Get the children of the node with the provided identifier.
     * 
     * @param identifier the identifier of the parent.
     * @return the TreeSet of children nodes of this parent.
     */
    public TreeSet<Node> getChildrenNodes(String identifier)
    {

        Node node = this.identifierToNode.get(identifier);
        if (node != null) {
            return node.getChildren();
        }
        // If this node is special, it is not fated to have children at all, and
        // is not even registered to belong to the tree..
        else {
            return new TreeSet<Node>();
        }

    }

    /**
     * Mark the locations of this node.
     * 
     * @param node the node whose locations are to be marked.
     */
    private void markLocations(Node node)
    {

        // If the locations have already been marked, do not do anything.
        if (node.getLocs() != null) {

            return;
        }
        HashSet<String> locs = this.idToLocs.get(node.getIdentifier());
        if (locs == null) {
            locs = new HashSet<String>();
        }
        node.setLocs(locs, true);
    }

    /**
     * Add this "node" to the tree. Assumes that this method is called only once for each child node.
     * 
     * @param child the child in this relationship.
     * @param parentID the parent's identifier in this relationship.
     * @param descriptionSpecialNode the description of the special node.
     */
    public void addNode(Node child, List<String> parentIDs, String descriptionSpecialNode)
    {

        // First of all, get the child node if it already exists from the
        // dictionary.
        // If the child node does not already exist, add to the list of nodes
        // to watch out for. Note that the information in this node should
        // be complete since this node is being encountered as a child.
        Node childref = this.identifierToNode.get(child.getIdentifier());
        if (childref != null) {

            child.copyInfoExceptChildrenTo(childref);
        } else {

            // Put into dictionary if node not seen yet.
            childref = child;
            this.identifierToNode.put(childref.getIdentifier(), childref);
        }

        markLocations(childref);

        // For each parent, mark this as their child.
        for (String parentID : parentIDs) {

            // If the parent has already been encountered, retrieve the record.
            // Otherwise, create a record in the dictionary.
            Node parentRef = this.identifierToNode.get(parentID);
            if (parentRef == null) {

                parentRef = new Node(parentID, descriptionSpecialNode, true);
                this.identifierToNode.put(parentID, parentRef);
            }

            // Mark the child. This is the easy part.
            parentRef.addChild(childref);
        }

    }

    /**
     * Add a (true) root to this tree.
     * 
     * @param root
     */
    public void addRoot(Node root, String descriptionSpecialNode)
    {

        // See if we already have this root in a map. If so, use it; otherwise,
        // put the root into the dictionary.
        Node rootRef = this.identifierToNode.get(root.getIdentifier());
        if (rootRef == null) {

            rootRef = root;
            this.identifierToNode.put(rootRef.getIdentifier(), rootRef);
        } else {
            root.copyInfoExceptChildrenTo(rootRef);
            root = rootRef;
        }

        // Mark the location of this node.
        markLocations(root);

        root.setSpecialNode(descriptionSpecialNode);
        // Add this node as a child to the fake root.
        this.fakeRoot.addChild(root);

    }

    /**
     * Provides the option of propagating up the genome locations. This is an optional operation.
     */
    public void propagateUp()
    {
        // System.out.println("Propagating up the locations in this tree.");
        // send to recursive function.
        setLocations(this.fakeRoot);
    }

    /**
     * Set the locations of this node. This is done recursively.
     * 
     * @param crt the present node to consider.
     * @return all locations associated with this node.
     */
    private void setLocations(Node crt)
    {

        // get all the children of the present node.
        TreeSet<Node> children = crt.getChildren();

        // locations for this node.
        HashSet<String> currLocs = crt.getLocs();

        // for each child:
        for (Node child : children) {

            // If this node is special, there is no need to do anything (as
            // the gene locations of this child are exactly the same as those of
            // the parent at this point in time.
            if (child.isSpecialNode()) {
                continue;
            }

            // if this node has not already been discovered.
            if (!child.isDiscovered()) {
                // apply this function to this node.
                setLocations(child);
            }

            // add all locations of this child to this parent node
            HashSet<String> childLocs = child.getLocs();
            currLocs.addAll(childLocs);
        }

        crt.setLocs(currLocs, false);

        // say that this node has been discovered.
        crt.discover();
    }

    public Node get(String identifier)
    {
        return this.identifierToNode.get(identifier);
    }

    public Set<Node> getNodeAndAncestors(String identifier)
    {
        Set<Node> result = new LinkedHashSet<Node>();
        Node node = this.identifierToNode.get(identifier);
        while (node != null && !node.equals(this.fakeRoot)) {
            result.add(node);
            node = node.getParent();
        }
        return result;
    }

    public Set<String> getNodeAndAncestorsIds(String identifier)
    {
        Set<String> result = new LinkedHashSet<String>();
        Node node = this.identifierToNode.get(identifier);
        while (node != null && !node.equals(this.fakeRoot)) {
            result.add(node.getIdentifier());
            node = node.getParent();
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        for (String s : this.identifierToNode.keySet()) {
            str.append(s).append(" : ").append(this.identifierToNode.get(s)).append('\n');
        }
        return str.toString();
    }

}
