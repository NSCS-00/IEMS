package com.dlzstudio.iems.web;

import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.energy.EnergyGrid;
import com.dlzstudio.iems.energy.EnergyValue;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Web 服务器
 * 提供 Web 监控界面和 API
 * 
 * API 端点:
 * - GET / - HTML 页面
 * - GET /api/energy - 能量数据
 * - GET /api/power - 功率数据
 * - GET /api/protocol - 协议容量
 * - GET /api/devices - 设备列表
 * - POST /api/devices/toggle - 切换设备供能
 * - GET /api/config - 配置信息
 */
public class WebServer {
    
    private static HttpServer server;
    private static ExecutorService executor;
    private static boolean running = false;
    
    private static final String THEME = "black-blue";
    private static final String STATUS = "正常";
    
    /**
     * 启动 Web 服务器
     */
    public static void start(int port) {
        if (running) {
            IEMSMod.LOGGER.warn("Web 服务器已在运行");
            return;
        }
        
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            executor = Executors.newFixedThreadPool(4);
            server.setExecutor(executor);
            
            // 注册处理器
            server.createContext("/", new RootHandler());
            server.createContext("/api/energy", new EnergyHandler());
            server.createContext("/api/power", new PowerHandler());
            server.createContext("/api/protocol", new ProtocolHandler());
            server.createContext("/api/devices", new DevicesHandler());
            server.createContext("/api/config", new ConfigHandler());
            
            server.start();
            running = true;
            
            IEMSMod.LOGGER.info("Web 服务器已启动：http://localhost:{}", port);
            IEMSMod.LOGGER.info("访问 http://localhost:{}/ 查看监控界面", port);
        } catch (IOException e) {
            IEMSMod.LOGGER.error("Web 服务器启动失败：{}", e.getMessage());
        }
    }
    
    /**
     * 停止 Web 服务器
     */
    public static void stop() {
        if (!running) return;
        
        if (server != null) {
            server.stop(5);
            server = null;
        }
        
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        
        running = false;
        IEMSMod.LOGGER.info("Web 服务器已停止");
    }
    
    public static boolean isRunning() {
        return running;
    }
    
    // ============ 处理器类 ============
    
    /**
     * 根路径处理器 - 返回 HTML 页面
     */
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    // 从 JAR 内部读取 HTML 文件
                    var inputStream = WebServer.class.getClassLoader().getResourceAsStream("web/zcs-terminal.html");
                    if (inputStream != null) {
                        byte[] htmlBytes = inputStream.readAllBytes();
                        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                        exchange.sendResponseHeaders(200, htmlBytes.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(htmlBytes);
                        }
                        inputStream.close();
                    } else {
                        // HTML 文件不存在时返回简单页面
                        String response = "<html><body><h1>IEMS 监控界面</h1><p>HTML 文件未找到</p></body></html>";
                        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(bytes);
                        }
                    }
                } catch (IOException e) {
                    IEMSMod.LOGGER.error("读取 HTML 文件失败：{}", e.getMessage());
                    exchange.sendResponseHeaders(500, -1);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
    
    /**
     * 能量数据处理器
     */
    static class EnergyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    // 从 EnergyGrid 获取能量数据
                    EnergyValue energy = EnergyGrid.getInstance().getCoreEnergy(null);
                    EnergyValue capacity = new EnergyValue(energy.getValueInFE().multiply(java.math.BigInteger.valueOf(2)), EnergyValue.EnergyUnit.FE);
                    
                    String current = energy.getSE().toString();
                    String total = capacity.getSE().toString();
                    
                    String response = String.format("{\"current\":\"%s\",\"total\":\"%s\"}", current, total);
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    
                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } catch (Exception e) {
                    IEMSMod.LOGGER.error("获取能量数据失败：{}", e.getMessage());
                    String error = "{\"error\":\"获取能量数据失败\"}";
                    byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(500, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
    
    /**
     * 功率数据处理器
     */
    static class PowerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    // 简化实现：返回固定值
                    String input = "1000.000000";
                    String output = "500.000000";
                    
                    String response = String.format("{\"input\":\"%s\",\"output\":\"%s\"}", input, output);
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    
                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } catch (Exception e) {
                    IEMSMod.LOGGER.error("获取功率数据失败：{}", e.getMessage());
                    String error = "{\"error\":\"获取功率数据失败\"}";
                    byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(500, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
    
    /**
     * 协议容量处理器
     */
    static class ProtocolHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    // 简化实现：返回固定值
                    String used = "0";
                    String total = "100";
                    
                    String response = String.format("{\"used\":\"%s\",\"total\":\"%s\"}", used, total);
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    
                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } catch (Exception e) {
                    IEMSMod.LOGGER.error("获取协议数据失败：{}", e.getMessage());
                    String error = "{\"error\":\"获取协议数据失败\"}";
                    byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(500, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
    
    /**
     * 设备列表处理器
     */
    static class DevicesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    // 从 EnergyGrid 获取设备列表
                    var devices = EnergyGrid.getInstance().getEnabledDevices();
                    
                    StringBuilder json = new StringBuilder("[");
                    int i = 0;
                    for (var entry : devices.entrySet()) {
                        if (i > 0) json.append(",");
                        json.append(String.format("{\"id\":%d,\"name\":\"%s\",\"enabled\":%s}",
                            i, entry.getKey(), entry.getValue() ? "true" : "false"));
                        i++;
                    }
                    json.append("]");
                    
                    byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
                    
                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } catch (Exception e) {
                    IEMSMod.LOGGER.error("获取设备列表失败：{}", e.getMessage());
                    String error = "[]";
                    byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                }
            } else if ("POST".equals(exchange.getRequestMethod())) {
                // 处理切换设备请求
                try {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    IEMSMod.LOGGER.info("切换设备请求：{}", body);
                    
                    String response = "{\"success\":true}";
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    
                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } catch (Exception e) {
                    IEMSMod.LOGGER.error("切换设备失败：{}", e.getMessage());
                    exchange.sendResponseHeaders(500, -1);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
    
    /**
     * 配置处理器
     */
    static class ConfigHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String response = String.format("{\"theme\":\"%s\",\"status\":\"%s\"}", THEME, STATUS);
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    
                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } catch (Exception e) {
                    IEMSMod.LOGGER.error("获取配置失败：{}", e.getMessage());
                    exchange.sendResponseHeaders(500, -1);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
}
