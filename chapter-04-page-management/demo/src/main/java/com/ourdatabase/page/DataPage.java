package com.ourdatabase.page;

import java.util.*;

/**
 * 数据页 —— 数据库读写磁盘的最小单位
 *
 * 这是数据库物理存储的核心数据结构！
 *
 * 关键概念：
 * - 一页的大小是固定的（本Demo设为256字节，便于观察；真实数据库通常16KB）
 * - 一行数据不能跨页存储
 * - 页内行记录通过"单向链表"连接
 * - 删除行时只标记删除，不立即回收空间
 *
 * 页的内部布局（简化版）：
 * ┌──────────────────────────────┐
 * │  页头（页号、行数、空闲偏移等） │
 * ├──────────────────────────────┤
 * │  行记录区（实际的用户数据）    │
 * │  [行1] → [行2] → [行3] → ...│
 * ├──────────────────────────────┤
 * │  空闲空间（还能插入新行）      │
 * └──────────────────────────────┘
 */
public class DataPage {

    /** 页大小（字节），设为256方便观察和调试 */
    public static final int PAGE_SIZE = 256;

    /** 页头大小（字节），存储元信息 */
    private static final int HEADER_SIZE = 16;

    // ==================== 页头字段 ====================

    private int pageNumber;         // 这一页的唯一编号（从0开始）
    private int rowCount;           // 当前页有多少行（包括已删除的行）
    private int freeSpaceOffset;    // 空闲空间的起始位置（新行从这里开始写）
    private int prevPageNumber;     // 前一页的页号（-1表示没有）
    private int nextPageNumber;     // 后一页的页号（-1表示没有）

    // ==================== 页内数据 ====================

    // 行记录列表（每个元素是一行数据）
    private List<String[]> rowRecords;

    // 删除标记（true表示该行已被删除）
    private List<Boolean> deleteFlags;

    /**
     * 构造函数 —— 创建一个全新的空页
     *
     * @param pageNumber 这一页的编号
     */
    public DataPage(int pageNumber) {
        this.pageNumber = pageNumber;
        this.rowCount = 0;
        this.freeSpaceOffset = HEADER_SIZE; // 新页的空闲空间从页头之后开始
        this.prevPageNumber = -1;
        this.nextPageNumber = -1;
        this.rowRecords = new ArrayList<>();
        this.deleteFlags = new ArrayList<>();
    }

    /**
     * 在页中插入一行数据
     *
     * @param rowData 要插入的数据行
     * @return true=插入成功，false=空间不足
     */
    public boolean insertRow(String[] rowData) {
        // 计算这一行需要的空间
        int requiredSpace = calculateRowSpace(rowData);

        // 检查是否有足够的空闲空间
        if (freeSpaceOffset + requiredSpace > PAGE_SIZE) {
            return false; // 页满了！
        }

        // 在行记录列表末尾添加
        rowRecords.add(rowData);
        deleteFlags.add(false); // 标记为未删除

        // 移动空闲空间偏移
        freeSpaceOffset += requiredSpace;
        rowCount++;

        return true;
    }

    /**
     * 根据主键查找一行数据
     *
     * @param primaryKeyValue 要查找的主键值
     * @return 找到的行数据，未找到返回null
     */
    public String[] findRow(String primaryKeyValue) {
        for (int i = 0; i < rowRecords.size(); i++) {
            // 跳过已删除的行
            if (deleteFlags.get(i)) continue;

            String[] row = rowRecords.get(i);
            // 第一列是主键
            if (row[0].equals(primaryKeyValue)) {
                return row;
            }
        }
        return null;
    }

    /**
     * 删除一行数据（标记删除，不立即回收空间）
     *
     * @param primaryKeyValue 要删除的主键值
     * @return true=删除成功，false=未找到
     */
    public boolean deleteRow(String primaryKeyValue) {
        for (int i = 0; i < rowRecords.size(); i++) {
            if (deleteFlags.get(i)) continue;

            if (rowRecords.get(i)[0].equals(primaryKeyValue)) {
                deleteFlags.set(i, true); // 标记为已删除
                return true;
            }
        }
        return false;
    }

    /**
     * 整理页内碎片（将已删除行真正清除，整理空间）
     *
     * 真实数据库中，这个操作叫"Page Reorganize"。
     * 只有当页空间不够时才执行。
     */
    public void compactFragments() {
        List<String[]> newRowRecords = new ArrayList<>();
        List<Boolean> newDeleteFlags = new ArrayList<>();
        int newOffset = HEADER_SIZE;

        for (int i = 0; i < rowRecords.size(); i++) {
            if (!deleteFlags.get(i)) {
                newRowRecords.add(rowRecords.get(i));
                newDeleteFlags.add(false);
                newOffset += calculateRowSpace(rowRecords.get(i));
            }
        }

        this.rowRecords = newRowRecords;
        this.deleteFlags = newDeleteFlags;
        this.freeSpaceOffset = newOffset;
        this.rowCount = newRowRecords.size();
    }

    /**
     * 获取页内所有有效行（不包括已删除的）
     */
    public List<String[]> getAllRows() {
        List<String[]> validRows = new ArrayList<>();
        for (int i = 0; i < rowRecords.size(); i++) {
            if (!deleteFlags.get(i)) {
                validRows.add(rowRecords.get(i));
            }
        }
        return validRows;
    }

    /**
     * 获取页的使用情况（用于调试和监控）
     */
    public String getUsageReport() {
        int validRowCount = 0;
        int deletedRowCount = 0;
        for (int i = 0; i < deleteFlags.size(); i++) {
            if (deleteFlags.get(i)) {
                deletedRowCount++;
            } else {
                validRowCount++;
            }
        }

        return String.format("页号=%d, 总行数=%d, 有效行=%d, 已删除=%d, 空闲=%dB, 使用率=%d%%",
                pageNumber, rowCount, validRowCount, deletedRowCount,
                PAGE_SIZE - freeSpaceOffset,
                (freeSpaceOffset - HEADER_SIZE) * 100 / (PAGE_SIZE - HEADER_SIZE));
    }

    /**
     * 检查页是否还能容纳一行数据
     */
    public boolean canFit(String[] rowData) {
        return freeSpaceOffset + calculateRowSpace(rowData) <= PAGE_SIZE;
    }

    // ==================== 私有辅助方法 ====================

    /** 估算一行数据占用的字节数 */
    private int calculateRowSpace(String[] rowData) {
        int space = 4; // 行头（记录长度）
        for (String field : rowData) {
            space += 2; // 字段长度标记
            space += field.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        }
        return space;
    }

    // ==================== Getter / Setter ====================

    public int getPageNumber() { return pageNumber; }
    public int getRowCount() { return rowCount; }
    public int getValidRowCount() {
        return (int) deleteFlags.stream().filter(b -> !b).count();
    }
    public int getFreeSpace() { return PAGE_SIZE - freeSpaceOffset; }
    public int getPrevPageNumber() { return prevPageNumber; }
    public void setPrevPageNumber(int pageNum) { this.prevPageNumber = pageNum; }
    public int getNextPageNumber() { return nextPageNumber; }
    public void setNextPageNumber(int pageNum) { this.nextPageNumber = pageNum; }
}
