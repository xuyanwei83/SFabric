package org.opendaylight.controller.fabric.flowsimple;

import java.util.Collection;

import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.model.rev140402.fabric.datas.FabricData;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

public interface FlowWriterServiceSimple {
    public FabricData addNodeToNodeFabricFlows(NodeId srcNodeId, NodeId dstNodeId, long tag);
    public void clearAllFabricFlows(Collection<NodeId> nodeIdCollection);
}
