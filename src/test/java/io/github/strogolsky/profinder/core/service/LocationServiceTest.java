package io.github.strogolsky.profinder.core.service;

import io.github.strogolsky.profinder.core.entity.Location;
import io.github.strogolsky.profinder.core.repository.LocationRepository;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The locations of the reference data. A new profile gets the default one, {@code Prague}.
 */
class LocationServiceTest {

    private static final UUID PRAGUE_ID = UUID.randomUUID();

    private LocationRepository repository;
    private LocationService service;
    private Location prague;

    @BeforeEach
    void setUp() {
        repository = mock(LocationRepository.class);
        service = new LocationService(repository);
        prague = new Location();
        prague.setId(PRAGUE_ID);
        prague.setName("Prague");
        when(repository.findById(PRAGUE_ID)).thenReturn(prague);
        when(repository.findByName("Prague")).thenReturn(prague);
    }

    @Test
    void allLocationsAreRead() {
        when(repository.listAll()).thenReturn(List.of(prague));

        assertEquals(List.of(prague), service.getAll());
    }

    @Test
    void locationIsFoundById() {
        assertSame(prague, service.getById(PRAGUE_ID));
    }

    @Test
    void unknownIdIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getById(UUID.randomUUID()));
    }

    @Test
    void locationIsFoundByName() {
        assertSame(prague, service.getByName("Prague"));
    }

    @Test
    void unknownNameIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getByName("Atlantis"));
    }

    @Test
    void defaultLocationIsPrague() {
        assertSame(prague, service.getDefault());
    }

    @Test
    void withoutPragueThereIsNoDefault() {
        when(repository.findByName("Prague")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> service.getDefault());
    }
}
