package org.opendaylight.controller.fabric;

import java.util.List;
import java.util.Set;

import org.opendaylight.controller.fabric.topology.ConnectorServiceNew;
import org.opendaylight.controller.fabric.util.SrcDstMap;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

public interface FabricService {
    //Base
    public boolean setupFabric();
    public boolean deleteFabric();
    public boolean updateFabric();
    public boolean resetupFabricWithOutLink(long fabricId,Link link);
    //Nodes
    public void setOutterFabricNodes(Set<NodeId> nodeSet);
    public Set<NodeId> getOutterFabricNodes();
    public void removeOutterFabricNode(Set<NodeId> nodeSet);
    public void addOutterFabricNode(Set<NodeId> nodeSet);
    public Set<NodeId> getFabricNodes();
    //Status
    public void enableFabricFromTopo();
    public void disableFabricFromTopo();
    public boolean getFabricFromTopoStatus();
    public void enableFabric();
    public void disableFabric();
    public boolean getFabricStatus();
    //Others
    public Set<SrcDstMap> getAllFabrics();
    public Set<SrcDstMap> getFabricsByMiddleLink(Link link);
    public Set<SrcDstMap> getFabricsByMiddleSrcDst(String src,String dst);
    public List<Link> getFabricPathBySrcDst(NodeId src,NodeId dst);
    public List<Link> getFabricPathBySrcDst(SrcDstMap map);
    public void updateFabricPathBySrcDst(NodeId src,NodeId dst, List<Link> path);
    public void updateFabricPathBySrcDst(SrcDstMap map, List<Link> path);
    public List<Link> getNewFabricPathBySrcDstWithOutBusy(NodeId src,NodeId dst);
    //Ids
    public Long getFabricIdByNodeId(NodeId nodeId);
    public NodeId getNodeIdByFabricId(Long id);
    //new
    public ConnectorServiceNew getConnectorService();
    public void downloadFabricFlow(NodeId nodeId);
}
