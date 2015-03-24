package org.opendaylight.controller.fabric.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;

public class FabricNodeFlowsUtils {
    private final Hashtable<NodeRef,FabricNodeFlows> htFabricNodeFlows;
    public FabricNodeFlowsUtils(){
        this.htFabricNodeFlows = new Hashtable<NodeRef,FabricNodeFlows>();
    }
    /**
     * get the fabricNodeFlows by nodeId
     * @param nodeId
     * @return
     */
    public synchronized FabricNodeFlows getFabricNodeFlowsByNodeId(NodeRef nr){
        FabricNodeFlows ret = this.htFabricNodeFlows.get(nr);
        if(ret == null){
            ret = new FabricNodeFlows(nr);
            this.htFabricNodeFlows.put(nr, ret);
        }
        return ret;
    }
    /**
     * set the fabricNodeFlows
     * @param f
     */
    public synchronized void setFabricNodeFlows(FabricNodeFlows f){
        this.htFabricNodeFlows.put(f.getNodeId(), f);
        return;
    }
    /**
     * remove the fabricNodeFlows
     * @param nodeId
     * @return
     */
    public synchronized FabricNodeFlows removeFabricNodeFlowsByNodeId(NodeRef nr){
        return this.htFabricNodeFlows.remove(nr);
    }

    /**
     * get the fabricNodeFlows set by tag
     * find the total fabricNodeFlows
     * @param tag
     * @return
     */
    public HashSet<FabricNodeFlows> getFabricPathFlowsByTag(Long tag){
        HashSet<FabricNodeFlows> ret = new HashSet<FabricNodeFlows>();
        Collection<FabricNodeFlows> cFabricNodeFlows = this.htFabricNodeFlows.values();
        if(cFabricNodeFlows != null && !cFabricNodeFlows.isEmpty()){
        Iterator<FabricNodeFlows> iFabricNodeFlows = cFabricNodeFlows.iterator();
            while(iFabricNodeFlows.hasNext()){
                FabricNodeFlows f = iFabricNodeFlows.next();
                if(f.getFabricSwapFlowByTag(tag) != null){
                    ret.add(f);
                }
            }
        }
        return ret;
    }
    public Hashtable<NodeRef,FabricNodeFlows> getAllFabricNodeFlows(){
        return this.htFabricNodeFlows;
    }
}