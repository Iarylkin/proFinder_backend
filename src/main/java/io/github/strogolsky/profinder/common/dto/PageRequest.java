package io.github.strogolsky.profinder.common.dto;

import io.github.strogolsky.profinder.common.validation.Limits;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.Data;

/**
 * Basic DTO for handling pagination parameters in API requests.
 * <p>
 * Out-of-range values are not an error: a page below 1 is read as the first page and a size outside
 * 1 - {@value Limits#PAGE_SIZE_MAX} is reduced to the nearest limit.
 * </p>
 */
@Data
public class PageRequest {

    /**
     * The page number to retrieve, starting from 1.
     */
    @QueryParam("page")
    @DefaultValue("1")
    private int page;

    /**
     * The number of items per page, 20 when the client sends none.
     */
    @QueryParam("size")
    @DefaultValue("20")
    private int size;

    /**
     * Returns the page number, at least 1.
     *
     * @return the page number
     */
    public int getPage() {
        return Math.max(page, Limits.PAGE_MIN);
    }

    /**
     * Returns the page size, between 1 and {@value Limits#PAGE_SIZE_MAX}.
     *
     * @return the page size
     */
    public int getSize() {
        return Math.clamp(size, 1, Limits.PAGE_SIZE_MAX);
    }
}
