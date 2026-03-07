package com.dlzstudio.iems;

import com.dlzstudio.iems.blocks.IEMSBlocks;
import com.dlzstudio.iems.energy.EnergyGrid;
import com.dlzstudio.iems.web.WebServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(IEMSMod.MODID)
public class IEMSMod {
    public static final String MODID = "iems";
    public static final String MOD_NAME = "Integrated Energy Management System";
    public static final String MOD_TEAM = "等离子工作室 (DLZstudio)";
    public static final Logger LOGGER = LoggerFactory.getLogger(IEMSMod.class);

    public static final int WEB_SERVER_PORT = 8080;

    private static IEMSMod instance;

    public IEMSMod(IEventBus modEventBus) {
        instance = this;
        LOGGER.info("===== {} 初始化 =====", MOD_NAME);
        LOGGER.info("开发团队：{}", MOD_TEAM);
        LOGGER.info("版本：0.6.5");

        IEMSBlocks.register(modEventBus);

        // 注册服务器事件到 NeoForge.EVENT_BUS (不是 modEventBus)
        NeoForge.EVENT_BUS.addListener(this::serverStarting);
        NeoForge.EVENT_BUS.addListener(this::serverStopped);
    }

    private void serverStarting(ServerStartingEvent event) {
        LOGGER.info("服务器启动，启动 Web 服务器...");
        WebServer.start(WEB_SERVER_PORT);
    }

    private void serverStopped(ServerStoppedEvent event) {
        LOGGER.info("服务器停止，关闭 Web 服务器...");
        WebServer.stop();
        EnergyGrid.getInstance().clear();
    }

    public static IEMSMod getInstance() {
        return instance;
    }
}
