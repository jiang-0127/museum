<template>
  <div class="not-found">
    <span class="code serif">404</span>
    <span class="eyebrow">PAGE NOT FOUND</span>
    <h1 class="serif">这一页，暂时不在展</h1>
    <p>你访问的页面可能已移动或不存在。不如回到展厅入口，重新选择一条参观路线。</p>
    <div class="actions">
      <el-button type="primary" round @click="$router.push('/')">返回首页</el-button>
      <el-button round plain @click="$router.push('/museums')">浏览博物馆</el-button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 48px 24px;
  text-align: center;
  background:
    radial-gradient(circle at 50% 40%, rgba(176, 138, 79, 0.12), transparent 40%),
    var(--paper);
}

.code {
  margin-bottom: 18px;
  color: var(--gold);
  font-size: clamp(90px, 16vw, 180px);
  font-weight: 400;
  line-height: 1;
  opacity: 0.65;
}

h1 {
  margin-top: 18px;
  color: var(--ink);
  font-size: clamp(30px, 4vw, 46px);
  font-weight: 500;
  letter-spacing: 0.05em;
}

p {
  max-width: 460px;
  margin-top: 16px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.9;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 32px;
}
</style>
