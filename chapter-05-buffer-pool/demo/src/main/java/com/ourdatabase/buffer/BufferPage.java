package com.ourdatabase.buffer;

public class BufferPage {
    private int pageNumber;
    private byte[] data;
    private boolean isDirty;
    private int pinCount;
    private long lastAccessTime;

    public BufferPage(int pageNumber, byte[] data) {
        this.pageNumber = pageNumber;
        this.data = data;
        this.isDirty = false;
        this.pinCount = 0;
        this.lastAccessTime = System.nanoTime();
    }

    public void markDirty() { this.isDirty = true; }
    public void markClean() { this.isDirty = false; }
    public void pin() { this.pinCount++; }
    public void unpin() { if (this.pinCount > 0) this.pinCount--; }
    public void touch() { this.lastAccessTime = System.nanoTime(); }
    public boolean canEvict() { return pinCount <= 0; }

    public int getPageNumber() { return pageNumber; }
    public byte[] getData() { return data; }
    public boolean isDirty() { return isDirty; }
    public int getPinCount() { return pinCount; }
    public long getLastAccessTime() { return lastAccessTime; }

    public String getSummary() {
        return String.format("page%d [%s] [pin=%d]", pageNumber, isDirty ? "dirty" : "clean", pinCount);
    }
}
