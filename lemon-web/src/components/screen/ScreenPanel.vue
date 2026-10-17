<template>
  <section class="screen-panel">
    <header class="panel-head">
      <span class="panel-title">{{ title }}</span>
      <span class="panel-extra"><slot name="extra" /></span>
    </header>
    <div class="panel-body"><slot /></div>
    <i class="corner tl" /><i class="corner tr" /><i class="corner bl" /><i class="corner br" />
  </section>
</template>

<script setup>
/** 大屏面板：标题栏 + 内容区，四角装饰；内容区占满剩余高度 */
defineProps({ title: String })
</script>

<style scoped>
.screen-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 12px 16px 14px;
  background: linear-gradient(180deg, rgba(14, 46, 92, 0.55), rgba(8, 26, 56, 0.45));
  border: 1px solid rgba(64, 158, 255, 0.22);
  box-shadow: inset 0 0 24px rgba(0, 140, 255, 0.12);
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 34px;
  margin-bottom: 8px;
  padding-left: 14px;
  background: linear-gradient(90deg, rgba(0, 228, 255, 0.18), transparent 70%);
  border-left: 3px solid #00e4ff;
}
.panel-title {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #e6f4ff;
}
.panel-extra {
  font-size: 13px;
  color: rgba(160, 190, 230, 0.8);
}
.panel-body {
  flex: 1;
  min-height: 0;
  position: relative;
}
.corner {
  position: absolute;
  width: 14px;
  height: 14px;
  border-color: #00e4ff;
  border-style: solid;
  pointer-events: none;
}
.tl { top: -1px; left: -1px; border-width: 2px 0 0 2px; }
.tr { top: -1px; right: -1px; border-width: 2px 2px 0 0; }
.bl { bottom: -1px; left: -1px; border-width: 0 0 2px 2px; }
.br { bottom: -1px; right: -1px; border-width: 0 2px 2px 0; }
</style>
