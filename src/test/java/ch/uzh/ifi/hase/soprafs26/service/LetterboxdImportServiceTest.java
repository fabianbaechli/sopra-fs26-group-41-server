package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LetterboxdImportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
// FIX: Added the missing import for ResponseStatusException
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class LetterboxdImportServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MovieSearchService movieSearchService;

    @InjectMocks
    private LetterboxdImportService importService;

    private User testUser;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("cinephile");
    }

    @Test
    public void importZip_validCsv_success() throws Exception {
        // Create a mock zip containing a ratings.csv file
        String csvContent = "Date,Name,Year,Letterboxd URI,Rating\n" +
                "2023-01-01,Dune,2021,uri,5.0\n" +
                "2023-01-02,Jaws,1975,uri,3.0\n";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("ratings.csv");
            zos.putNextEntry(entry);
            zos.write(csvContent.getBytes());
            zos.closeEntry();
        }

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "data.zip", "application/zip", baos.toByteArray());

        Mockito.when(movieSearchService.fetchInternalMovieIds(Mockito.anyList()))
                .thenReturn(Map.of("Dune", "int-101", "Jaws", "int-102"));

        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(i -> i.getArgument(0));

        LetterboxdImportResponse response = importService.importZip(testUser, mockFile);

        assertNotNull(response);
        assertTrue(response.getHasLetterboxdData());
        assertEquals(2, response.getStats().getMoviesLogged());
        assertEquals(1, response.getStats().getHighlyRatedMovies()); // Only Dune is >= 4.5

        Mockito.verify(userRepository, Mockito.times(1)).save(testUser);
        Mockito.verify(userRepository, Mockito.times(1)).flush();
    }

    @Test
    public void importZip_missingRequiredColumns_throwsException() throws Exception {
        // Missing the "Rating" column
        String csvContent = "Date,Name,Year,Letterboxd URI\n" +
                "2023-01-01,Dune,2021,uri\n";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("ratings.csv");
            zos.putNextEntry(entry);
            zos.write(csvContent.getBytes());
            zos.closeEntry();
        }

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "data.zip", "application/zip", baos.toByteArray());

        assertThrows(ResponseStatusException.class, () -> importService.importZip(testUser, mockFile));
    }

    @Test
    public void importZip_invalidRatingFormat_throwsException() throws Exception {
        // Rating is "Five" instead of a number
        String csvContent = "Date,Name,Year,Letterboxd URI,Rating\n" +
                "2023-01-01,Dune,2021,uri,Five\n";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("ratings.csv");
            zos.putNextEntry(entry);
            zos.write(csvContent.getBytes());
            zos.closeEntry();
        }

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "data.zip", "application/zip", baos.toByteArray());

        assertThrows(ResponseStatusException.class, () -> importService.importZip(testUser, mockFile));
    }

    @Test
    public void importZip_fileNotFoundInZip_throwsException() throws Exception {
        // Zip contains "wrong_file.csv" instead of "ratings.csv"
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("wrong_file.csv");
            zos.putNextEntry(entry);
            zos.write("Some data".getBytes());
            zos.closeEntry();
        }

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "data.zip", "application/zip", baos.toByteArray());

        assertThrows(java.io.IOException.class, () -> importService.importZip(testUser, mockFile));
    }
}