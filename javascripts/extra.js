// 搜索框占位符：置灰显示 "按 / 搜索"
// 参考: https://squidfunk.github.io/mkdocs-material/setup/setting-up-site-search/

document.addEventListener('DOMContentLoaded', function() {
  // 使用 MutationObserver 等待搜索框渲染完成
  const observer = new MutationObserver(function(mutations, obs) {
    const searchInput = document.querySelector('.md-search__input');
    if (searchInput) {
      // 设置中文占位符
      searchInput.setAttribute('placeholder', '按 / 搜索');

      // 监听 focus 事件，聚焦时清空占位符（原生行为已支持）
      // 监听 blur 事件，若值为空则恢复占位符
      searchInput.addEventListener('blur', function() {
        if (this.value === '') {
          this.setAttribute('placeholder', '按 / 搜索');
        }
      });

      searchInput.addEventListener('focus', function() {
        this.setAttribute('placeholder', '');
      });

      obs.disconnect(); // 找到后停止观察
    }
  });

  observer.observe(document.body, {
    childList: true,
    subtree: true
  });
});
