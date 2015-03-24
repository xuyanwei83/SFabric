package org.opendaylight.controller.fabric;

import org.opendaylight.controller.fabric.flow.FlowWriterService;
import org.opendaylight.controller.fabric.flow.FlowWriterServiceVlanMix;
import org.opendaylight.controller.fabric.topology.NetworkGraphService;
import org.opendaylight.controller.fabric.topology.NetworkGraphServiceImpl;
import org.opendaylight.controller.fabric.topology.TopologyLinkDataChangeHandler;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricProvider extends AbstractBindingAwareProvider implements AutoCloseable{

    private final Logger logger = LoggerFactory.getLogger(FabricProvider.class);
    private FabricService fabricService;
    private TopologyLinkDataChangeHandler topologyLinkDataChangeHandler;
    private BundleContext context;
    @Override
    public void onSessionInitiated(ProviderContext session) {
        // TODO Auto-generated method stub
        // get the md service from provider session
        DataBroker dataService = session.getSALService(DataBroker.class);
        SalFlowService flowService = session.getRpcService(SalFlowService.class);

        // create the network graph service to calculate the path
        NetworkGraphService networkGraphService = new NetworkGraphServiceImpl();

        // create the flow service to write the flow into the switch
        FlowWriterService flowWriterService = new FlowWriterServiceVlanMix(dataService,networkGraphService,flowService);

        // create the fabric service to setup, delete or update the fabric path
        //this.fabricService = new FabricServiceImplLastNodeTag(dataService,flowWriterService,networkGraphService);
        this.fabricService = new FabricServiceImplLastNodeTagByOutterTopo(dataService,flowWriterService,networkGraphService);

        // regist the topology listener, while the topology changed, fresh the data of the network graph
        //this.topologyLinkDataChangeHandler = new TopologyLinkDataChangeHandler(dataService,networkGraphService,fabricService);
        //this.topologyLinkDataChangeHandler.registerAsDataChangeListener();

        // regist the fabric service into the BundleContext, provide for the web
        this.context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        this.context.registerService(FabricService.class, this.fabricService,null);

        this.logger.info("FabricProvider onSessionInitialized");
    }

    /**
     *
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        if(this.topologyLinkDataChangeHandler != null){
            this.topologyLinkDataChangeHandler.close();
        }
        return;
    }

    @Override
    protected void stopImpl(BundleContext context) {
        // NOOP
        this.fabricService.deleteFabric();
        this.logger.info("FabricProvider stopImpl!");
    }
}
