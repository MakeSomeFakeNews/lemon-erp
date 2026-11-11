<template>
  <!-- 布局缩略示意图：深色块为导航（主题色），浅色块为顶栏 / 第二栏，白色为内容区 -->
  <div :class="['thumb', `thumb-${layout}`]" aria-hidden="true">
    <template v-if="layout === LayoutMode.MIX">
      <i class="nav-light side" />
      <i class="bar top-right"><b class="dots" /></i>
    </template>
    <template v-else-if="layout === LayoutMode.SIDE">
      <i class="nav side"><b class="tree" /></i>
      <i class="bar top-right" />
    </template>
    <template v-else-if="layout === LayoutMode.TOP">
      <i class="nav top"><b class="dots" /></i>
    </template>
    <template v-else>
      <i class="nav rail" />
      <i class="nav-light second" />
      <i class="bar top-second" />
    </template>
    <i class="body" />
  </div>
</template>

<script setup>
import { LayoutMode } from '@/utils/uiSettings'

defineProps({ layout: { type: String, required: true } })
</script>

<style scoped>
.thumb {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: 6px;
  overflow: hidden;
  background: var(--app-bg);
}
.thumb i {
  position: absolute;
  display: block;
}
.nav {
  background: var(--app-primary);
}
.nav-light {
  background: color-mix(in srgb, var(--app-primary) 30%, var(--app-card));
}
.bar {
  background: var(--app-card);
  border-bottom: 1px solid var(--app-border);
}
.body {
  border-radius: 3px;
  background: var(--app-card);
}
/* 顶部模块 + 侧边菜单：侧边栏 + 顶栏中的模块 */
.thumb-mix .side {
  left: 0;
  top: 0;
  bottom: 0;
  width: 24%;
}
.thumb-mix .top-right,
.thumb-side .top-right {
  left: 24%;
  right: 0;
  top: 0;
  height: 18%;
}
.thumb-mix .body,
.thumb-side .body {
  left: calc(24% + 6px);
  right: 6px;
  top: calc(18% + 6px);
  bottom: 6px;
}
/* 侧边菜单：深色侧边栏 + 树形线条 */
.thumb-side .side {
  left: 0;
  top: 0;
  bottom: 0;
  width: 24%;
}
/* 顶部菜单：通栏 */
.thumb-top .top {
  left: 0;
  right: 0;
  top: 0;
  height: 18%;
}
.thumb-top .body {
  left: 6px;
  right: 6px;
  top: calc(18% + 6px);
  bottom: 6px;
}
/* 双栏：窄模块栏 + 第二栏 */
.thumb-column .rail {
  left: 0;
  top: 0;
  bottom: 0;
  width: 11%;
}
.thumb-column .second {
  left: 11%;
  top: 0;
  bottom: 0;
  width: 22%;
}
.thumb-column .top-second {
  left: 33%;
  right: 0;
  top: 0;
  height: 18%;
}
.thumb-column .body {
  left: calc(33% + 6px);
  right: 6px;
  top: calc(18% + 6px);
  bottom: 6px;
}
/* 菜单项示意 */
.dots {
  position: absolute;
  left: 10%;
  top: 50%;
  width: 50%;
  height: 3px;
  transform: translateY(-50%);
  border-radius: 2px;
  background: repeating-linear-gradient(90deg, currentColor 0 14%, transparent 14% 22%);
}
.bar .dots {
  color: var(--app-primary);
  opacity: 0.6;
}
.nav .dots {
  color: #fff;
  opacity: 0.85;
}
.tree {
  position: absolute;
  left: 18%;
  right: 18%;
  top: 12%;
  height: 60%;
  background: repeating-linear-gradient(180deg, rgba(255, 255, 255, 0.85) 0 3px, transparent 3px 9px);
}
</style>
