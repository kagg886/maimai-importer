# Maimai-Importer-No-Proxy

无代理方案的maimai分数导出实践



## 1. 技术原理

通过逆向微信web端，在用户扫码完成后，登录舞萌net获取数据

## 2. 开发进度

- [x] 后端
  - [x] 定义查分器协议规范
  - [x] 微信扫码登录
  - [x] 微信OAuth登录舞萌net
  - [x] 重连时自动连接正在导入分数的会话
    - [x] 通过wxUin重连
    - [ ] 通过session-HashCode重连
- [x] Web前端
  - [x] 动态生成配置表单
  - [x] 连接到websocket
  - [x] 展示微信二维码
  - [x] 显示服务器日志

## 3. 体验

1. clone该项目

2. 打开`src/test/kotlin/top/kagg886/maimai/ApplicationTest.kt`

3. 跳转到78行，在此处填写水鱼查分器的username和password还有diff字段

   > 从0到5，diff为Basic Advance Expert Master Re:Maser

4. 运行`testRoot()`

5. 扫描二维码

6. 等待maimai数据导出完毕