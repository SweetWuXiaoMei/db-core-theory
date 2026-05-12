package com.ourdatabase.buffer;

import java.util.*;

public class BufferPool {
    private final int capacity;
    private final Map<Integer, BufferPage> pageMap;
    private final LinkedList<Integer> lruList;
    private long hitCount = 0;
    private long missCount = 0;
    private boolean showDiskAccess = true;

    public BufferPool(int capacity) {
        this.capacity = capacity;
        this.pageMap = new LinkedHashMap<>();
        this.lruList = new LinkedList<>();
    }

    public String getPage(int pageNumber) {
        BufferPage page = pageMap.get(pageNumber);
        if (page != null) {
            hitCount++;
            page.touch();
            moveToLRUHead(pageNumber);
            if (showDiskAccess)
                System.out.println("    [HIT] page" + pageNumber + " in memory! (hitRate=" + String.format("%.1f%%", getHitRate() * 100) + ")");
            return "page" + pageNumber + " data (from cache)";
        }

        missCount++;
        if (showDiskAccess)
            System.out.println("    [MISS] page" + pageNumber + " loading from disk... (hitRate=" + String.format("%.1f%%", getHitRate() * 100) + ")");

        if (pageMap.size() >= capacity) evictLRU();

        String data = loadFromDisk(pageNumber);
        BufferPage newPage = new BufferPage(pageNumber, data.getBytes());
        pageMap.put(pageNumber, newPage);
        lruList.addFirst(pageNumber);
        return data;
    }

    public void modifyPage(int pageNumber, String newData) {
        BufferPage page = pageMap.get(pageNumber);
        if (page == null) { System.out.println("    [ERR] page not in pool"); return; }
        page.markDirty();
        page.touch();
        moveToLRUHead(pageNumber);
        System.out.println("    [MODIFY] page" + pageNumber + " marked dirty");
    }

    public void flushPage(int pageNumber) {
        BufferPage page = pageMap.get(pageNumber);
        if (page == null || !page.isDirty()) return;
        System.out.println("    [FLUSH] page" + pageNumber + " -> disk (dirty->clean)");
        page.markClean();
    }

    public void flushAll() {
        int count = 0;
        for (BufferPage page : pageMap.values()) {
            if (page.isDirty()) { flushPage(page.getPageNumber()); count++; }
        }
        if (count > 0) System.out.println("    -> flushed " + count + " dirty pages");
    }

    private void evictLRU() {
        Iterator<Integer> it = lruList.descendingIterator();
        while (it.hasNext()) {
            int pn = it.next();
            BufferPage page = pageMap.get(pn);
            if (page != null && page.canEvict()) {
                if (page.isDirty()) flushPage(pn);
                pageMap.remove(pn);
                it.remove();
                System.out.println("    [EVICT] page" + pn + " removed from pool");
                return;
            }
        }
        System.out.println("    [WARN] cannot evict any page!");
    }

    private void moveToLRUHead(int pageNumber) {
        lruList.remove(Integer.valueOf(pageNumber));
        lruList.addFirst(pageNumber);
    }

    private String loadFromDisk(int pageNumber) { return "page" + pageNumber + " raw data"; }

    public double getHitRate() { long total = hitCount + missCount; return total > 0 ? (double) hitCount / total : 0.0; }

    public void printStatus() {
        System.out.println("\n  ===== BufferPool Status =====");
        System.out.println("  capacity: " + capacity + " pages");
        System.out.println("  cached: " + pageMap.size() + " pages");
        System.out.println("  hitRate: " + String.format("%.1f%%", getHitRate() * 100));
        int dirty = 0;
        for (BufferPage p : pageMap.values()) if (p.isDirty()) dirty++;
        System.out.println("  dirty: " + dirty);
        System.out.println("  LRU (recent->oldest):");
        for (int pn : lruList) {
            BufferPage p = pageMap.get(pn);
            if (p != null) System.out.println("    " + p.getSummary());
        }
    }
}
