package model;

import java.util.*;

/**
 * A strategy to finding an optimal path between two location on a floor. This
 * strategy explores nodes in a recursive fashion choosing the "most efficient"
 * node to explore next. The most efficient node is determined by a combination
 * of heuristic (closest direct distance) and cost (number of steps it took to
 * travel to the node).
 */
public class PathFindingStrategy {
	private boolean avoidCollisions;
	private Floor floor;
	private Deque<Location> path;
	private Node[][] floorNodes;

	List<Node> unexploredNodes;
	List<Node> exploredNodes;

	/**
	 * Constructs a strategy for path finding with a default value to avoid
	 * collisions.
	 * 
	 * @param floor the floor determining the size and location of other robots.
	 */
	public PathFindingStrategy(Floor floor) {
		this(floor, true);
	}

	/**
	 * Constructs a strategy for path finding.
	 * 
	 * @param floor           the floor determining the size and location of other
	 *                        robots.
	 * @param avoidCollisions a flag allowing the search to avoid location of
	 *                        current robots.
	 */
	public PathFindingStrategy(Floor floor, boolean avoidCollisions) {
		this.floor = floor;
		this.avoidCollisions = avoidCollisions;
		this.path = new LinkedList<Location>();
		this.floorNodes = PathFindingStrategy.buildFloorWithNodes(this.floor.getNumberOfColumns(),
				this.floor.getNumberOfRows());
	}

	/**
	 * Given a number of columns and rows (typically for the Floor), this recreates
	 * it with Nodes used by the searching.
	 * 
	 * This is a static method as it does not use or modify anything locally.
	 * 
	 * @param columns the number to columns in the floor.
	 * @param rows    the number of rows in the floor.
	 * @return a two dimensional array storing the nodes in their grid format.
	 */
	private static Node[][] buildFloorWithNodes(int columns, int rows) {
		Node[][] nodes = new Node[columns][rows];

		for (int column = 0; column < columns; column++) {
			for (int row = 0; row < rows; row++) {
				nodes[column][row] = new Node(column, row);
			}
		}

		return nodes;
	}

	/**
	 * Given two location, this will find an optimal path between them.
	 * 
	 * @param beginningLocation the Location to start the search.
	 * @param targetLocation    the Location to end the search.
	 * @return a boolean value; true if a path was found, otherwise false.
	 * @throws LocationNotValidException
	 */
	public boolean calculatePath(Location beginningLocation, Location targetLocation) {

		// If both locations are the same, return true (with a path count of 0)
		if (beginningLocation.equals(targetLocation))
			return false;

		// Create lists for storing nodes to explore and those explored
		// TODO add flag to node class?
		this.unexploredNodes = new ArrayList<Node>(this.floor.getNumberOfColumns() * this.floor.getNumberOfRows());
		this.exploredNodes = new ArrayList<Node>(this.floor.getNumberOfColumns() * this.floor.getNumberOfRows());

		// Convert Locations to Nodes
		Node beginningNode = this.getNodeAtLocation(beginningLocation);
		Node targetNode = this.getNodeAtLocation(targetLocation);

		// Add current location to begin search
		this.unexploredNodes.add(beginningNode);

		searchForPath(targetLocation);

		// No path was found
		if (targetNode.getParent() == null)
			return false;

		// Populate the path with calculated route.
		this.path = PathFindingStrategy.convertLinkedNodesToPath(beginningNode, targetNode);

		return true;

	}

	/**
	 * Returns a node at a given x, y coordinates. Returns null if the given
	 * coordinates are Out Of Bounds
	 * 
	 * @param column
	 * @param row
	 * @return
	 */
	private Node getNodeAtLocation(int column, int row) {
		try {
			return this.floorNodes[column][row];
		} catch (ArrayIndexOutOfBoundsException execption) {
			return null;
		}
	}

	/**
	 * Returns a node at a given Location. Returns null if the given Location is Out
	 * Of Bounds
	 * 
	 * @param location
	 * @return
	 */
	private Node getNodeAtLocation(Location location) {
		return this.getNodeAtLocation(location.getColumn(), location.getRow());
	}

