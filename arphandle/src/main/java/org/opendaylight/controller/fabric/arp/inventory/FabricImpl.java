package org.opendaylight.controller.fabric.arp.inventory;


public class FabricImpl {
    private HostImpl src;
    private HostImpl dst;
    public FabricImpl(HostImpl src,HostImpl dst){
        this.src = src;
        this.dst = dst;
        return;
    }
    public void setSrc(HostImpl src){
        this.src = src;
    }
    public void setDst(HostImpl dst){
        this.dst = dst;
    }
    public HostImpl getSrc(){
        return this.src;
    }
    public HostImpl getDst(){
        return this.dst;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((src == null) ? 0 : src.hashCode());
        result = prime * result + ((dst == null) ? 0 : dst.hashCode());
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
        FabricImpl other = (FabricImpl) obj;
        if(src == null ){
            if(other.src != null){
                return false;
            }
        }else if(!this.src.equals(other.src)){
            return false;
        }
        if(dst == null ){
            if(other.dst != null){
                return false;
            }
        }else if(!this.dst.equals(other.dst)){
            return false;
        }
        return true;
    }

}
