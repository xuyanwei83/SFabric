package org.opendaylight.controller.fabric.arp.inventory;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;

public class HostImpl {
    private IpAddress ip;
    private MacAddress mac;
    private NodeConnectorRef ncr;
    public HostImpl(IpAddress ip,MacAddress mac,NodeConnectorRef ncr){
        this.ip = ip;
        this.mac = mac;
        this.ncr = ncr;
        return;
    }
    public void setIp(IpAddress ip){
        this.ip = ip;
    }
    public void setMac(MacAddress mac){
        this.mac = mac;
    }
    public void setNodeConnector(NodeConnectorRef ncr){
        this.ncr = ncr;
    }
    public IpAddress getIp(){
        return this.ip;
    }
    public MacAddress getMac(){
        return this.mac;
    }
    public NodeConnectorRef getNodeConnector(){
        return this.ncr;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ncr == null) ? 0 : ncr.hashCode());
        result = prime * result + ((ip == null) ? 0 : ip.hashCode());
        result = prime * result + ((mac == null) ? 0 : mac.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HostImpl other = (HostImpl) obj;
        if(ip == null ){
            if(other.ip != null){
                return false;
            }
        }else if(!this.ip.equals(other.ip)){
            return false;
        }
        if(mac == null ){
            if(other.mac != null){
                return false;
            }
        }else if(!this.mac.equals(other.mac)){
            return false;
        }
        if(ncr == null ){
            if(other.ncr != null){
                return false;
            }
        }else if(!this.ncr.equals(other.ncr)){
            return false;
        }
        return true;
    }

}
