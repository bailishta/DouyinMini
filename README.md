# 抖音Mini

> 精简版抖音 Android App，只保留刷视频和看直播的核心功能。

## 下载

[下载 APK (v1.0.0)](https://github.com/bailishta/DouyinMini/releases/download/v1.0.0/app-debug.apk)

需要 Android 7.0 及以上系统。

## 简介

抖音Mini 是一个基于 Kotlin 原生开发的 Android 壳应用，通过 WebView 加载抖音桌面版网页来实现视频浏览和直播观看。

采用桌面版 User-Agent 伪装，让抖音服务器以为是电脑浏览器访问，从而直接展示视频内容页面而非 App 下载页。

## 功能

- 全屏 WebView 浏览抖音视频
- 直播观看支持
- 全屏视频播放
- 深色模式（跟随系统或手动切换）
- 页面加载进度条
- 网络断开自动提示与重试
- 返回键导航（WebView 历史回退）

## 技术栈

| 项目 | 内容 |
|------|------|
| 语言 | Kotlin |
| 最低 SDK | 24 (Android 7.0) |
| 目标 SDK | 34 |
| 依赖 | AndroidX WebKit, AppCompat, Material Design |

## 项目结构

```
app/src/main/java/com/douyinmini/
├── MainActivity.kt              # 核心编排：WebView + 进度条 + 深色模式
├── config/
│   └── WebViewConfig.kt         # WebSettings 集中配置
├── ui/
│   ├── FullscreenVideoLayout.kt # 全屏视频容器
│   └── ErrorOverlayView.kt      # 离线错误覆盖层
└── util/
    ├── NetworkMonitor.kt        # 网络状态实时监听
    └── ThemeUtils.kt            # 深色模式工具
```

## 构建

```bash
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## 原理

抖音网页版对移动端 WebView 会强制跳转到 App 下载页。本 App 通过设置桌面端 Chrome 的 User-Agent，让抖音服务器返回桌面版页面，从而在移动端 WebView 中正常加载视频内容。用户在网页中登录抖音账号后即可正常使用。

## License

MIT
