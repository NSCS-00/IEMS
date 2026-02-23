package com.dlzstudio.iems.web;

import com.dlzstudio.iems.energy.EnergyGrid;
import com.dlzstudio.iems.energy.EnergyValue;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Web 服务器 - 提供电网信息网页界面
 */
public class WebServer {
    private static WebServer instance;
    private HttpServer server;
    private int port;
    private boolean running = false;
    
    private WebServer() {}
    
    public static WebServer getInstance() {
        if (instance == null) {
            instance = new WebServer();
        }
        return instance;
    }
    
    /**
     * 启动 Web 服务器
     */
    public void start(int port) {
        if (running) {
            stop();
        }
        
        this.port = port;
        
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // 主页面
            server.createContext("/", new RootHandler());
            
            // API 端点
            server.createContext("/api/status", new ApiStatusHandler());
            
            // 静态资源
            server.createContext("/css", new CssHandler());
            server.createContext("/js", new JsHandler());
            
            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();
            running = true;
            
            System.out.println("[IEMS Web] 服务器已启动在 http://localhost:" + port);
            
        } catch (IOException e) {
            System.err.println("[IEMS Web] 启动失败：" + e.getMessage());
        }
    }
    
    /**
     * 停止 Web 服务器
     */
    public void stop() {
        if (server != null && running) {
            server.stop(0);
            running = false;
            System.out.println("[IEMS Web] 服务器已停止");
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public int getPort() {
        return port;
    }
    
    /**
     * 主页面处理器
     */
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = generateHtml();
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
        
        private String generateHtml() {
            EnergyGrid grid = EnergyGrid.getInstance();
            
            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html>\n");
            sb.append("<html lang=\"zh-CN\">\n");
            sb.append("<head>\n");
            sb.append("    <meta charset=\"UTF-8\">\n");
            sb.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            sb.append("    <title>IEMS - 综合能源管理系统</title>\n");
            sb.append("    <style>\n");
            sb.append(generateCss());
            sb.append("    </style>\n");
            sb.append("</head>\n");
            sb.append("<body>\n");
            sb.append("    <div class=\"container\">\n");
            sb.append("        <h1>⚡ 综合能源管理系统</h1>\n");
            sb.append("        <div class=\"status-card ").append(grid.isDepleted() ? "depleted" : "normal").append("\">\n");
            sb.append("            <h2>电网状态</h2>\n");
            sb.append("            <div class=\"status-indicator ").append(grid.isDepleted() ? "indicator-red" : "indicator-green").append("\"></div>\n");
            sb.append("            <p class=\"status-text\">").append(grid.isDepleted() ? "⚠ 电网已耗尽" : "✓ 电网正常运行").append("</p>\n");
            sb.append("        </div>\n");
            sb.append("        \n");
            sb.append("        <div class=\"info-grid\">\n");
            sb.append("            <div class=\"info-card\">\n");
            sb.append("                <h3>🔋 当前电量</h3>\n");
            sb.append("                <p class=\"value\">").append(grid.getEnergyDisplay()).append("</p>\n");
            sb.append("            </div>\n");
            sb.append("            \n");
            sb.append("            <div class=\"info-card\">\n");
            sb.append("                <h3>📊 能量消耗</h3>\n");
            sb.append("                <p class=\"value consumption\">").append(grid.getConsumption().toString()).append("</p>\n");
            sb.append("            </div>\n");
            sb.append("            \n");
            sb.append("            <div class=\"info-card\">\n");
            sb.append("                <h3>⚡ 能量产出</h3>\n");
            sb.append("                <p class=\"value generation\">").append(grid.getGeneration().toString()).append("</p>\n");
            sb.append("            </div>\n");
            sb.append("            \n");
            sb.append("            <div class=\"info-card\">\n");
            sb.append("                <h3>⏱️ 充满时间</h3>\n");
            sb.append("                <p class=\"value\">").append(grid.getTimeToFull()).append("</p>\n");
            sb.append("            </div>\n");
            sb.append("        </div>\n");
            sb.append("        \n");
            sb.append("        <div class=\"network-status\">\n");
            sb.append("            <h3>🌐 网络连接</h3>\n");
            sb.append("            <p>核心位置：").append(grid.getActiveCorePos() != null ? 
                grid.getActiveCorePos().getX() + ", " + grid.getActiveCorePos().getY() + ", " + grid.getActiveCorePos().getZ() : 
                "未找到").append("</p>\n");
            sb.append("            <p>连接设备数：").append(grid.getConnectedDevices().size()).append("</p>\n");
            sb.append("            <p>存储器数量：").append("N/A").append("</p>\n");
            sb.append("        </div>\n");
            sb.append("        \n");
            sb.append("        <footer>\n");
            sb.append("            <p>IEMS v0.4.0 | 等离子工作室 (DLZstudio)</p>\n");
            sb.append("            <p>数据每 5 秒自动刷新</p>\n");
            sb.append("        </footer>\n");
            sb.append("    </div>\n");
            sb.append("    <script>\n");
            sb.append(generateJs());
            sb.append("    </script>\n");
            sb.append("</body>\n");
            sb.append("</html>");
            
            return sb.toString();
        }
        
        private String generateCss() {
            return "* { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                   "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); min-height: 100vh; color: #fff; }\n" +
                   ".container { max-width: 1200px; margin: 0 auto; padding: 40px 20px; }\n" +
                   "h1 { text-align: center; margin-bottom: 40px; font-size: 2.5em; text-shadow: 0 0 20px rgba(0, 200, 255, 0.5); }\n" +
                   ".status-card { background: rgba(255,255,255,0.1); border-radius: 15px; padding: 30px; margin-bottom: 30px; backdrop-filter: blur(10px); border: 1px solid rgba(255,255,255,0.2); }\n" +
                   ".status-card h2 { display: inline-block; margin-right: 20px; }\n" +
                   ".status-indicator { display: inline-block; width: 15px; height: 15px; border-radius: 50%; margin-right: 15px; }\n" +
                   ".indicator-green { background: #00ff88; box-shadow: 0 0 10px #00ff88; animation: pulse 2s infinite; }\n" +
                   ".indicator-red { background: #ff4444; box-shadow: 0 0 10px #ff4444; animation: pulse 1s infinite; }\n" +
                   ".status-text { display: inline-block; font-size: 1.2em; }\n" +
                   ".info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-bottom: 30px; }\n" +
                   ".info-card { background: rgba(255,255,255,0.1); border-radius: 15px; padding: 25px; backdrop-filter: blur(10px); border: 1px solid rgba(255,255,255,0.2); }\n" +
                   ".info-card h3 { margin-bottom: 15px; font-size: 1.1em; opacity: 0.8; }\n" +
                   ".info-card .value { font-size: 1.8em; font-weight: bold; }\n" +
                   ".consumption { color: #ff6b6b; }\n" +
                   ".generation { color: #51cf66; }\n" +
                   ".network-status { background: rgba(255,255,255,0.1); border-radius: 15px; padding: 30px; backdrop-filter: blur(10px); border: 1px solid rgba(255,255,255,0.2); }\n" +
                   ".network-status h3 { margin-bottom: 20px; }\n" +
                   ".network-status p { margin: 10px 0; opacity: 0.8; }\n" +
                   "footer { text-align: center; margin-top: 40px; opacity: 0.6; }\n" +
                   "@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }\n" +
                   "@media (max-width: 768px) { .info-grid { grid-template-columns: 1fr; } }";
        }
        
        private String generateJs() {
            return "setInterval(function() { location.reload(); }, 5000);";
        }
    }
    
    /**
     * API 状态处理器
     */
    static class ApiStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            EnergyGrid grid = EnergyGrid.getInstance();
            
            String json = String.format(
                "{\"energy\":\"%s\",\"consumption\":\"%s\",\"generation\":\"%s\",\"timeToFull\":\"%s\",\"depleted\":%s}",
                grid.getEnergyDisplay(),
                grid.getConsumption().toString(),
                grid.getGeneration().toString(),
                grid.getTimeToFull(),
                grid.isDepleted()
            );
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
    
    static class CssHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/css");
            exchange.sendResponseHeaders(200, -1);
            exchange.getResponseBody().close();
        }
    }
    
    static class JsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/javascript");
            exchange.sendResponseHeaders(200, -1);
            exchange.getResponseBody().close();
        }
    }
}
