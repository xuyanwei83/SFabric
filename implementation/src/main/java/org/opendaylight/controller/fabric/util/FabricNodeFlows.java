package org.opendaylight.controller.fabric.util;

import java.util.HashSet;
import java.util.Hashtable;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;

public class FabricNodeFlows {
    private final NodeRef nr;
    private HashSet<Flow> switchInputFlows;
    private Flow hostInputFlow;
    private Flow arpMissMatchPushtagFlow;
    private Flow ipMissMatchPushtagFlow;
    private Hashtable<Long,Flow> fabricSwapFlows;
    private Hashtable<Long,Flow> fabricInputFlows;

    public FabricNodeFlows(NodeRef nr){
        this.nr = nr;
        this.switchInputFlows = new HashSet<Flow>();
        this.hostInputFlow = null;
        this.arpMissMatchPushtagFlow = null;
        this.ipMissMatchPushtagFlow = null;
        this.fabricSwapFlows = new Hashtable<Long,Flow>();
        this.fabricInputFlows = new Hashtable<Long,Flow>();
        return;
    }

    /**
     * initialize inner variables
     */
    public void init(){
        this.switchInputFlows.clear();
        this.fabricSwapFlows.clear();
        this.fabricInputFlows.clear();
        this.hostInputFlow = null;
        this.arpMissMatchPushtagFlow = null;
        this.ipMissMatchPushtagFlow = null;
        return;
    }

    /**
     * set the host input flow
     * @param flow
     */
    public boolean addHostInputFlow(Flow flow){
        if(this.hostInputFlow == null){
            this.hostInputFlow = flow;
            return true;
        }else{
            return false;
        }
    }

    /**
     * remove the host input flow
     * @return
     */
    public Flow removeHostInputFlow(){
        Flow ret = this.hostInputFlow;
        this.hostInputFlow = null;
        return ret;
    }
    /**
     * get the host input flow
     * @return
     */
    public Flow getHostInputFlow(){
        return this.hostInputFlow;
    }

    /**
     * set the ip miss match  flow
     * @param flow
     */
    public boolean addIPMissMatchPushtagFlow(Flow flow){
        if(this.ipMissMatchPushtagFlow == null){
            this.ipMissMatchPushtagFlow = flow;
            return true;
        }else{
            return false;
        }
    }

    /**
     * remove the ip miss match flow
     * @return
     */
    public Flow removeIPMissMatchPushtagFlow(){
        Flow ret = this.ipMissMatchPushtagFlow;
        this.ipMissMatchPushtagFlow = null;
        return ret;
    }
    /**
     * get the ip miss match flow
     * @return
     */
    public Flow getIPMissMatchPushtagFlow(){
        return this.ipMissMatchPushtagFlow;
    }
    /**
     * set the arp miss match  flow
     * @param flow
     */
    public boolean addARPMissMatchPushtagFlow(Flow flow){
        if(this.arpMissMatchPushtagFlow == null){
            this.arpMissMatchPushtagFlow = flow;
            return true;
        }else{
            return false;
        }
    }

    /**
     * remove the arp miss match flow
     * @return
     */
    public Flow removeARPMissMatchPushtagFlow(){
        Flow ret = this.arpMissMatchPushtagFlow;
        this.arpMissMatchPushtagFlow = null;
        return ret;
    }
    /**
     * get the arp miss match flow
     * @return
     */
    public Flow getARPMissMatchPushtagFlow(){
        return this.arpMissMatchPushtagFlow;
    }
    /**
     * add a switch input flow
     * @param flow
     */
    public boolean addSwitchInputFlow(Flow flow){
        if(this.switchInputFlows.contains(flow)){
            return false;
        }else{
            this.switchInputFlows.add(flow);
            return true;
        }
    }

    /**
     * remove a switch input flow
     * @param flow
     */
    public void removeSwitchInputFlow(Flow flow){
        this.switchInputFlows.remove(flow);
        return;
    }
    /**
     * remove switch input flows
     * @return HashSet<flow>
     */
    public HashSet<Flow> removeSwitchInputFlows(){
        HashSet<Flow> ret = new HashSet<Flow>(this.switchInputFlows);
        this.switchInputFlows.clear();
        return ret;
    }
    /**
     * get switch input flows
     * @return
     */
    public HashSet<Flow> getSwitchInputFlows(){
        return this.switchInputFlows;
    }
    /**
     * add a fabric swap flow
     * @param tag
     * @param flow
     */
    public boolean addFabricSwapFlow(Long tag, Flow flow){
        if(this.fabricSwapFlows.containsKey(tag)){
            return false;
        }else{
            this.fabricSwapFlows.put(tag, flow);
            return true;
        }
    }

    /**
     * get a fabric swap flow
     * @param tag
     * @return Flow
     */
    public Flow getFabricSwapFlowByTag(Long tag){
        return this.fabricSwapFlows.get(tag);
    }
    /**
     * remove a fabric swap flow
     * @param flow
     */
    public void removeFabricSwapFlow(Flow flow){
        this.fabricSwapFlows.remove(flow);
        return;
    }

    /**
     * remove a fabric swap flow by tag
     * @param tag
     * @return
     */
    public Flow removeFabricSwapFlowByTag(Long tag){
        return this.fabricSwapFlows.remove(tag);
    }
    /**
     * remove fabric swap flows
     * @return HashSet<flow>
     */
    public Hashtable<Long,Flow> removeFabricSwapFlows(){
        Hashtable<Long,Flow> ret = new Hashtable<Long,Flow>(this.fabricSwapFlows);
        this.fabricSwapFlows.clear();
        return ret;
    }
    /**
     * get fabric swap flows
     * @return
     */
    public Hashtable<Long,Flow> getFabricSwapFlows(){
        return this.fabricSwapFlows;
    }
    /**
     * add a fabric input flow
     * @param tag
     * @param flow
     */
    public boolean addFabricInputFlow(Long tag, Flow flow){
        if(this.fabricInputFlows.containsKey(tag)){
            return false;
        }else{
            this.fabricInputFlows.put(tag, flow);
            return true;
        }
    }

    /**
     * get a fabric input flow
     * @param tag
     * @return Flow
     */
    public Flow getFabricInputFlowByTag(Long tag){
        return this.fabricInputFlows.get(tag);
    }
    /**
     * remove a fabric input flow
     * @param flow
     */
    public void removeFabricInputFlow(Flow flow){
        this.fabricInputFlows.remove(flow);
        return;
    }

    /**
     * remove a fabric input flow by tag
     * @param tag
     * @return
     */
    public Flow removeFabricInputFlowByTag(Long tag){
        return this.fabricInputFlows.remove(tag);
    }
    /**
     * remove fabric input flows
     * @return HashSet<flow>
     */
    public Hashtable<Long,Flow> removeFabricInputFlows(){
        Hashtable<Long,Flow> ret = new Hashtable<Long,Flow>(this.fabricInputFlows);
        this.fabricInputFlows.clear();
        return ret;
    }
    /**
     * get fabric input flows
     * @return
     */
    public Hashtable<Long,Flow> getFabricInputFlows(){
        return this.fabricInputFlows;
    }

    /**
     * get the node id
     * @return
     */
    public NodeRef getNodeId(){
        return this.nr;
    }
}
