package com.example.bank.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Spring'in kendi Page<T> nesnesini doğrudan JSON olarak dışarı vermek yerine bunu
 * kullanıyoruz - Spring'in Page çıktısı gereğinden fazla iç detay (pageable, sort
 * nesnesi vb.) içerir ve versiyon değişikliklerinde JSON şekli kırılabilir. Bu sınıf,
 * frontend'in ihtiyaç duyduğu minimum ve sabit alan setini garanti eder.
 */
public class PageResponse<T> {
    private List<T> content;
    private int page;         // 0 tabanlı mevcut sayfa numarası
    private int size;         // sayfa başına istenen eleman sayısı
    private long totalElements;
    private int totalPages;
    private boolean last;     // bu son sayfa mı

    public static <T> PageResponse<T> from(Page<T> page) {
        PageResponse<T> response = new PageResponse<>();
        response.content = page.getContent();
        response.page = page.getNumber();
        response.size = page.getSize();
        response.totalElements = page.getTotalElements();
        response.totalPages = page.getTotalPages();
        response.last = page.isLast();
        return response;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }
}
