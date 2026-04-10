# ArcFlow TV Launcher 開發技術文件

## 1\. 專案概述

**ArcFlow (弧流桌面)** 是一款專為 Android TV 設計的極簡啟動器，強調流暢的視覺動畫與高度自定義的媒體背景。

* **核心理念**：極簡、流暢、沉浸式體驗。  
* **招牌功能**：左下角隱藏式弧形選單、動態影片桌布、5x3 分頁式應用集。

---

## 2\. UI/UX 規格設計

### 2.1 桌面布局 (Main Activity)

* **時間顯示**：左上方，24小時制，下方 mm/DD 小字日期。  
* **天氣顯示**：右上方，由 WorkManager 每 30 分鐘更新一次。  
* **背景層次 (Z-Order)**：  
  1. **底層 (Z=-1)**：`SurfaceView` (ExoPlayer)，`setZOrderOnTop(false)`。  
  2. **中層 (Z=0)**：靜態備援桌布 (`APP_BG.jpg`)。  
  3. **頂層 (Z=1)**：時間、天氣、狀態資訊。  
  4. **覆蓋層 (Z=2)**：弧形功能選單。

### 2.2 弧形選單 (Custom ArcMenuLayout)

* **交互邏輯**：預設焦點在左下角圓形按鈕，按下 OK 鍵觸發。  
* **數學模型**：  
  * 中心點 (0, 0\) 位於左下角。  
  * 位置：$x \= R \\cdot \\cos(\\theta)$, $y \= \-R \\cdot \\sin(\\theta)$。  
  * 夾角：$0^\\circ$ 至 $90^\\circ$ 平均分配。  
* **動畫效果**：  
  * 展開/收縮：半徑 R 與子項 ScaleX/Y 同時從 0 變換到 1 (或相反)。  
  * 狀態判定：動畫期間鎖定點擊 (AnimationState Flag)，防止連擊衝突。

### 2.3 全部應用頁面 (AppDrawerActivity)

* **佈局**：5x3 網格分頁。  
* **組件**：`ViewPager2` 配合 `RecyclerView`。  
* **分頁導航**：  
  * 底部顯示圓點指示器 (Dots Indicator)。  
  * **主動跳轉**：焦點位於頁面邊界時，按下方向鍵自動執行 `setCurrentItem` 進行平滑滾動。  
* **視覺優化**：  
  * 使用 `Facebook Shimmer` 特效。  
  * `offscreenPageLimit = 1` 確保翻頁前已加載完成。

---

## 3\. 技術實作架構

### 3.1 數據管理

* **App 快取**：在 `Application` 層級維護 `packageName` 列表，減少 `PackageManager` 負載。  
* **同步機制**：  
  * 監聽 `ACTION_PACKAGE_ADDED` / `REMOVED` 廣播。  
  * 使用 `Interface Callback` 通知 Activity 刷新。  
* **持久化**：使用 `SharedPreferences` 儲存捷徑配置與天氣快取。

### 3.2 媒體與任務

* **背景播放**：`ExoPlayer`。進入 AppDrawer 時暫停，返回時從頭播放。  
* **定時任務**：使用 `WorkManager` 執行天氣 API 抓取。

---

## 4\. 功能選單規劃 (按鈕清單)

1. **影片APP捷徑**：長按開啟選單配置，儲存於 SP。  
2. **音樂APP捷徑**：長按開啟選單配置，儲存於 SP。  
3. **全部應用**：跳轉至 5x3 分頁 Activity。  
4. **設定**：  
   * 時間/天氣設定。  
   * 桌布管理（支援預設 20 張圖、自定義圖/影片）。  
5. **收起**：縮回選單。  
6. **關於**：顯示 **ArcFlow TV Launcher** 版本與開發資訊。

---

## 5\. 開發 Roadmap

1. **Phase 1**：實作 `AppRepository` 快取與廣播監聽架構。  
2. **Phase 2**：自定義 `ArcMenuLayout` 座標運算與插值器動畫。  
3. **Phase 3**：`ViewPager2` 焦點攔截邏輯與分頁動畫實作。  
4. **Phase 4**：`ExoPlayer` 與 `SurfaceView` 層級處理。  
5. **Phase 5**：整合 `WorkManager` 與設定頁面。

