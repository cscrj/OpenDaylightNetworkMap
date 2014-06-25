package org.opendaylight.controller.networkMap;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.core.IContainer;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.topology.IListenTopoUpdates;
//import org.opendaylight.controller.protocol_plugin.openflow.ITopologyServiceShimListener;
//import org.opendaylight.controller.protocol_plugin.openflow.core.ISwitchStateListener;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends ComponentActivatorAbstractBase {

    private static final Logger log = LoggerFactory.getLogger(NetworkMap.class);

    @Override
    public Object[] getImplementations() {
        log.trace("Getting Implementations");

        Object[] res = { NetworkMap.class };
        return res;
    }

    @Override
    public void configureInstance(Component c, Object imp, String containerName) {
        log.trace("Configuring NetworkMap instance");

        if (imp.equals(NetworkMap.class)) {

            // set name property for when class is registered in SAL
            Dictionary<String, Object> inventoryServiceProperties = new Hashtable<String, Object>();
            inventoryServiceProperties.put("salListenerName", "networkMapper");

            /* List the interfaces we're going to implement */
            c.setInterface(
                    new String[] { IInventoryListener.class.getName(),
                            IListenDataPacket.class.getName(),
                            IListenTopoUpdates.class.getName() },
                    inventoryServiceProperties);

            /*
             * Below is from example to export callback methods. We have no
             * callbacks, as we're only listening, (for now) so do we don't need
             * this
             */

            c.add(createContainerServiceDependency(containerName)
                    .setService(IDataPacketService.class)
                    .setCallbacks("setDataPacketService",
                            "unsetDataPacketService").setRequired(true));

            c.add(createContainerServiceDependency(containerName)
                    .setService(IContainer.class)
                    .setCallbacks("setIContainer", "unsetIContainer")
                    .setRequired(true));

        }

    }
}