	/**
	 * Begins the recursive like search through the unexplored Nodes.
	 * 
	 * @param targetLocation the location used to determine success.
	 */
	private void searchForPath(Location targetLocation) {
		Node targetNode = this.getNodeAtLocation(targetLocation);
		do {
			Collections.sort(this.unexploredNodes);

			Node currentNode = this.unexploredNodes.get(0);

			// Found target, breaking out to stop search
			if (currentNode.toLocation().equals(targetLocation))
				break;

			int nextStepCost = currentNode.getCost() + 1;

			this.checkNodeForExplorationInDirection(nextStepCost, currentNode, targetNode, +0, -1); // ABOVE
			this.checkNodeForExplorationInDirection(nextStepCost, currentNode, targetNode, +1, +0); // RIGHT
			this.checkNodeForExplorationInDirection(nextStepCost, currentNode, targetNode, +0, +1); // BELOW
			this.checkNodeForExplorationInDirection(nextStepCost, currentNode, targetNode, -1, +0); // LEFT

			// Move current node to explored.
			this.unexploredNodes.remove(currentNode);
			this.exploredNodes.add(currentNode);

		} while (unexploredNodes.size() > 0);
	}

	/**
	 * A wrapper around checkNodeForExploration(...) defining the current node as
	 * the relative position change from the previous node.
	 * 
	 * @param nextStepCost the cost taken to travel to this node.
	 * @param previous     the previous node to the one being explored.
	 * @param target       the target node used to determine heuristic later.
	 * @param columnChange the relative change in column.
	 * @param rowChange    the relative change in row.
	 */
	private void checkNodeForExplorationInDirection(int nextStepCost, Node previous, Node target, int columnChange,
			int rowChange) {
		Node current = this.getNodeAtLocation(previous.getX() + columnChange, previous.getY() + rowChange);
		if (current != null)
			this.checkNodeForExploration(current, nextStepCost, previous, target);
	}

	/**
	 * Adding a node to explore checking the location is valid, it hasn't been
	 * explored already.
	 * 
	 * @param current      the current node to explore.
	 * @param nextStepCost the cost taken to travel to this node.
	 * @param previous     the previous node to the one being explored.
	 * @param target       the target node used to determine heuristic later.
	 */
	private void checkNodeForExploration(Node current, int nextStepCost, Node previous, Node target) {
		// Check location validity
		if (!this.floor.locationIsValid(current.toLocation()))
			return;
		if (this.avoidCollisions && !this.floor.locationIsEmpty(current.toLocation()))
			return;

		// If the node's current cost is greater than the nextStepCost, remove from
		// lists as a more efficient path has been found.
		if (nextStepCost < current.getCost()) {
			this.exploredNodes.remove(current);
			this.unexploredNodes.remove(current);
		}

		// This node has already been handled.
		if (this.unexploredNodes.contains(current) || this.exploredNodes.contains(current))
			return;

		current.setCost(nextStepCost);
		current.setHeuristic(PathFindingStrategy.calculateHeuristic(current, target));
		current.setParent(previous);
		this.unexploredNodes.add(current);
	}

	/**
	 * Converts the linked nodes to a path stack.
	 * 
	 * This is static because it doesn't use or modify anything locally.
	 * 
	 * @param start the beginning node of the path (used as a stopping condition)
	 * @param end   the target node of the path (used as the initial point)
	 */
	private static Deque<Location> convertLinkedNodesToPath(Node start, Node end) {
		Deque<Location> path = new LinkedList<Location>();
		Node currentNode = end;

		while (!currentNode.equals(start)) {
			path.addFirst(currentNode.toLocation());
			currentNode = currentNode.getParent();
		}

		return path;
	}

	/**
	 * Calculates the heuristic between the two given nodes. The heuristic is simply
	 * the direct distance between the two nodes calculated using Pythagoras
	 * Theorem.
	 * 
	 * This is static because it doesn't use or modify anything locally.
	 * 
	 * @param n1 the first Node.
	 * @param n2 the second Node.
	 * @return the direct distance between them.
	 */
	private static double calculateHeuristic(Node n1, Node n2) {
		int differenceInColumns = Math.abs(n1.getX() - n2.getX());
		int differenceInRows = Math.abs(n1.getY() - n2.getY());

		return Math.sqrt(differenceInRows ^ 2 + differenceInColumns ^ 2);
	}

	/**
	 * @return the entire path.
	 */
	public Collection<Location> getPath() {
		return this.path;
	}

	/**
	 * @return the next step in the path (which is removed).
	 */
	public Location getNextLocation() {
		return this.path.pop();
	}

	/**
	 * @return the number of steps in the path.
	 */
	public int getNumberOfRemainingSteps() {
		return this.path.size();
	}

}