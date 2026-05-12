package com.ourdatabase.page;

import java.util.*;

/**
 * 页管理器 —— 管理所有数据页的分配、访问和释放
 *
 * 页管理器是存储引擎的核心组件之一。
 * 它负责：
 * 1. 分配新页（当旧页满了时）
 * 2. 根据页号找到对应的页
 * 3. 管理页之间的链表关系（上一页 ↔ 下一页）
 * 4. 提供统一的插入、查询、删除接口
 *
 * 在真实数据库中，页管理器还需要：
 * - 管理磁盘上的页文件（每个页读写到文件对应位置）
 * - 空闲空间管理（哪些页还有空间，能快速找到）
 * - 和缓冲池配合（第5章）
 */
public class PageManager {

    // 所有页面，按页号索引
    private Map<Integer, DataPage> pageMap = new LinkedHashMap<>();

    // 下一个可用的页号
    private int nextPageNumber = 0;

    // 表名和它的第一个页号
    private String tableName;
    private int firstPageNumber = -1;
    private String[] columnNames;

    /**
     * 构造函数
     *
     * @param tableName 表名
     * @param columnNames 列名数组
     */
    public PageManager(String tableName, String[] columnNames) {
        this.tableName = tableName;
        this.columnNames = columnNames;
    }

    /**
     * 分配一个新页
     *
     * @return 新分配的页
     */
    private DataPage allocatePage() {
        DataPage newPage = new DataPage(nextPageNumber);
        pageMap.put(nextPageNumber, newPage);

        // 更新页链表
        if (firstPageNumber == -1) {
            firstPageNumber = nextPageNumber;
        } else {
            // 找到当前最后一页，将它的"下一页"指向新页
            DataPage lastPage = findLastPage();
            if (lastPage != null) {
                lastPage.setNextPageNumber(nextPageNumber);
                newPage.setPrevPageNumber(lastPage.getPageNumber());
            }
        }

        nextPageNumber++;
        return newPage;
    }

    /**
     * 插入一行数据
     *
     * 插入逻辑：
     * 1. 如果有页，尝试在最后一页插入
     * 2. 如果最后一页满了（空间不够），分配新页
     * 3. 在新页中插入
     *
     * @param rowData 要插入的数据行
     * @return 插入到了哪个页
     */
    public DataPage insertRow(String[] rowData) {
        DataPage targetPage;

        // 1. 如果还没有任何页，先分配一页
        if (firstPageNumber == -1) {
            targetPage = allocatePage();
        } else {
            // 2. 获取最后一页
            targetPage = findLastPage();
        }

        // 3. 尝试插入
        if (!targetPage.canFit(rowData)) {
            // 页满了，分配新页
            System.out.println("    -> 页" + targetPage.getPageNumber() + "已满，分配新页...");
            targetPage = allocatePage();
        }

        targetPage.insertRow(rowData);
        return targetPage;
    }

    /**
     * 根据主键查询数据
     *
     * 查询逻辑：
     * 1. 遍历所有页（顺序扫描）
     * 2. 在每页中查找
     * 3. 找到后返回
     *
     * 注意：这是全表扫描！在第7章中，B+树索引会让查询变成O(log n)。
     *
     * @param primaryKeyValue 要查询的主键
     * @return 找到的行，null=未找到
     */
    public String[] findRow(String primaryKeyValue) {
        int currentPageNumber = firstPageNumber;

        while (currentPageNumber != -1) {
            DataPage currentPage = pageMap.get(currentPageNumber);
            if (currentPage == null) break;

            String[] result = currentPage.findRow(primaryKeyValue);
            if (result != null) {
                return result;
            }

            currentPageNumber = currentPage.getNextPageNumber();
        }

        return null;
    }

    /**
     * 删除一行
     */
    public boolean deleteRow(String primaryKeyValue) {
        int currentPageNumber = firstPageNumber;

        while (currentPageNumber != -1) {
            DataPage currentPage = pageMap.get(currentPageNumber);
            if (currentPage == null) break;

            if (currentPage.deleteRow(primaryKeyValue)) {
                return true;
            }

            currentPageNumber = currentPage.getNextPageNumber();
        }

        return false;
    }

    /**
     * 查询所有行
     */
    public List<String[]> findAllRows() {
        List<String[]> result = new ArrayList<>();
        int currentPageNumber = firstPageNumber;

        while (currentPageNumber != -1) {
            DataPage currentPage = pageMap.get(currentPageNumber);
            if (currentPage == null) break;

            result.addAll(currentPage.getAllRows());
            currentPageNumber = currentPage.getNextPageNumber();
        }

        return result;
    }

    /**
     * 找到页链表的最后一页
     */
    private DataPage findLastPage() {
        int currentPageNumber = firstPageNumber;
        DataPage currentPage = null;

        while (currentPageNumber != -1) {
            currentPage = pageMap.get(currentPageNumber);
            if (currentPage == null) break;
            currentPageNumber = currentPage.getNextPageNumber();
        }

        return currentPage;
    }

    /**
     * 打印所有页的使用情况
     */
    public void printPageUsage() {
        System.out.println("\n  ===== 页使用情况 =====");
        System.out.println("  总页数: " + pageMap.size());

        int currentPageNumber = firstPageNumber;
        while (currentPageNumber != -1) {
            DataPage currentPage = pageMap.get(currentPageNumber);
            if (currentPage == null) break;
            System.out.println("  " + currentPage.getUsageReport());
            currentPageNumber = currentPage.getNextPageNumber();
        }
    }

    // ============ Getter ============

    public String getTableName() { return tableName; }
    public int getTotalPageCount() { return pageMap.size(); }
    public int getFirstPageNumber() { return firstPageNumber; }
}
