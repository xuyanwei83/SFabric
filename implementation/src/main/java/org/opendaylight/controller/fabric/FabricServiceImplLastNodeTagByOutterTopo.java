package org.opendaylight.controller.fabric;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.count.CountProvider;
import org.opendaylight.controller.count.CountService;
import org.opendaylight.controller.fabric.flow.FlowWriterService;
import org.opendaylight.controller.fabric.topology.ConnectorServiceNew;
import org.opendaylight.controller.fabric.topology.NetworkGraphService;
import org.opendaylight.controller.fabric.util.SrcDstMap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.topologystatic.TopologyStaticProvider;
import org.opendaylight.controller.topologystatic.TopologyStaticService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.map.rev140402.FabricMaps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.map.rev140402.FabricMapsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.map.rev140402.fabric.maps.FabricMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fabric.map.rev140402.fabric.maps.FabricMapBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricServiceImplLastNodeTagByOutterTopo implements FabricService {
    private final Logger logger = LoggerFactory.getLogger(FabricServiceImplLastNodeTagByOutterTopo.class);
    private final DataBroker dataBroker;
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

    private TopologyStaticService topologyStaticService;
    private ConnectorServiceNew connectorService;
    private HashSet<String> coreNodeSet;
    private CountService countService;
    private final Hashtable<SrcDstMap,List<Link>> tempInSrcDst;
    //////////
    //private TopoToFabric topoToFabric = new TopoToFabric();
    /////////////////////////////////////////////////////////////////////
    //
    // Construction functions
    //
    /////////////////////////////////////////////////////////////////////
    /**
     * Construction function
     * Initialize parameters: dataBroker,inventoryService
     * @param dataBroker
     * @param inventoryService
     * @param flowWriterService
     */
    public FabricServiceImplLastNodeTagByOutterTopo(DataBroker dataBroker,
            FlowWriterService flowWriterService,
            NetworkGraphService networkGraphService){
        this.dataBroker = dataBroker;
        this.flowWriterService = flowWriterService;
        this.networkGraphService = networkGraphService;

        // initialize parameters
        this.tagMapNodeId = new Hashtable<Long,NodeId>();
        this.nodeIdMapTag = new Hashtable<NodeId,Long>();
        this.currentNodeSet = new HashSet<NodeId>();
        this.outterNodeSet = new HashSet<NodeId>();
        this.linksInSrcDst = new Hashtable<SrcDstMap,List<Link>>();
        this.tempInSrcDst = new Hashtable<SrcDstMap,List<Link>>();
        this.nodesFromTopo = true;
        this.fabircStatus = true;
        this.connectorService = new ConnectorServiceNew();
        this.coreNodeSet = new HashSet<String>();
        this.initCoreNodeSet();
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
        //this.removeFabricMaps();
    }
    private void initCoreNodeSet(){
        this.coreNodeSet.add("openflow:11259204");
        this.coreNodeSet.add("openflow:10489897");
    }
    private void initTopo(){
        if(this.topologyStaticService == null){
            BundleContext bCtx = FrameworkUtil.getBundle(TopologyStaticProvider.class).getBundleContext();
            ServiceReference<TopologyStaticService> srf = bCtx.getServiceReference(TopologyStaticService.class);
            this.topologyStaticService = (TopologyStaticService)bCtx.getService(srf);
        }
        this.networkGraphService.clear();
        this.networkGraphService.addLinks(this.topologyStaticService.getTopoloy().getLink());
        this.networkGraphService.updateTopology(this.topologyStaticService.getTopoloy());
        this.connectorService.update(this.topologyStaticService.getTopoloy());
        return;
    }

    /////////////////////////////////////////////////////////////////////
    //
    // Public functions
    //
    /////////////////////////////////////////////////////////////////////
    @Override
    public boolean setupFabric() {
        // TODO Auto-generated method stub
        logger.info("Fabric-implementation: SetupFabric Start!");
        this.initTopo();
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
        //this.initFabricMaps();

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
        this.initTopo();
        // delete fabricNodes
        //Set<NodeId> c = this.getFabricNodes();
        // delete all nodes flows
        Set<NodeId> c = new HashSet<NodeId>();
        List<Node> listNode = this.networkGraphService.getNodes();
        if(listNode == null || listNode.size() == 0){
            return true;
        }
        for(Node node : listNode){
            c.add(node.getNodeId());
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
    @Override
    public boolean resetupFabricWithOutLink(long fabricId,Link link){
        this.deleteFabricFlowsByFabricId(fabricId);
        List<Link> links = new ArrayList<Link>();
        links.add(link);
        this.networkGraphService.removeLinks(links);
        NodeId dstNodeId = this.tagMapNodeId.get(fabricId);
        HashSet<NodeId> nodeIds = new HashSet<NodeId>(this.tagMapNodeId.values());
        nodeIds.remove(dstNodeId);
        Iterator<NodeId> srcNodeIds = nodeIds.iterator();
        while(srcNodeIds.hasNext()){
            NodeId srcNodeId = srcNodeIds.next();
            List<Link> linkList = this.flowWriterService.addNodeToNodeFabricFlows(srcNodeId, dstNodeId, fabricId);
            if(linkList != null){
                SrcDstMap s = new SrcDstMap(srcNodeId,dstNodeId,fabricId);
                this.linksInSrcDst.put(s, linkList);
            }
        }
        this.networkGraphService.addLinks(links);
        return true;
    }
    //@Override
    public void deleteFabricFlowsByFabricId(long fabricId){
        List<Node> listNode =this.networkGraphService.getNodes();
        Iterator<Node> iNode = listNode.iterator();
        while(iNode.hasNext()){
            this.flowWriterService.deleteNodeFabricFlows(iNode.next().getNodeId().getValue(), fabricId);
        }
        this.removePathByFabricId(fabricId);
        return;
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
    public Set<SrcDstMap> getFabricsByMiddleLink(Link link){
        HashSet<SrcDstMap> ret = new HashSet<SrcDstMap>();
        Set<Entry<SrcDstMap, List<Link>>> tempSet = this.linksInSrcDst.entrySet();
        Iterator<Entry<SrcDstMap, List<Link>>> iTempSet = tempSet.iterator();
        while(iTempSet.hasNext()){
            Entry<SrcDstMap, List<Link>> entry = iTempSet.next();
            if(entry.getValue().contains(link)){
                ret.add(entry.getKey());
            }
        }
        return ret;
    }
    @Override
    public Set<SrcDstMap> getFabricsByMiddleSrcDst(String src,String dst){
        HashSet<SrcDstMap> ret = new HashSet<SrcDstMap>();
        Set<Entry<SrcDstMap, List<Link>>> tempSet = this.linksInSrcDst.entrySet();
        Iterator<Entry<SrcDstMap, List<Link>>> iTempSet = tempSet.iterator();
        while(iTempSet.hasNext()){
            Entry<SrcDstMap, List<Link>> entry = iTempSet.next();
            if(checkSrcDstInLinkList(entry.getValue(),src,dst)){
                ret.add(entry.getKey());
            }
        }
        return ret;
    }
    private boolean checkSrcDstInLinkList(List<Link> listLink, String src,String dst){
        Iterator<Link> iLink = listLink.iterator();
        while(iLink.hasNext()){
            Link link = iLink.next();
            if(link.getSource().getSourceNode().getValue().equals(src) &&
                    link.getDestination().getDestNode().getValue().equals(dst))
                return true;
        }
        return false;
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
        // way 1:
        Long tag = this.getFabricIdByNodeId(dst);
        SrcDstMap temp = new SrcDstMap(src,dst,tag);
        List<Link> tempPath = this.tempInSrcDst.remove(temp);
        List<Link> currentPath = this.linksInSrcDst.get(temp);
        Link firstLink = tempPath.get(0);
        if(!firstLink.equals(currentPath.get(0))){
            this.flowWriterService.addTempFirstFlow(firstLink.getSource().getSourceNode(),
                    tag,
                    firstLink.getSource().getSourceTp());
        }
        tempPath.remove(0);
        for(Link link : tempPath){
            if(!currentPath.contains(link)){
                this.flowWriterService.addTempMiddleFlow(link.getSource().getSourceNode(),
                       tag,
                       link.getSource().getSourceTp());
            }
        }

        // way 2:
        tag = this.getFabricIdByNodeId(src);
        temp = new SrcDstMap(dst,src,tag);
        tempPath = this.tempInSrcDst.remove(temp);
        currentPath = this.linksInSrcDst.get(temp);
        firstLink = tempPath.get(0);
        if(!firstLink.equals(currentPath.get(0))){
            this.flowWriterService.addTempFirstFlow(firstLink.getSource().getSourceNode(),
                    tag,
                    firstLink.getSource().getSourceTp());
        }
        tempPath.remove(0);
        for(Link link : tempPath){
            if(!currentPath.contains(link)){
                this.flowWriterService.addTempMiddleFlow(link.getSource().getSourceNode(),
                       tag,
                       link.getSource().getSourceTp());
            }
        }
        return;
    }
    @Override
    public void updateFabricPathBySrcDst(SrcDstMap map, List<Link> path) {
        // TODO Auto-generated method stub
        return;
    }
    @Override
    public List<Link> getNewFabricPathBySrcDstWithOutBusy(NodeId src, NodeId dst) {
        // TODO Auto-generated method stub
        List<Link> busyLinks = this.getCountService().getBusyLinks();
        if(busyLinks == null){
            return this.getFabricPathBySrcDst(src, dst);
        }
        this.networkGraphService.removeLinks(busyLinks);
        List<Link> ret = this.networkGraphService.getPath(src, dst);
        List<Link> bak = this.networkGraphService.getPath(dst, src);
        this.networkGraphService.addLinks(busyLinks);
        if(ret != null){
            this.tempInSrcDst.put(new SrcDstMap(src,dst,0L), ret);
            this.tempInSrcDst.put(new SrcDstMap(dst,src,0L), bak);
        }
        return ret;
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
    @Override
    public ConnectorServiceNew getConnectorService() {
        // TODO Auto-generated method stub
        return this.connectorService;
    }
    @Override
    public void downloadFabricFlow(NodeId nodeId){
        this.flowWriterService.downloadFabricFlowsByNodeId(nodeId,100);
        return;
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

//        HashSet<Long> ids = new HashSet<Long>(this.tagMapNodeId.keySet());
//        // create multi-thread to creat each node to others' path
//        // install each path into the switch
//        for(Long srcId : ids){
//            // count path by NetworkGraphService and download flows
//            //new Thread(new InstallFabric(srcId,tagMapNodeId,flowWriterService,linksInSrcDst)).start();
//            // count path by destNode and download flows
//            //new Thread(new InstallFabricNew(srcId,tagMapNodeId,flowWriterService,linksInSrcDst,networkGraphService)).start();
//        }
        //
        this.countPathTotal();
        HashSet<NodeId> ids = new HashSet<NodeId>(this.nodeIdMapTag.keySet());
        for(NodeId nodeId : ids){
            new Thread(new InstallFabricFlow(nodeId,flowWriterService)).start();
        }
        return;
    }

    /**
     * setup fabric base flows
     */
    private void initBaseFLows(){
        List<Link> links = this.networkGraphService.getLinks();
        Set<String> internalNodeConnectors = new HashSet<>();

        for (NodeId id : this.tagMapNodeId.values()){
            this.flowWriterService.addNodeFabricBaseFlows(id, internalNodeConnectors);
        }
        this.flowWriterService.addNodeFabricBaseFlowsByLinks(links);
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
        WriteTransaction it = this.dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<FabricMaps> dataRef = InstanceIdentifier.builder(FabricMaps.class).toInstance();
        it.put(LogicalDatastoreType.OPERATIONAL,dataRef, maps);
        it.submit();

        // return
        return;
    }
    /**
     * Remove all nodeId and id maps
     * @return
     */
    private void removeFabricMaps(){
        InstanceIdentifier<FabricMaps> dataRef = InstanceIdentifier.builder(FabricMaps.class).toInstance();
        WriteTransaction it = this.dataBroker.newWriteOnlyTransaction();
        it.delete(LogicalDatastoreType.OPERATIONAL,dataRef);
        it.submit();
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
        //List<Node> listNode = this.topoToFabric.getNodes();
        if(listNode != null){
            for(Node node : listNode){
                //if(!this.isCoreNode(node.getNodeId().getValue()))
                    this.currentNodeSet.add(node.getNodeId());
            }
        }
        return this.currentNodeSet;
    }
    /**
     * remove data stored paths
     */
    private void removePathByFabricId(long fabricId){
        NodeId nodeId = this.tagMapNodeId.get(fabricId);
        HashSet<SrcDstMap> tempSrcDstMap = new HashSet<SrcDstMap>(this.linksInSrcDst.keySet());
        Iterator<SrcDstMap> iSrcDstMap = tempSrcDstMap.iterator();
        while(iSrcDstMap.hasNext()){
            SrcDstMap temp = iSrcDstMap.next();
            if(temp.getDst().equals(nodeId)){
                this.linksInSrcDst.remove(temp);
            }
        }
        return;
    }
    /**
     * checke the node is corNode
     * @return
     */
    private boolean isCoreNode(String id){
        for(String tId : this.coreNodeSet){
            if(id.equals(tId)){
                return true;
            }
        }
        return false;
    }
    private CountService getCountService(){
        if(this.countService == null){
            try{
                BundleContext bCtx = FrameworkUtil.getBundle(CountProvider.class).getBundleContext();
                ServiceReference<CountService> srf = bCtx.getServiceReference(CountService.class);
                this.countService = (CountService)bCtx.getService(srf);
            }catch(Exception ex){
            }
        }
        return this.countService;
    }
    /////////////////////////////////////////////////////////////////////
    //
    // count path
    //
    /////////////////////////////////////////////////////////////////////

    private void countPathTotal(){
        Iterator<Entry<Long, NodeId>> iEntry = this.tagMapNodeId.entrySet().iterator();
        while(iEntry.hasNext()){
            Entry<Long,NodeId> entry = iEntry.next();
            this.countPath(entry.getValue(),entry.getKey());
        }
        return;
    }
    private void countPath(NodeId dstNodeId,long id){
        HashSet<NodeId> alreadyNode = new HashSet<NodeId>();
        HashSet<NodeId> currentNode = new HashSet<NodeId>();
        HashSet<NodeId> nextNode = new HashSet<NodeId>();
        HashMap<SrcDstMap,List<Link>> currentLinksInSrcDst = new HashMap<SrcDstMap,List<Link>>();

        this.flowWriterService.addFabricLastFlow(dstNodeId, id);
        currentNode.add(dstNodeId);
        // while current node hasn't deal with
        // to find neighbours and add flows
        while(!currentNode.isEmpty()){
            Iterator<NodeId> iNodeId = currentNode.iterator();
            while(iNodeId.hasNext()){
                NodeId currentId = iNodeId.next();
                // get the current node to dst node's path from already paths
                List<Link> lastLinkList = this.getFabricLinks(currentId, dstNodeId,currentLinksInSrcDst);
                Collection<Link> neighbourLinks = this.networkGraphService.getLinksByNodeId(currentId);
                if(neighbourLinks != null){
                    boolean addMiddleFlowFlag = false;
                    Iterator<Link> iLink = neighbourLinks.iterator();
                    while(iLink.hasNext()){
                        Link temp = iLink.next();
                        NodeId srcNodeId = temp.getSource().getSourceNode();
                        if(!isAdd(alreadyNode,currentNode,nextNode,srcNodeId)){
                            addMiddleFlowFlag = true;
                            // next node list added
                            nextNode.add(srcNodeId);
                            // add links
                            SrcDstMap s = new SrcDstMap(srcNodeId,dstNodeId,id);
                            List<Link> tempLink = new ArrayList<Link>();
                            tempLink.add(temp);
                            if(lastLinkList != null){
                                tempLink.addAll(lastLinkList);
                            }
                            currentLinksInSrcDst.put(s, tempLink);
                            // add first flow
                            this.flowWriterService.addFabricFirstFlow(srcNodeId, id, temp.getSource().getSourceTp());
                        }
                    }
                    // while it has middle flow
                    //& last link path is not null,
                    // then add middle flag
                    if(addMiddleFlowFlag && lastLinkList!= null){
                        this.flowWriterService.addFabricMiddleFlow(currentId, id, lastLinkList.get(0).getSource().getSourceTp());
                    }
                }
            }
            alreadyNode.addAll(currentNode);
            HashSet<NodeId> temp = currentNode;
            currentNode = nextNode;
            nextNode = temp;
            nextNode.clear();
        }
        this.linksInSrcDst.putAll(currentLinksInSrcDst);
        return;
    }
    private List<Link> getFabricLinks(NodeId srcNodeId,NodeId dstNodeId,HashMap<SrcDstMap,List<Link>> currentLinksInSrcDst){
        Set<Entry<SrcDstMap, List<Link>>> entrySet = currentLinksInSrcDst.entrySet();
        if( entrySet != null){
            Iterator<Entry<SrcDstMap, List<Link>>> iEntrySet = entrySet.iterator();
            while(iEntrySet.hasNext()){
                Entry<SrcDstMap, List<Link>> temp = iEntrySet.next();
                if(temp.getKey().getDst().equals(dstNodeId) && temp.getKey().getSrc().equals(srcNodeId)){
                    return temp.getValue();
                }
            }
        }
        return null;
    }
    private boolean isAdd(HashSet<NodeId> alreadyNode,
            HashSet<NodeId> currentNode,
            HashSet<NodeId> nextNode,
            NodeId nodeId){
        return alreadyNode.contains(nodeId)||currentNode.contains(nodeId)||nextNode.contains(nodeId);
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
            NodeId dstNodeId = this.idMapNodeId.get(id);
            HashSet<NodeId> nodeIds = new HashSet<NodeId>(this.idMapNodeId.values());
            nodeIds.remove(dstNodeId);
            Iterator<NodeId> srcNodeIds = nodeIds.iterator();
            while(srcNodeIds.hasNext()){
                NodeId srcNodeId = srcNodeIds.next();
                List<Link> linkList = this.flowWriterService.addNodeToNodeFabricFlows(srcNodeId, dstNodeId, id);
                //List<Link> linkList = networkGraphService.getPath(srcNodeId, dstNodeId);
                if(linkList != null){
                    //this.flowWriterService.addNodeToNodeFabricFlows(linkList, id);
                    SrcDstMap s = new SrcDstMap(srcNodeId,dstNodeId,id);
                    this.linksInSrcDst.put(s, linkList);
                }
            }
        }
    }
    private class InstallFabricNew implements Runnable{
        private final Hashtable<Long,NodeId> idMapNodeId;
        private final long id;
        private final FlowWriterService flowWriterService;
        private final Hashtable<SrcDstMap,List<Link>> linksInSrcDst;
        private final HashMap<SrcDstMap,List<Link>> currentLinksInSrcDst;
        private final NetworkGraphService networkGraphService;
        public InstallFabricNew(long id,
                Hashtable<Long,NodeId> idMapNodeId,
                FlowWriterService flowWriterService,
                Hashtable<SrcDstMap,List<Link>> linksInSrcDst,
                NetworkGraphService networkGraphService){
            this.idMapNodeId = new Hashtable<Long,NodeId>(idMapNodeId);
            this.linksInSrcDst = linksInSrcDst;
            this.id = id;
            this.flowWriterService = flowWriterService;
            this.networkGraphService = networkGraphService;
            this.currentLinksInSrcDst = new HashMap<SrcDstMap,List<Link>>();
            return;
        }
        @Override
        public void run() {
            HashSet<NodeId> alreadyNode = new HashSet<NodeId>();
            HashSet<NodeId> currentNode = new HashSet<NodeId>();
            HashSet<NodeId> nextNode = new HashSet<NodeId>();
            NodeId dstNodeId = this.idMapNodeId.get(id);
            //add table3
            this.flowWriterService.addFabricLastFlow(dstNodeId, id);
            currentNode.add(dstNodeId);
            // while current node hasn't deal with
            // to find neighbours and add flows
            while(!currentNode.isEmpty()){
                Iterator<NodeId> iNodeId = currentNode.iterator();
                while(iNodeId.hasNext()){
                    NodeId currentId = iNodeId.next();
                    List<Link> lastLinkList = this.getFabricLinks(currentId, dstNodeId);
                    Collection<Link> neighbourLinks = this.networkGraphService.getLinksByNodeId(currentId);
                    if(neighbourLinks != null){
                        boolean addMiddleFlowFlag = false;
                        Iterator<Link> iLink = neighbourLinks.iterator();
                        while(iLink.hasNext()){
                            Link temp = iLink.next();
                            NodeId srcNodeId = temp.getSource().getSourceNode();
                            if(!isAdd(alreadyNode,currentNode,nextNode,srcNodeId)){
                                addMiddleFlowFlag = true;
                                // next node list added
                                nextNode.add(srcNodeId);
                                // add links
                                SrcDstMap s = new SrcDstMap(srcNodeId,dstNodeId,id);
                                List<Link> tempLink = new ArrayList<Link>();
                                tempLink.add(temp);
                                if(lastLinkList != null){
                                    tempLink.addAll(lastLinkList);
                                }
                                this.currentLinksInSrcDst.put(s, tempLink);
                                // add first flow
                                this.flowWriterService.addFabricFirstFlow(srcNodeId, id, temp.getSource().getSourceTp());
                            }
                        }
                        if(addMiddleFlowFlag && lastLinkList!= null){
                            this.flowWriterService.addFabricMiddleFlow(currentId, id, lastLinkList.get(0).getSource().getSourceTp());
                        }
                    }
                }
                alreadyNode.addAll(currentNode);
                HashSet<NodeId> temp = currentNode;
                currentNode = nextNode;
                nextNode = temp;
                nextNode.clear();
            }
            this.linksInSrcDst.putAll(this.currentLinksInSrcDst);
        }

        private List<Link> getFabricLinks(NodeId srcNodeId,NodeId dstNodeId){
            Set<Entry<SrcDstMap, List<Link>>> entrySet = this.currentLinksInSrcDst.entrySet();
            if( entrySet != null){
                Iterator<Entry<SrcDstMap, List<Link>>> iEntrySet = entrySet.iterator();
                while(iEntrySet.hasNext()){
                    Entry<SrcDstMap, List<Link>> temp = iEntrySet.next();
                    if(temp.getKey().getDst().equals(dstNodeId) && temp.getKey().getSrc().equals(srcNodeId)){
                        return temp.getValue();
                    }
                }
            }
            return null;
        }
        private boolean isAdd(HashSet<NodeId> alreadyNode,
                HashSet<NodeId> currentNode,
                HashSet<NodeId> nextNode,
                NodeId nodeId){
            return alreadyNode.contains(nodeId)||currentNode.contains(nodeId)||nextNode.contains(nodeId);
        }
    }
    /**
     * down load flows
     * @author
     *
     */
    private class InstallFabricFlow implements Runnable{
        private final NodeId id;
        private final FlowWriterService flowWriterService;
        public InstallFabricFlow(NodeId id,FlowWriterService flowWriterService){
            this.id = id;
            this.flowWriterService = flowWriterService;
            return;
        }

        @Override
        public void run() {
            long time = 0;
            if(coreNodeSet.contains(id.getValue())){
                time = 200;
            }
            this.flowWriterService.downloadFabricFlowsByNodeId(this.id,time);
        }
    }
}
