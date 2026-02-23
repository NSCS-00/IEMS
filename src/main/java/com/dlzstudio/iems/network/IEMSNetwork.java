package com.dlzstudio.iems.network;

import com.dlzstudio.iems.IEMSMod;
import net.neoforged.neoforge.network.registration.PayloadRegistry;

/**
 * 网络包注�? */
public class IEMSNetwork {
    
    public static void register() {
        PayloadRegistry.init();
        IEMSMod.LOGGER.info("注册 IEMS 网络�?);
    }
}
