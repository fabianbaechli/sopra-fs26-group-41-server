package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LetterboxdImportResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class LetterboxdImportService {
    public LetterboxdImportResponse importZip(User user, MultipartFile file) throws IOException {
        // TODO:
        //   1. unzip [x]
        //   2. import/overwrite current user's Letterboxd data []
        //   3. compute stats []
        //   4. return updated user payload []
        String watchedCsv;
        try (InputStream is = file.getInputStream()) {
            watchedCsv = findFileContentInZip(is, "watched.csv", StandardCharsets.UTF_8);
        }
        LetterboxdImportResponse response = new LetterboxdImportResponse();
        LetterboxdImportResponse.Stats stats = new LetterboxdImportResponse.Stats();

        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setHasLetterboxdData(true);

        stats.setMoviesLogged(1);
        stats.setHighlyRatedMovies(1);
        stats.setTopGenres(Arrays.asList("Foo", "Bar", "Foobar"));
        response.setStats(stats);
        return response;
    }

    public static String findFileContentInZip(
            InputStream zipInputStream,
            String targetFilename,
            Charset charset
    ) throws IOException {

        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && matchesFilename(entry.getName(), targetFilename)) {
                    byte[] content = zis.readAllBytes();
                    return new String(content, charset);
                }
                zis.closeEntry();
            }
        }

        throw new IOException("File not found in zip: " + targetFilename);
    }

    private static boolean matchesFilename(String entryName, String targetFilename) {
        String normalizedEntry = entryName.replace("\\", "/");
        String normalizedTarget = targetFilename.replace("\\", "/");

        return normalizedEntry.equals(normalizedTarget)
                || normalizedEntry.endsWith("/" + normalizedTarget)
                || getBaseName(normalizedEntry).equals(normalizedTarget);
    }

    private static String getBaseName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
