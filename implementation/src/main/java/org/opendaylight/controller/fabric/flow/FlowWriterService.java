/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.fabric.flow;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

/**
 * Service that adds packet forwarding flows to configuration data store.
 */
public interface FlowWriterService {

    public List<Link> addNodeToNodeFabricFlows(NodeId srcNodeId, NodeId dstNodeId, long tag);
    public void addNodeToNodeFabricFlows(List<Link> linkList, long tag);
    public void addNodeFabricBaseFlows(NodeId nodeIdnet,Set<String> internalNodeConnectors);
    public void addNodeFabricBaseFlowsByLinks(List<Link> listLink);
    public void clearAllFlows(Collection<NodeId> nodeIdCollection);
    public void deleteNodeFabricFlows(String nodeId, long tag);
    ///////////////
    // store flows
    public void addFabricLastFlow(NodeId nodeId,long tag);
    public void addFabricMiddleFlow(NodeId nodeId,long tag,Uri destPort);
    public void addFabricFirstFlow(NodeId nodeId,long tag,Uri destPort);
    // down load stored flows
    public void downloadAllFabricFlows();
    public void downloadFabricFlowsByNodeId(NodeId nodeId,long sleepTime);
    //////////////
    // add temp flows
    public void addTempFirstFlow(NodeId nodeId,long tag,Uri destPort);
    public void addTempMiddleFlow(NodeId nodeId,long tag,Uri destPort);
}