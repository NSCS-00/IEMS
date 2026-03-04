# ZCS 终端 - 外部数据接口文档

## 概述

本文档描述了 ZCS 终端 HTML 页面与 Java 后端之间的数据交互接口。所有数据均由外部（Java 后端）提供，前端页面通过 JavaScript API 接收并展示数据。

---

## 数据模型

### 1. 能量数据 (EnergyData)

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| currentEnergy | String | 当前能量值 (SE)，高精度字符串 | "1270.000000" |
| totalCapacity | String | 总能量容量 (SE)，高精度字符串 | "1500.000000" |

### 2. 功率数据 (PowerData)

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| inputPower | String | 输入功率 (SE/s)，高精度字符串 | "25400.000000" |
| outputPower | String | 输出功率 (SE/s)，高精度字符串 | "18700.000000" |

### 3. 协议容量数据 (ProtocolData)

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| used | String | 已使用协议容量 | "75" |
| total | String | 总协议容量 | "100" |

### 4. 设备数据 (Device)

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| id | int | 设备唯一标识 | 1 |
| name | String | 设备名称 | "量子熔炉" |
| enabled | boolean | 供能开关状态 | true |

---

## JavaScript API

### `window.ZCSTerminal` 对象

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `updateEnergy(current, total)` | current: String, total: String | void | 更新能量数据 |
| `updatePower(input, output)` | input: String, output: String | void | 更新功率数据 |
| `updateProtocol(used, total)` | used: String, total: String | void | 更新协议容量 |
| `updateDevices(devices)` | devices: Device[] | void | 更新设备列表 |
| `setTheme(theme)` | theme: String | void | 设置配色方案 |
| `setStatus(status)` | status: String | void | 设置状态文本 |
| `getState()` | void | Object | 获取当前状态 |
| `toggleDevice(deviceId)` | deviceId: int | void | 切换设备开关 |
| `onDeviceToggle(callback)` | callback: Function | void | 设置设备开关回调 |

### 配色方案参数

| 值 | 说明 |
|------|------|
| `black-blue` | 黑蓝配色 |
| `black-gold` | 黑金配色 |
| `white-blue` | 白蓝配色 |
| `white-gold` | 白金配色 |

---

## 使用示例

### 基础数据更新

```javascript
// 更新能量数据（字符串高精度）
window.ZCSTerminal.updateEnergy("1270.000000", "1500.000000");

// 更新功率数据（字符串高精度）
window.ZCSTerminal.updatePower("25400.000000", "18700.000000");

// 更新协议容量（字符串）
window.ZCSTerminal.updateProtocol("75", "100");

// 更新设备列表
window.ZCSTerminal.updateDevices([
    { id: 1, name: '量子熔炉', enabled: true },
    { id: 2, name: '空间折叠器', enabled: true },
    { id: 3, name: '护盾发生器', enabled: false },
    { id: 4, name: '防御阵列', enabled: true }
]);

// 设置配色方案
window.ZCSTerminal.setTheme("black-blue");

// 设置状态文本
window.ZCSTerminal.setStatus("正常");

// 获取当前状态
const state = window.ZCSTerminal.getState();
console.log(state);

// 切换设备开关
window.ZCSTerminal.toggleDevice(3);

// 设置设备开关回调
window.ZCSTerminal.onDeviceToggle((deviceId, enabled) => {
    console.log(`设备 ${deviceId} 已${enabled ? '开启' : '关闭'}`);
    // 在这里调用后端 API
});
```

---

## Java 后端集成示例

### 方式一：通过 WebView 注入 (推荐)

```java
import javafx.webengine.WebEngine;
import java.util.List;

public class ZCSTerminalController {
    private WebEngine webEngine;
    
    // 初始化配色
    public void initTheme() {
        webEngine.executeScript("window.ZCSTerminal.setTheme('black-blue')");
    }
    
    // 更新能量数据（字符串高精度）
    public void updateEnergy(String current, String total) {
        String js = String.format("window.ZCSTerminal.updateEnergy(\"%s\", \"%s\")", current, total);
        webEngine.executeScript(js);
    }
    
    // 更新功率数据（字符串高精度）
    public void updatePower(String input, String output) {
        String js = String.format("window.ZCSTerminal.updatePower(\"%s\", \"%s\")", input, output);
        webEngine.executeScript(js);
    }
    
    // 更新协议容量（字符串）
    public void updateProtocol(String used, String total) {
        String js = String.format("window.ZCSTerminal.updateProtocol(\"%s\", \"%s\")", used, total);
        webEngine.executeScript(js);
    }
    
    // 更新设备列表
    public void updateDevices(List<Device> devices) {
        StringBuilder js = new StringBuilder("window.ZCSTerminal.updateDevices([");
        for (int i = 0; i < devices.size(); i++) {
            Device d = devices.get(i);
            js.append(String.format(
                "{id: %d, name: '%s', enabled: %s}",
                d.getId(), d.getName(), d.isEnabled() ? "true" : "false"
            ));
            if (i < devices.size() - 1) {
                js.append(",");
            }
        }
        js.append("])");
        webEngine.executeScript(js.toString());
    }
    
    // 更新状态
    public void updateStatus(String status) {
        String js = String.format("window.ZCSTerminal.setStatus(\"%s\")", status);
        webEngine.executeScript(js);
    }
}
```

### 方式二：通过 HTTP API + JavaScript 轮询

前端轮询代码（需在 HTML 中添加）：

