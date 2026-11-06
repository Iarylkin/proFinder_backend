package fit.biejk.common.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageRequestTest {

    private static PageRequest request(final int page, final int size) {
        PageRequest request = new PageRequest();
        request.setPage(page);
        request.setSize(size);
        return request;
    }

    @Test
    void valuesInRangeAreKept() {
        PageRequest request = request(3, 50);

        assertEquals(3, request.getPage());
        assertEquals(50, request.getSize());
    }

    @Test
    void pageBelowOneIsTheFirstPage() {
        assertEquals(1, request(0, 10).getPage());
        assertEquals(1, request(-5, 10).getPage());
    }

    @Test
    void sizeIsClampedToTheLimits() {
        assertEquals(1, request(1, 0).getSize());
        assertEquals(1, request(1, -3).getSize());
        assertEquals(100, request(1, 1000).getSize());
    }
}
