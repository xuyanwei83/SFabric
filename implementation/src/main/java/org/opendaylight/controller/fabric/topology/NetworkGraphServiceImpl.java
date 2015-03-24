/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.fabric.topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;

/**
 * Implementation of NetworkGraphService
 * It uses Jung graph library internally to maintain a graph and optimum way to return shortest path using
 * Dijkstra algorithm.
 */
public class NetworkGraphServiceImpl implements NetworkGraphService {

    private final Logger _logger = LoggerFactory.getLogger(NetworkGraphServiceImpl.class);
    private DijkstraShortestPath<NodeId, Link> shortestPath = null;
    private Graph<NodeId, Link> networkGraph = null;
    private Topology topology = null;

    /**
     * Adds links to existing graph or creates new directed graph with given links if graph was not initialized.
     * @param links
     */
    @Override
    public synchronized void addLinks(List<Link> links) {
        if(links == null || links.isEmpty()) {
            this._logger.info("In addLinks: No link added as links is null or empty.");
            return;
        }
        if(this.networkGraph == null) {
            this.networkGraph = new DirectedSparseGraph<>();
        }

        for(Link link : links) {
            NodeId sourceNodeId = link.getSource().getSourceNode();
            NodeId destinationNodeId = link.getDestination().getDestNode();
            this.networkGraph.addVertex(sourceNodeId);
            this.networkGraph.addVertex(destinationNodeId);
            this.networkGraph.addEdge(link, sourceNodeId, destinationNodeId);
        }
        if(this.shortestPath == null) {
            this.shortestPath = new DijkstraShortestPath<>(this.networkGraph);
        } else {
            this.shortestPath.reset();
        }
    }

    /**
     * removes links from existing graph.
     * @param links
     */
    @Override
    public synchronized void removeLinks(List<Link> links) {
        Preconditions.checkNotNull(networkGraph, "Graph is not initialized, add links first.");

        if(links == null || links.isEmpty()) {
            this._logger.info("In removeLinks: No link removed as links is null or empty.");
            return;
        }

        for(Link link : links) {
            this.networkGraph.removeEdge(link);
        }

        if(this.shortestPath == null) {
            this.shortestPath = new DijkstraShortestPath<>(networkGraph);
        } else {
            this.shortestPath.reset();
        }
    }

    /**
     * returns a path between 2 nodes. Uses Dijkstra's algorithm to return shortest path.
     * @param sourceNodeId
     * @param destinationNodeId
     * @return
     */
    @Override
    public synchronized List<Link> getPath(NodeId sourceNodeId, NodeId destinationNodeId) {
        Preconditions.checkNotNull(shortestPath, "Graph is not initialized, add links first.");
        List<Link> ret = null;

        if(sourceNodeId == null || destinationNodeId == null) {
            this._logger.info("In getPath: returning null, as sourceNodeId or destinationNodeId is null.");
            return null;
        }

        try{
             ret = this.shortestPath.getPath(sourceNodeId, destinationNodeId);
        }catch(Exception ex){
            return null;
        }
        return ret;
    }

    @Override
    public synchronized Collection<Link> getLinkByNodeId(NodeId id) {
        return this.networkGraph.getOutEdges(id);
    }
    /**
     *
     */
    @Override
    public synchronized void clear() {
        this.networkGraph = null;
        this.shortestPath = null;
        this.topology = null;
    }

    @Override
    public List<Node> getNodes() {
        // TODO Auto-generated method stub
        return this.topology == null?null:this.topology.getNode();
    }

    @Override
    public List<Link> getLinks() {
        // TODO Auto-generated method stub
        return this.topology == null?null:this.topology.getLink();
    }

    @Override
    public synchronized void updateTopology(Topology topology) {
        // TODO Auto-generated method stub
        this.topology = topology;
        return;
    }

    @Override
    public Collection<Link> getLinksByNodeId(NodeId id) {
        // TODO Auto-generated method stub
        Collection<Link> ret = null;
        try{
            ret = this.networkGraph.getInEdges(id);
        }catch(Exception ex){
        }
        return ret;
    }

    @Override
    public List<NodeId> getNeighborsByNodeId(NodeId id) {
        // TODO Auto-generated method stub
        ArrayList<NodeId> ret = new ArrayList<NodeId>();
        if(this.networkGraph != null){
            ret.addAll(this.networkGraph.getNeighbors(id));
        }
        return ret;
    }

    public List<NodeId> getNodeIds() {
        ArrayList<NodeId> ret = new ArrayList<NodeId>();
        if(this.networkGraph != null){
            this.networkGraph.getVertices();
        }
        return ret;
    }
}
