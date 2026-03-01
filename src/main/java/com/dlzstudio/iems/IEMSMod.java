package com.dlzstudio.iems;

import com.mojang.logging.LogUtils;
import com.dlzstudio.iems.blocks.IEMSBlocks;
import com.dlzstudio.iems.energy.EnergyGrid;
import com.dlzstudio.iems.entities.IEMSEntities;
import com.dlzstudio.iems.network.IEMSNetwork;
import com.dlzstudio.iems.web.WebServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

@Mod(IEMSMod.MODID)
public class IEMSMod {
    public static final String MODID = "IEMS";
    public static final String MOD_NAME = "Integrated Energy Management System";
    public static final String MOD_TEAM = "等离子工作室 (DLZstudio)";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Web 服务器默认端口
    public static final int WEB_SERVER_PORT = 8080;

    private static IEMSMod instance;

    public IEMSMod(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;
        LOGGER.info("===== {} 初始化 =====", MOD_NAME);
        LOGGER.info("开发团队：{}", MOD_TEAM);
        LOGGER.info("版本：0.4.0");

        IEMSBlocks.register(modEventBus);
        IEMSEntities.register(modEventBus);
        IEMSNetwork.register();

        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        
        // 注册服务器事件
        modEventBus.addListener(this::serverStarting);
        modEventBus.addListener(this::serverStopped);

        LOGGER.info("===== {} 初始化完成 =====", MOD_NAME);
        LOGGER.info("Web 界面将在服务器启动后可访问：http://localhost:{}", WEB_SERVER_PORT);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("通用设置完成");
    }
    
    private void serverStarting(final ServerStartingEvent event) {
        LOGGER.info("服务器启动中...");
        
        // 启动 Web 服务器
        EnergyGrid.getInstance().setWebServerPort(WEB_SERVER_PORT);
        WebServer.getInstance().start(WEB_SERVER_PORT);
        
        LOGGER.info("Web 界面已启用：http://localhost:{}", WEB_SERVER_PORT);
    }
    
    private void serverStopped(final ServerStoppedEvent event) {
        LOGGER.info("服务器停止中...");
        
        // 停止 Web 服务器
        WebServer.getInstance().stop();
        
        // 清空电网数据
        EnergyGrid.getInstance().clear();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        EnergyGrid.getInstance().onServerTick(event.getServer());
    }

    public static IEMSMod getInstance() {
        return instance;
    }
    
    public static Logger getLogger() {
        return LOGGER;
    }
}
