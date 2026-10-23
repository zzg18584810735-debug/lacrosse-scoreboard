# La Scoreboard Vue

Vue 3 迁移版棍网球计分台。

当前阶段保留原项目经过测试的比赛规则和计分界面，由 Vue 接管应用入口、主题生命周期以及后续组件化边界。后端请求统一放在 `src/api`。

```bash
npm install
npm run dev
npm test
npm run build
```

开发环境中的 `/api` 默认代理到 `http://localhost:8080`，可以通过 `VITE_API_BASE_URL` 覆盖。
