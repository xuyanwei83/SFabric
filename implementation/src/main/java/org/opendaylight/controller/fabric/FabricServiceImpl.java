package org.opendaylight.controller.fabric;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.fabric.flow.FlowWriterService;
import org.opendaylight.controller.fabric.topology.ConnectorServiceNew;
import org.opendaylight.controller.fabric.topology.NetworkGraphService;
import org.opendaylight.controller.fabric.util.SrcDstMap;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.enumeration.rev140402.FabricTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.map.rev140402.FabricMaps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.map.rev140402.FabricMapsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.map.rev140402.fabric.maps.FabricMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.map.rev140402.fabric.maps.FabricMapBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricServiceImpl implements FabricService{
    private final Logger logger = LoggerFactory.getLogger(FabricServiceImpl.class);
    private final DataBrokerService dataBrokerService;
    private final FlowWriterService flowWriterService;
    private final NetworkGraphService networkGraphService;

    private final HashSet<NodeId> currentNodeSet;
    private final HashSet<NodeId> outterNodeSet;
    private final Hashtable<Long,NodeId> tagMapNodeId;
    private final Hashtable<NodeId,Long> nodeIdMapTag;
    private final Hashtable<SrcDstMap,List<Link>> linksInSrcDst;
    private Long currentId = (long) 2;
    private boolean nodesFromTopo = true;
    private boolean fabircStatus = true;
    /////////////////////////////////////////////////////////////////////
    //
    // Construction functions
    //
    /////////////////////////////////////////////////////////////////////
    /**
     * Construction function
     * Initialize parameters: dataBrokerService,inventoryService
     * @param dataBrokerService
     * @param inventoryService
     * @param flowWriterService
     */
    public FabricServiceImpl(DataBrokerService dataBrokerService,
            FlowWriterService flowWriterService,
            NetworkGraphService networkGraphService){
        this.dataBrokerService = dataBrokerService;
        this.flowWriterService = flowWriterService;
        this.networkGraphService = networkGraphService;

        // initialize parameters
        this.tagMapNodeId = new Hashtable<Long,NodeId>();
        this.nodeIdMapTag = new Hashtable<NodeId,Long>();
        this.currentNodeSet = new HashSet<NodeId>();
        this.outterNodeSet = new HashSet<NodeId>();
        this.linksInSrcDst = new Hashtable<SrcDstMap,List<Link>>();
        this.nodesFromTopo = true;
        this.fabircStatus = true;
        this.initialize();
        return;
    }
    private void initialize(){
        // initialize all collections
        this.tagMapNodeId.clear();
        this.nodeIdMapTag.clear();
        this.linksInSrcDst.clear();
        this.currentId = (long) 2;
        // clear data store
        this.removeFabricMaps();
    }

    /////////////////////////////////////////////////////////////////////
    //
    // Public functions
    //
    /////////////////////////////////////////////////////////////////////
    @Override
    public boolean setupFabric() {
        // TODO Auto-generated method stub
        //logger.info("Fabric-implementation: SetupFabric Start!");
        if(!this.fabircStatus){
            return false;
        }
        // get current nodes
        this.getFabricNodes();

        // if current nodes is empty, return;
        if(this.currentNodeSet.isEmpty()){
            return true;
        }
        // assign the id to each node
        for(NodeId id : this.currentNodeSet){
            this.tagMapNodeId.put(this.currentId, id);
            this.nodeIdMapTag.put(id, this.currentId);
            this.currentId++;
        }

        // add nodeId-id maps to DataStore
        this.initFabricMaps();

        // add base flows
        this.initBaseFLows();

        // add fabric flows
        this.initFabricFlows();

        logger.info("Fabric-implementation: SetupFabric End!");
        return true;
    }

    @Override
    public boolean deleteFabric() {
        // TODO Auto-generated method stub
        logger.info("Fabric-implementation: DeleteFabric Start!");
        //Clear flows
        Collection<NodeId> c= this.tagMapNodeId.values();
        if( c.isEmpty() ){
            return true;
        }
        this.flowWriterService.clearAllFlows(c);

        //Clear locals data
        this.initialize();
        logger.info("Fabric-implementation: DeleteFabric End!");
        return true;
    }

    @Override
    public boolean updateFabric() {
        // TODO Auto-generated method stub
        this.deleteFabric();
        this.setupFabric();
        return true;
    }
    /////////////////////////////////////////////////////////////////////
    //
    // Public functions : others
    //
    /////////////////////////////////////////////////////////////////////
    @Override
    public Set<SrcDstMap> getAllFabrics(){
        return this.linksInSrcDst.keySet();
    }

    @Override
    public List<Link> getFabricPathBySrcDst(NodeId src,NodeId dst){
        return this.linksInSrcDst.get(new SrcDstMap(src,dst,0L));
    }
    @Override
    public List<Link> getFabricPathBySrcDst(SrcDstMap map){
        return this.linksInSrcDst.get(map);
    }
    @Override
    public void updateFabricPathBySrcDst(NodeId src,NodeId dst, List<Link> path) {
        // TODO Auto-generated method stub
        return;
    }
    @Override
    public void updateFabricPathBySrcDst(SrcDstMap map, List<Link> path) {
        // TODO Auto-generated method stub
        return;
    }
    /////////////////////////////////////////////////////////////////////
    //
    // Public functions : ids
    //
    /////////////////////////////////////////////////////////////////////
    @Override
    public Long getFabricIdByNodeId(NodeId nodeId){
        return this.nodeIdMapTag.get(nodeId);
    }
    @Override
    public NodeId getNodeIdByFabricId(Long id){
        return this.tagMapNodeId.get(id);
    }
    /////////////////////////////////////////////////////////////////////
    //
    // Public functions : nodes
    //
    /////////////////////////////////////////////////////////////////////
    @Override
    public void setOutterFabricNodes(Set<NodeId> nodeSet){
        this.outterNodeSet.clear();
        this.outterNodeSet.addAll(nodeSet);
        return;
    }
    @Override
    public Set<NodeId> getOutterFabricNodes(){
        return this.outterNodeSet;
    }
    @Override
    public void removeOutterFabricNode(Set<NodeId> nodeSet){
        this.outterNodeSet.removeAll(nodeSet);
    }
    @Override
    public void addOutterFabricNode(Set<NodeId> nodeSet){
        this.outterNodeSet.addAll(nodeSet);
    }
    /**
     * Get the nodes of fabric
     * Warning: now get all topology nodes
     * @return not null
     */
    @Override
    public Set<NodeId> getFabricNodes(){
        return this.nodesFromTopo?this.getFabricNodesFromTopo():this.getFabricNodesFromOutter();
    }
    /////////////////////////////////////////////////////////////////////
    //
    // Public functions : status
    //
    /////////////////////////////////////////////////////////////////////
    @Override
    public void enableFabricFromTopo(){
        this.nodesFromTopo = true;
    }
    @Override
    public void disableFabricFromTopo(){
        this.nodesFromTopo = false;
    }
    @Override
    public boolean getFabricFromTopoStatus(){
        return this.nodesFromTopo;
    }
    @Override
    public void enableFabric(){
        this.fabircStatus = true;
    }
    @Override
    public void disableFabric(){
        this.fabircStatus = false;
    }
    @Override
    public boolean getFabricStatus(){
        return this.fabircStatus;
    }
    /////////////////////////////////////////////////////////////////////
    //
    // Private functions
    //
    /////////////////////////////////////////////////////////////////////
    /**
     * setup fabric flows
     */
    private void initFabricFlows(){
        HashSet<Long> ids = new HashSet<Long>(this.tagMapNodeId.keySet());

        // create multi-thread to creat each node to others' path
        // install each path into the switch
        for(Long srcId : ids){
            new Thread(new InstallFabric(srcId,tagMapNodeId,flowWriterService,linksInSrcDst)).start();
        }

        return;
    }

    /**
     * setup fabric base flows
     */
    private void initBaseFLows(){
        List<Link> links = this.networkGraphService.getLinks();
        Set<String> internalNodeConnectors = new HashSet<>();
//        InstanceIdentifier<Topology> topologyPath = InstanceIdentifier.builder(NetworkTopology.class)
//                .child(Topology.class, new TopologyKey(new TopologyId(DEFAULT_TOPOLOGY_ID))).toInstance();
//        Topology completeTopology =(Topology)this.dataBrokerService.readOperationalData(topologyPath);
        for (Link link : links) {
            internalNodeConnectors.add(link.getDestination().getDestTp().getValue());
            internalNodeConnectors.add(link.getSource().getSourceTp().getValue());
        }
        for (NodeId id : this.tagMapNodeId.values()){
            //this.networkGraphService.getPath(id, destinationNodeId)
            this.flowWriterService.addNodeFabricBaseFlows(id, internalNodeConnectors);
        }
        return;
    }

    /**
     * Initialize the nodeId-id maps
     * And write the maps to Operational Data Store
     * @return
     */
    private void initFabricMaps(){

        //Set fabric map nodeId-id
        List<FabricMap> lFabricMap = new ArrayList<FabricMap>();
        FabricMapBuilder fmb = new FabricMapBuilder();

        // circle the hash table
        Iterator<Entry<NodeId,Long>> iter = this.nodeIdMapTag.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<NodeId,Long> entry = (Map.Entry<NodeId,Long>) iter.next();
            FabricMap fm = fmb.setId(entry.getValue())
                                .setNodeId(entry.getKey())
                                .build();
            lFabricMap.add(fm);
        }
        FabricMaps maps = new FabricMapsBuilder().setFabricMap(lFabricMap).build();

        // submit to Operational Data Store!
        DataModificationTransaction it = this.dataBrokerService.beginTransaction();
        InstanceIdentifier<FabricMaps> dataRef = InstanceIdentifier.builder(FabricMaps.class).toInstance();
        it.putOperationalData(dataRef, maps);
        it.commit();

        // return
        return;
    }
    /**
     * Remove all nodeId and id maps
     * @return
     */
    private void removeFabricMaps(){
        InstanceIdentifier<FabricMaps> dataRef = InstanceIdentifier.builder(FabricMaps.class).toInstance();
        FabricMaps fabricMaps = (FabricMaps)this.dataBrokerService.readOperationalData(dataRef);
        if(fabricMaps != null){
            DataModificationTransaction it = this.dataBrokerService.beginTransaction();
            it.removeOperationalData(dataRef);
            it.commit();
        }
        return;
    }

    /**
     * Get All nodes from outter
     * @return not null
     */
    private Set<NodeId> getFabricNodesFromOutter(){
        // clear all ths current node set
        this.currentNodeSet.clear();
        // get current outter nodes
        this.currentNodeSet.addAll(this.outterNodeSet);
        return this.currentNodeSet;
    }
    /**
     * Get All nodes from topology
     * @return not null
     */
    private Set<NodeId> getFabricNodesFromTopo(){
        // clear all the current node set
        this.currentNodeSet.clear();
        // get current topology nodes
        List<Node> listNode = this.networkGraphService.getNodes();
        if(listNode != null){
            for(Node node : listNode){
                this.currentNodeSet.add(node.getNodeId());
            }
        }
        return this.currentNodeSet;
    }
    /////////////////////////////////////////////////////////////////////
    //
    // private class
    //
    /////////////////////////////////////////////////////////////////////

    /**
     * create a thread to create fabric flows
     * @author zhaoliangzhi
     *
     */
    private class InstallFabric implements Runnable{
        private final Hashtable<Long,NodeId> idMapNodeId;
        private final long id;
        private final FlowWriterService flowWriterService;
        private final Hashtable<SrcDstMap,List<Link>> linksInSrcDst;
        public InstallFabric(long id,
                Hashtable<Long,NodeId> idMapNodeId,
                FlowWriterService flowWriterService,
                Hashtable<SrcDstMap,List<Link>> linksInSrcDst){
            this.idMapNodeId = new Hashtable<Long,NodeId>(idMapNodeId);
            this.linksInSrcDst = linksInSrcDst;
            this.id = id;
            this.flowWriterService = flowWriterService;
            return;
        }

        @Override
        public void run() {
            NodeId srcNodeId = this.idMapNodeId.get(id);
            // TODO Auto-generated method stub
            HashSet<Long> ids = new HashSet<Long>(this.idMapNodeId.keySet());
            ids.remove(this.id);
            Iterator<Long> itIds = ids.iterator();
            while(itIds.hasNext()){
                Long dstId = itIds.next();
                long t = id<<FabricTag.VLAN.getIntValue();
                long tag = t + dstId.longValue();
                NodeId dstNodeId = this.idMapNodeId.get(dstId);
                //List<Link> linkList=this.flowWriterService.addNodeToNodeFabricFlows(srcNodeId,dstNodeId, tag);
                List<Link> linkList= networkGraphService.getPath(srcNodeId, dstNodeId);
                if(linkList != null){
                    this.flowWriterService.addNodeToNodeFabricFlows(linkList, tag);
                    SrcDstMap s = new SrcDstMap(srcNodeId,dstNodeId,tag);
                    this.linksInSrcDst.put(s, linkList);
                }
            }
        }
    }

    @Override
    public Set<SrcDstMap> getFabricsByMiddleLink(Link link) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public boolean resetupFabricWithOutLink(long fabricId, Link link) {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public ConnectorServiceNew getConnectorService() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public void downloadFabricFlow(NodeId nodeId) {
        // TODO Auto-generated method stub
    }
    @Override
    public Set<SrcDstMap> getFabricsByMiddleSrcDst(String src, String dst) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public List<Link> getNewFabricPathBySrcDstWithOutBusy(NodeId src, NodeId dst) {
        // TODO Auto-generated method stub
        return null;
    }
}
