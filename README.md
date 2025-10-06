# 小趣空间第三方客户端 (BBQ的继任者QUBOT)

![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-7F52FF.svg?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.6.8-4285F4.svg?logo=jetpackcompose)
![Version](https://img.shields.io/badge/Version-9.1-brightgreen.svg)

**小趣空间第三方客户端 (QUBOT)** 是对原 iApp 版本 "小趣空间" 的一次彻底的现代化重构。本项目旨在利用当前主流的 Android 开发技术栈，为用户提供一个更流畅、更美观、功能更完善的社区和资源分享平台。

项目完全开源，遵循 **GNU General Public License v3.0** 协议。所有数据和资源均来源于官方平台 `app.xiaoqu.online`，本项目仅作为其第三方客户端。

## 技术特色 🚀

本次重构的核心是采用了一整套现代化的 Android 开发技术和架构模式，旨在提高开发效率、应用性能和代码可维护性。

- **现代架构 (MVVM)**: 项目严格遵循 **Model-View-ViewModel (MVVM)** 架构模式。通过 `ViewModel` 将业务逻辑与UI分离，利用 `Repository` 模式（如 `PlazaRepository`）封装数据源，使得代码结构清晰，职责分明。

- **100% 声明式UI (Jetpack Compose)**: 整个应用的 UI 层完全由 **Jetpack Compose** 构建。相较于传统的 XML 布局，Compose 提供了更直观、更灵活的 UI 开发体验。项目广泛使用了 Material 3 设计语言，实现了动态主题、组件化和响应式布局。

- **先进的并发模型 (Kotlin Coroutines)**: 所有异步操作，如网络请求、数据库访问，均通过 **Kotlin 协程** 在 `viewModelScope` 中进行管理。这确保了主线程的流畅性，避免了回调地狱，使异步代码如同步代码般易于读写。

- **响应式编程 (Kotlin Flow)**: `ViewModel` 中的数据状态通过 **StateFlow** 对外暴露，UI 层使用 `collectAsState` 进行订阅。这种响应式数据流确保了数据驱动UI的模式，当数据发生变化时，UI 会自动、高效地更新，保证了数据的一致性。

- **模块化设计**: 项目代码按功能模块进行组织，例如 `ui/community`、`ui/user`、`ui/plaza` 等，每个模块都包含其独立的 `ViewModel` 和 `Screen`。`data` 包负责本地数据持久化，`api` 包定义网络服务，结构清晰，易于扩展和维护。

## 功能特性 ✨

本项目不仅复刻了原版的核心功能，还在此基础上进行了扩展和优化。

## 技术栈 📚

- **核心语言**: [Kotlin](https://kotlinlang.org/) `1.9.23`
- **UI 框架**:
  - [Jetpack Compose](https://developer.android.com/jetpack/compose) BOM: `2025.01.01`
  - [Material 3](https://m3.material.io/) (Design System)
- **架构模式**:
  - **单Activity架构** - 基于 Navigation Compose 的现代化架构
  - [MVVM (Model-View-ViewModel)](https://developer.android.com/jetpack/guide)
  - [Repository Pattern](https://developer.android.com/jetpack/guide/data-layer)
- **导航系统**:
  - [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) `2.8.9`
  - **全局侧边栏导航** - 提供快速屏幕切换体验
  - **极速屏幕切换** - 基于 Compose 的即时渲染
- **网络请求**:
  - [Retrofit2](https://square.github.io/retrofit/) `2.9.0`
  - [OkHttp3](https://square.github.io/okhttp/) `4.12.0`
  - [Moshi](https://github.com/square/moshi) `1.15.1`
- **异步编程**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) `1.8.0` & [Flow](https://kotlinlang.org/docs/flow.html)
- **本地存储**: 
  - [Jetpack DataStore (Preferences)](https://developer.android.com/topic/libraries/architecture/datastore) `1.0.0`
  - [Room Database](https://developer.android.com/training/data/room) `2.6.1`
- **依赖注入**: [Koin](https://insert-koin.io/) `4.0.4`
- **图片加载**: [Coil](https://coil-kt.github.io/coil/) `2.7.0`
- **视频播放**: [ijkplayer](https://github.com/bilibili/ijkplayer) `0.0.2`
- **弹幕**: [DanmakuFlameMaster](https://github.com/bilibili/DanmakuFlameMaster)
- **其他**:
  - [ImagePicker](https://github.com/Dhaval2404/ImagePicker) `2.1` (图片选择与裁剪)
  - [PhotoView](https://github.com/chrisbanes/PhotoView) `2.3.0` (图片手势缩放)
  - [Lifecycle Service](https://developer.android.com/jetpack/androidx/releases/lifecycle) `2.8.0`

## 架构特色 🏗️

### 单Activity架构优势
- **极速导航**: 屏幕间切换无延迟，提供原生般的流畅体验
- **状态共享**: 全局状态在Activity级别管理，跨屏幕数据共享
- **统一生命周期**: 简化组件生命周期管理
- **内存优化**: 减少多Activity带来的内存开销

### 全局侧边栏设计
- **快速访问**: 一键跳转到任何主要功能模块
- **用户体验**: 符合Material Design规范的导航模式
- **状态持久**: 导航状态在应用生命周期内保持一致性

这样的架构设计在性能和用户体验上都达到了现代Android应用的最高标准！

## 项目结构

项目采用清晰的、基于功能的模块化包结构，便于维护和扩展。