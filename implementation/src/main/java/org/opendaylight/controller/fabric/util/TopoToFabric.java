package org.opendaylight.controller.fabric.util;


public class TopoToFabric {
//    private NodePortInfoService nodePortInfoService;
//    private List<Link> links;
//    private List<Node> nodes;
//    public TopoToFabric(){
//        links = new ArrayList<Link>();
//        nodes = new ArrayList<Node>();
//    }
//    private NodePortInfoService getNodePortInfoService(){
//        if(this.nodePortInfoService == null){
//            try{
//                BundleContext bCtx = FrameworkUtil.getBundle(NodePortInfoProvider.class).getBundleContext();
//                ServiceReference<NodePortInfoService> srf = bCtx.getServiceReference(NodePortInfoService.class);
//                this.nodePortInfoService = (NodePortInfoService)bCtx.getService(srf);
//            }catch(Exception ex){
//            }
//        }
//        return this.nodePortInfoService;
//    }
//    public synchronized List<Link> getLinks(){
//        this.getNodePortInfoService();
//        Set<NodePortInfo> tSet = this.nodePortInfoService.getNodePortInfo();
//        if(tSet.isEmpty()){
//            return null;
//        }
//        if(tSet.size() == this.links.size()){
//            return this.links;
//        }
//        Iterator<NodePortInfo> iSet = tSet.iterator();
//        this.links.clear();
//        // Links
//        LinkBuilder lb = new LinkBuilder();
//        SourceBuilder srb = new SourceBuilder();
//        DestinationBuilder dsb = new DestinationBuilder();
//        while(iSet.hasNext()){
//            NodePortInfo t = iSet.next();
//            srb.setSourceNode(new NodeId(t.getSrcNodeId()))
//                .setSourceTp(new TpId(t.getSrcNodePort()));
//            dsb.setDestNode(new NodeId(t.getDstNodeId()))
//                .setDestTp(new TpId(t.getDstNodePort()));
//
//            lb.setSource(srb.build())
//                .setDestination(dsb.build());
//            links.add(lb.build());
//        }
//        return this.links;
//    }
//    public synchronized List<Node> getNodes(){
//        this.getNodePortInfoService();
//        List<String> tList = this.nodePortInfoService.getAllNodes();
//        if(tList.isEmpty()){
//            return null;
//        }
//        if(tList.size() == this.nodes.size()){
//            return this.nodes;
//        }
//        Iterator<String> iList = tList.iterator();
//        this.nodes.clear();
//        // Nodes
//        NodeBuilder nb = new NodeBuilder();
//        while(iList.hasNext()){
//            this.nodes.add(nb.setNodeId(new NodeId(iList.next())).build());
//        }
//
//        return this.nodes;
//    }
}
