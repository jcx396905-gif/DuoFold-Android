# DuoFold for Android

[![Android](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)](https://github.com/jcx396905-gif/DuoFold-Android)
[![Shizuku](https://img.shields.io/badge/Shizuku-required-7C4DFF)](https://github.com/RikkaApps/Shizuku)
[![Release](https://img.shields.io/github/v/release/jcx396905-gif/DuoFold-Android?display_name=tag)](https://github.com/jcx396905-gif/DuoFold-Android/releases/latest)
[![License](https://img.shields.io/github/license/jcx396905-gif/DuoFold-Android)](LICENSE)
[![Stars](https://img.shields.io/github/stars/jcx396905-gif/DuoFold-Android?style=flat)](https://github.com/jcx396905-gif/DuoFold-Android/stargazers)

**A system-wide iPhone Duo-style folding illusion for regular Android phones — no foldable hardware and no root required.**

DuoFold turns the entire Android display into a motion-driven spatial surface. As you tilt your phone left/right or forward/backward, the current screen is reprojected in real time with perspective, translation, blur, lighting, and depth effects. The motion pauses when the phone stops and reverses naturally when you tilt it back.

**English** · [简体中文](#简体中文)

## Demo

<a href="https://github.com/jcx396905-gif/DuoFold-Android/releases/download/v0.4.0/DuoFold-IMG-0019.mov"><img src="media/extra-demo-cover.jpg" width="360" alt="DuoFold latest real-device demo"></a>

▶ [Watch the latest real-device demo (~23 s)](https://github.com/jcx396905-gif/DuoFold-Android/releases/download/v0.4.0/DuoFold-IMG-0019.mov)

## More test videos

<a href="https://github.com/jcx396905-gif/DuoFold-Android/releases/download/v0.4.0/DuoFold-full-test.mp4"><img src="media/full-test-cover.jpg" width="360" alt="DuoFold full test video"></a>

▶ [Watch the full test video (~30 s)](https://github.com/jcx396905-gif/DuoFold-Android/releases/download/v0.4.0/DuoFold-full-test.mp4)

<a href="https://github.com/jcx396905-gif/DuoFold-Android/releases/download/v0.4.0/DuoFold-demo-clip.mp4"><img src="media/demo-clip-cover.jpg" width="360" alt="DuoFold short demo clip"></a>

▶ [Watch the short demo clip (~6 s)](https://github.com/jcx396905-gif/DuoFold-Android/releases/download/v0.4.0/DuoFold-demo-clip.mp4)

## Screenshots

<p align="center">
  <img src="media/app-setup.jpg" width="320" alt="DuoFold setup screen">
  <img src="media/app-settings.jpg" width="320" alt="DuoFold settings and author screen">
  <img src="media/app-effects.png" width="320" alt="DuoFold 0.6.0 animation effects">
</p>

## Download

- **Android 14 and newer:** download `DuoFold-0.6.1-Android14-and-newer.apk` from the [Latest Release](https://github.com/jcx396905-gif/DuoFold-Android/releases/latest).
- **Android 10–13:** download `DuoFold-0.6.1-Android10-13.apk` from the same [Latest Release](https://github.com/jcx396905-gif/DuoFold-Android/releases/latest).

## Features

- Treats the entire active Android screen as one continuous spatial surface and applies perspective, translation, frosted blur, and lighting effects.
- Uses device motion sensors to drive the effect in real time. Stop moving the phone and the animation stops; tilt in the opposite direction and it reverses naturally.
- Left/right motion is enabled by default. Optional **Z-axis sensing · forward/back tilt** adds front/back motion, with diagonal movement blended continuously.
- Automatically adapts to portrait and landscape orientation and maps sensor axes to the current display coordinates.
- Does not modify launcher layouts or third-party app UIs; the effect is rendered over whatever interface you are currently using.
- On Android 14+, the effect can be stopped with a three-finger touch, the notification action, or by returning to DuoFold. The compatibility build uses the notification stop action.

## Animation effects (v0.6.0)

Choose a style in the app's **Animation effects** card. Changes are saved and applied immediately while the effect is running. **Classic Frosted** remains the default, and Z-axis sensing remains disabled by default in both Android builds.

| Style | Tilt and hover behavior |
| --- | --- |
| Classic Frosted · default | Original distance blur, grain, and shading; the frosted appearance remains while the phone is still. |
| Edge Fade | Keeps the anchored edge clearer while the opening edge gradually blurs and darkens. |
| Soft Depth | Uses softer weighted sampling and lighter shading for a subtle glass depth effect. |
| Hover Clear | Blurs during motion, then clears gradually after the phone has remained still for about half a second. |
| Clear Projection | Keeps the folding perspective without added blur or shading. |
| Lens Bokeh | Uses an independent 30° camera projection, a sharp near edge, quadratic far-edge blur, 16-tap disc bokeh, and continuous texture prefiltering. |

The first five styles share the classic projection. **Lens Bokeh** uses independent two-axis edge rotation and camera projection; Android 14+ touch coordinates follow that projection. Horizontal tilt is capped at 60°, forward/back tilt at 80°, and **Spatial strength** controls the aperture intensity.

Android 10/11 still use the static base frame captured when the effect starts; choosing another style does not make capture live on those versions.

## Requirements

- The standard build requires Android 14 or newer. The compatibility build supports Android 10 and newer.
- [Shizuku](https://github.com/RikkaApps/Shizuku) must be installed and running.
- **No root required.**
- Tested on **Xiaomi 15 / Android 16**. Screen-capture and overlay restrictions may vary between Android vendors.

## Setup

1. Download the matching build from the [Latest Release](https://github.com/jcx396905-gif/DuoFold-Android/releases/latest): use the standard build on Android 14+ and the compatibility build on Android 10–13.
2. Open Shizuku and start its service using wireless debugging or ADB, following Shizuku's instructions.
3. Open DuoFold and tap **01 Connect & authorize Shizuku**, then allow access in the authorization dialog.
4. Tap **02 Enable screen-effect accessibility service**, find DuoFold, and enable the service.
5. Hold the phone naturally while facing the screen and tap **03 Try for 10 seconds**. Tilt the phone left and right to see the folding illusion.
6. Adjust **Maximum tilt angle** and **Spatial strength** to your preference. Z-axis sensing is disabled by default in both builds; enable **Z-axis sensing · forward/back tilt** only if you want front/back motion. Changes apply the next time the effect starts.
7. Tap **04 Enable global effect**. If you change your grip, return to the app and tap **Recalibrate current grip**.
8. Android 14+ can stop the effect with a three-finger touch or the notification action. Android 10–13 uses the notification stop action.

If the accessibility service appears connected but the effect does not start, toggle the accessibility service off and on once. On some Xiaomi systems, touch correction also requires **USB debugging (Security settings)** in Developer options.

## How it works

DuoFold uses Shizuku to access the current display image and passes it through local memory to an OpenGL ES renderer. The game rotation vector and gyroscope are used to calculate relative device pose. If those sensors are unavailable, the compatibility build falls back to the gravity sensor or accelerometer. The frame is reconstructed according to the current display orientation, and calibration updates automatically after portrait/landscape changes.

The capture service detects AOSP and vendor ROM variants of `ScreenCapture`, `ScreenCaptureInternal`, and legacy `SurfaceControl`, then selects an available backend. This includes Android 16 custom ROMs where `getInternalDisplayToken()` is absent.

Default parameters use an **80° maximum tilt angle**, **100% spatial strength**, **6–32 blur samples**, and **40 ms gyroscope prediction**. To limit power consumption, capture width is capped at **1080 px** and updates are capped at **30 FPS**.

## Privacy and limitations

- The app has no Internet permission. During normal use, screen images are neither saved nor uploaded.
- The effect overlay is excluded from system screenshots to prevent recursive screen capture.
- Android 10/11 lack the per-layer exclusion API. On those versions, the compatibility build captures one real frame when the effect starts and then uses sensors to manipulate that frame in real time. Restarting the effect or rotating the display refreshes the base frame.
- Android 10–13 do not support the accessibility touch-coordinate correction API introduced in Android 14. If interaction becomes difficult while tilted, return to the calibrated pose first.
- Protected or screenshot-blocked content may appear black.
- DuoFold changes only the final displayed image. It does not rearrange icons, text, or components inside third-party apps.
- Extended use increases GPU load and battery consumption.

## Screen-off recovery (0.6.2 source; release signing pending)

Global mode now preserves its resume request across screen-off, delayed Shizuku reconnection, and system accessibility-service recreation. The overlay, sensors, and touch remapping are suspended while the device is non-interactive or locked. Recovery checks the actual power/keyguard state on screen-on, unlock, service connection, and window events; it waits for Shizuku and rebuilds the capture surface. Failed resume starts are limited to three attempts. Manual stop, trial mode, and disabling the accessibility service cancel recovery.

This addresses code paths implicated by [#4](https://github.com/jcx396905-gif/DuoFold-Android/issues/4) and [#6](https://github.com/jcx396905-gif/DuoFold-Android/issues/6). Samsung S24 Ultra validation is still required; no device-specific fix is claimed. If Android revokes accessibility access or Shizuku permission, restore it in system/Shizuku settings; the app does not bypass those controls.

## Build from source

JDK 17 and Android SDK 36 are required:

```shell
./gradlew :app:assembleStandardDebug :app:assembleCompatDebug :motion:test
```

The Android 14+ standard build is generated at `app/build/outputs/apk/standard/debug/app-standard-debug.apk`. The Android 10+ compatibility build is generated at `app/build/outputs/apk/compat/debug/app-compat-debug.apk`.

The project contains two modules: `app` handles Shizuku integration, accessibility services, screen capture, and OpenGL ES rendering; `motion` handles pose-axis mapping, filtering, and projection math and can run independent JVM unit tests.

### Release signing

Use the original signing key for an in-place update. Never generate a replacement debug key and label it as an upgrade-compatible release. Without an explicit key path, release tasks produce **unsigned APKs**, which cannot be installed until signed.

```shell
# Set DUOFOLD_STORE_PASSWORD and DUOFOLD_KEY_PASSWORD privately when not using the old debug-key defaults.
./gradlew :app:assembleStandardRelease :app:assembleCompatRelease \
  -PduofoldKeystore=/absolute/path/to/original.keystore \
  -PduofoldKeyAlias=androiddebugkey
```

Compare the resulting APK signing certificate with the previous release before publishing. Debug APKs generated on another machine generally cannot replace the published APK.

## Author

[jcx](https://github.com/jcx396905-gif)

## License

Released under the MIT License. Third-party license notices are available at `app/src/main/assets/THIRD_PARTY_NOTICES.txt`.

---

# 简体中文

## Duo Fold for Android

在普通 Android 设备上实现 **iPhone Duo 风格的整屏翻折动效**，无需折叠屏硬件，也无需 Root。界面会跟随手机的左右、前后倾斜实时投影，可暂停、反向移动，并支持横竖屏方向映射。

[Back to English](#duofold-for-android)

## 最新实机演示

<a href="https://github.com/jcx396905-gif/DuoFold-Android/releases/download/v0.4.0/DuoFold-IMG-0019.mov"><img src="media/extra-demo-cover.jpg" width="360" alt="Duo Fold 最新实机演示封面"></a>

▶ [播放最新实机演示（约 23 秒）](https://github.com/jcx396905-gif/DuoFold-Android/releases/download/v0.4.0/DuoFold-IMG-0019.mov)

## 更多测试视频

<a href="https://github.com/jcx396905-gif/DuoFold-Android/releases/download/v0.4.0/DuoFold-full-test.mp4"><img src="media/full-test-cover.jpg" width="360" alt="完整测试视频封面"></a>

▶ [播放完整测试视频（约 30 秒）](https://github.com/jcx396905-gif/DuoFold-Android/releases/download/v0.4.0/DuoFold-full-test.mp4)

<a href="https://github.com/jcx396905-gif/DuoFold-Android/releases/download/v0.4.0/DuoFold-demo-clip.mp4"><img src="media/demo-clip-cover.jpg" width="360" alt="演示切片封面"></a>

▶ [播放演示切片（约 6 秒）](https://github.com/jcx396905-gif/DuoFold-Android/releases/download/v0.4.0/DuoFold-demo-clip.mp4)

## 软件截图

<p align="center">
  <img src="media/app-setup.jpg" width="320" alt="Duo Fold 开启步骤">
  <img src="media/app-settings.jpg" width="320" alt="Duo Fold 参数设置与作者入口">
  <img src="media/app-effects.png" width="320" alt="Duo Fold 0.6.0 动画效果选择">
</p>

## 下载

- **Android 14 及以上：**从 [Latest Release](https://github.com/jcx396905-gif/DuoFold-Android/releases/latest) 下载 `DuoFold-0.6.1-Android14-and-newer.apk`。
- **Android 10–13：**从同一个 [Latest Release](https://github.com/jcx396905-gif/DuoFold-Android/releases/latest) 下载 `DuoFold-0.6.1-Android10-13.apk`。

## 功能

- 将正在使用的整个 Android 屏幕作为连续空间进行透视、位移、磨砂与明暗处理。
- 使用手机姿态传感器实时控制动画，停住手机时画面同步停住，反向倾斜时动画反向播放。
- 默认响应左右翻转；开启“Z 轴感应 · 前后倾斜”后，同时响应前后方向，斜向动作会连续合成。
- 自动适配横屏和竖屏，并将传感器方向映射到当前屏幕坐标。
- 不修改桌面或第三方应用布局，可在当前正在使用的界面上显示效果。
- Android 14+ 可用三指触屏、通知栏操作或返回 Duo Fold 停止效果；兼容版使用通知栏停止按钮。

## 动画效果选择（0.6.0）

在应用内的“动画效果”卡片中选择方案。选择自动保存，正在运行时也会立即应用；默认保持原有的经典磨砂。两种 Android 构建共用这些选项，Z 轴仍默认关闭。

| 方案 | 倾斜与悬停表现 |
| --- | --- |
| 经典磨砂 · 默认 | 保留原有的距离模糊、颗粒和暗部，停住后保持磨砂。 |
| 边缘渐隐 | 固定边缘更清楚，展开侧逐渐模糊、变暗，停住后保持渐变。 |
| 柔和景深 | 柔和加权采样、较轻暗部，停住后保留轻微玻璃感。 |
| 悬停清晰 | 移动时模糊，停住约半秒后逐渐清晰；保持透视位置，再次移动恢复模糊。 |
| 清透投影 | 保留翻折透视，关闭磨砂和额外暗部。 |
| 镜头散景 | 独立的 30° 镜头透视，近边清晰、远边按距离平方化开；16 点圆盘散景与连续纹理预过滤，悬停保持景深。 |

前五个方案共用经典投影。“镜头散景”使用独立的双轴边缘旋转与镜头投影，Android 14+ 的触控坐标同步使用该投影；左右方向最多 60°、前后方向最多 80°，空间强度对应光圈强度。默认仍选中经典磨砂，切换后保存用户选择。

Android 10/11 仍使用开启时取得的静态底图，选择其他动画不会使底图变成实时画面。

## 使用要求

- 标准版需要 Android 14 或更高版本；兼容版最低支持 Android 10。
- [Shizuku](https://github.com/RikkaApps/Shizuku) 已安装并运行。
- 无需 Root。
- 已在 Xiaomi 15、Android 16 上完成实机测试。其他厂商系统对屏幕捕获和悬浮层的限制可能不同。

## 使用教程

1. 从 [Latest Release](https://github.com/jcx396905-gif/DuoFold-Android/releases/latest) 下载对应版本：Android 14 及以上使用标准版，Android 10–13 使用兼容版。
2. 打开 Shizuku，按其提示通过无线调试或电脑 ADB 启动服务。
3. 打开 Duo Fold，点击“01 连接并授权 Shizuku”，在授权窗口中允许访问。
4. 点击“02 开启屏幕效果辅助服务”，找到 Duo Fold 并启用服务。
5. 正对屏幕，以自然握姿点击“03 体验 10 秒”。左右倾斜手机即可查看翻折效果。
6. 根据手感调整“最大倾斜角度”和“空间强度”。两个版本的 Z 轴感应均默认关闭；需要前后方向时再打开“Z 轴感应 · 前后倾斜”。参数在下一次开启效果时生效。
7. 点击“04 开启全局效果”。需要更换握姿时返回应用，点击“重新校准当前握姿”。
8. Android 14+ 可三指同时触屏或使用通知栏停止按钮退出；Android 10–13 使用通知栏停止按钮。

如果辅助服务显示已连接但无法启动效果，请重新打开一次辅助服务开关。部分 Xiaomi 系统在修正触控时还需要打开开发者选项中的“USB 调试（安全设置）”。

## 工作方式

Duo Fold 通过 Shizuku 获取当前显示画面，在本机内存中交给 OpenGL ES 投影。游戏旋转矢量与陀螺仪用于计算相对姿态；没有这些传感器时，兼容版会使用重力计或加速度计。画面根据当前显示方向重建，横竖屏切换后自动重新校准。

画面服务会自动识别 AOSP 与厂商 ROM 的 `ScreenCapture`、`ScreenCaptureInternal` 和旧版 `SurfaceControl` 接口，并按系统实际提供的接口降级，兼容 Android 16 定制系统移除 `getInternalDisplayToken()` 的情况。

默认参数使用 80° 最大倾斜角度、100% 空间强度、6–32 个模糊采样和 40 ms 陀螺仪预测。为控制功耗，捕获宽度上限为 1080 像素，更新上限为每秒 30 次。

## 隐私与限制

- 应用没有网络权限，正常使用时不保存、不上传屏幕画面。
- 效果层会从系统截图中排除，避免画面被重复捕获。
- Android 10/11 缺少单图层排除接口，兼容版会在开启时获取一帧真实画面，再让传感器实时控制这一帧的翻折；重新开启或旋转屏幕时会刷新底图。
- Android 10–13 不支持 Android 14 新增的辅助服务触摸坐标修正接口，倾斜后操作界面时可先回到校准姿态。
- 受保护或禁止截屏的界面可能显示黑色。
- 应用只改变最终显示效果，不会重新排列第三方应用内部的图标、文字或组件。
- 长时间使用会增加 GPU 负载与耗电。

## 从源码构建

需要 JDK 17 和 Android SDK 36：

```shell
./gradlew :app:assembleStandardDebug :app:assembleCompatDebug :motion:test
```

Android 14+ 标准版位于 `app/build/outputs/apk/standard/debug/app-standard-debug.apk`；Android 10+ 兼容版位于 `app/build/outputs/apk/compat/debug/app-compat-debug.apk`。

项目包含两个模块：`app` 负责 Shizuku、辅助服务、画面捕获和 OpenGL ES 渲染；`motion` 负责姿态轴映射、滤波与投影数学，可独立运行 JVM 单元测试。

## 作者

[jcx](https://github.com/jcx396905-gif)

## 许可证

项目以 MIT License 发布。第三方许可证声明见 `app/src/main/assets/THIRD_PARTY_NOTICES.txt`。