```javascript
const API_BASE_URL = 'http://localhost:8080/api/zcs';

async function fetchData() {
    try {
        const energyRes = await fetch(`${API_BASE_URL}/energy`);
        const energy = await energyRes.json();
        window.ZCSTerminal.updateEnergy(energy.current, energy.total);
        
        const powerRes = await fetch(`${API_BASE_URL}/power`);
        const power = await powerRes.json();
        window.ZCSTerminal.updatePower(power.input, power.output);
        
        const protocolRes = await fetch(`${API_BASE_URL}/protocol`);
        const protocol = await protocolRes.json();
        window.ZCSTerminal.updateProtocol(protocol.used, protocol.total);
        
        const devicesRes = await fetch(`${API_BASE_URL}/devices`);
        const devices = await devicesRes.json();
        window.ZCSTerminal.updateDevices(devices);
        
        const configRes = await fetch(`${API_BASE_URL}/config`);
        const config = await configRes.json();
        window.ZCSTerminal.setTheme(config.theme);
        window.ZCSTerminal.setStatus(config.status);
    } catch (error) {
        console.error('数据获取失败:', error);
    }
}

// 每 1 秒轮询一次
setInterval(fetchData, 1000);

// 设备开关回调
window.ZCSTerminal.onDeviceToggle((deviceId, enabled) => {
    fetch(`${API_BASE_URL}/devices/${deviceId}/toggle`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({enabled: enabled})
    });
});
```

Java REST API 示例：

```java
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/zcs")
@CrossOrigin(origins = "*")
public class ZCSTerminalController {
    
    @Autowired
    private EnergyService energyService;
    
    @GetMapping("/energy")
    public EnergyData getEnergy() {
        return energyService.getCurrentEnergy();
    }
    
    @GetMapping("/power")
    public PowerData getPower() {
        return energyService.getCurrentPower();
    }
    
    @GetMapping("/protocol")
    public ProtocolData getProtocol() {
        return energyService.getProtocolData();
    }
    
    @GetMapping("/devices")
    public List<Device> getDevices() {
        return energyService.getAllDevices();
    }
    
    @PostMapping("/devices/{id}/toggle")
    public Device toggleDevice(@PathVariable int id, @RequestBody ToggleRequest req) {
        return energyService.setDeviceState(id, req.isEnabled());
    }
    
    @GetMapping("/config")
    public Config getConfig() {
        return energyService.getConfig();
    }
}

// 数据传输对象
class EnergyData {
    private String current;
    private String total;
}

class PowerData {
    private String input;
    private String output;
}

class ProtocolData {
    private String used;
    private String total;
}

class Device {
    private int id;
    private String name;
    private boolean enabled;
}

class Config {
    private String theme = "black-blue";
    private String status = "正常";
}
```

### 方式三：通过 WebSocket 实时推送

```javascript
const ws = new WebSocket('ws://localhost:8080/ws/zcs');

ws.onmessage = function(event) {
    const data = JSON.parse(event.data);
    
    switch (data.type) {
        case 'energy':
            window.ZCSTerminal.updateEnergy(data.current, data.total);
            break;
        case 'power':
            window.ZCSTerminal.updatePower(data.input, data.output);
            break;
        case 'protocol':
            window.ZCSTerminal.updateProtocol(data.used, data.total);
            break;
        case 'devices':
            window.ZCSTerminal.updateDevices(data.devices);
            break;
        case 'theme':
            window.ZCSTerminal.setTheme(data.theme);
            break;
        case 'status':
            window.ZCSTerminal.setStatus(data.status);
            break;
    }
};

ws.onopen = function() {
    console.log('WebSocket 连接已建立');
};
```

---

## JSON 响应示例

```json
// GET /api/zcs/energy
{
    "current": "1270.000000",
    "total": "1500.000000"
}

// GET /api/zcs/power
{
    "input": "25400.000000",
    "output": "18700.000000"
}

// GET /api/zcs/protocol
{
    "used": "75",
    "total": "100"
}

// GET /api/zcs/devices
[
    {"id": 1, "name": "量子熔炉", "enabled": true},
    {"id": 2, "name": "空间折叠器", "enabled": true},
    {"id": 3, "name": "护盾发生器", "enabled": false}
]

// GET /api/zcs/config
{
    "theme": "black-blue",
    "status": "正常"
}

// WebSocket 消息示例
{"type": "energy", "current": "1270.000000", "total": "1500.000000"}
{"type": "power", "input": "25400.000000", "output": "18700.000000"}
{"type": "protocol", "used": "75", "total": "100"}
{"type": "devices", "devices": [...]}
{"type": "theme", "theme": "black-gold"}
{"type": "status", "status": "警告"}
```

---

## 文件清单

| 文件名 | 说明 |
|--------|------|
| `zcs-terminal.html` | 主页面（含四套配色） |
| `API_DOCUMENTATION.md` | 本接口文档 |

---

## 注意事项

1. **数据精度**：所有数值数据均使用字符串传递，以保持高精度
2. **配色控制**：通过 `setTheme()` API 设置，无需用户手动选择
3. **充满时间计算**：前端自动计算，公式为 `(总容量 - 当前能量) / (输入功率 - 输出功率)`，单位为秒
4. **低能量警报**：当能量低于 20% 时显示红色警告，低于 50% 时显示橙色警告
5. **设备开关**：点击设备列表中的开关可切换供能状态，可通过 `onDeviceToggle()` 设置回调
