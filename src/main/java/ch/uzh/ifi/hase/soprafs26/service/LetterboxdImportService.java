package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.entity.RatedMovie;
import ch.uzh.ifi.hase.soprafs26.entity.TasteProfile;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LetterboxdImportResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Transactional
public class LetterboxdImportService {
    private final UserRepository userRepository;

    public LetterboxdImportService(@Qualifier("userRepository") UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LetterboxdImportResponse importZip(User user, MultipartFile file) throws IOException {
        String ratingsCsv;
        try (InputStream is = file.getInputStream()) {
            ratingsCsv = findFileContentInZip(is, "ratings.csv", StandardCharsets.UTF_8);
        }

        List<RatedMovie> ratedMovies = parseRatedMovies(ratingsCsv);

        TasteProfile tasteProfile = new TasteProfile();
        tasteProfile.setRatedMovies(ratedMovies);
        user.setTasteProfile(tasteProfile);
        User savedUser = userRepository.save(user);
        userRepository.flush();

        LetterboxdImportResponse response = new LetterboxdImportResponse();
        LetterboxdImportResponse.Stats stats = new LetterboxdImportResponse.Stats();

        response.setId(savedUser.getId());
        response.setUsername(savedUser.getUsername());
        response.setHasLetterboxdData(!ratedMovies.isEmpty());

        stats.setMoviesLogged(ratedMovies.size());
        stats.setHighlyRatedMovies((int) ratedMovies.stream()
                // all movies with rating >= 4 are ranked highly
                .filter(movie ->   movie.getRating() >= 4.5)
                .count());
        stats.setTopGenres(Collections.emptyList());
        response.setStats(stats);
        return response;
    }

    static List<RatedMovie> parseRatedMovies(String ratingsCsv) throws IOException {
        try (BufferedReader reader = new BufferedReader(new StringReader(stripUtf8Bom(ratingsCsv)))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return new ArrayList<>();
            }

            List<String> headers = parseCsvLine(headerLine);
            int nameIndex = findRequiredColumnIndex(headers, "Name");
            int ratingIndex = findRequiredColumnIndex(headers, "Rating");

            List<RatedMovie> ratedMovies = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                List<String> columns = parseCsvLine(line);
                String movieName = getColumnValue(columns, nameIndex);
                String ratingValue = getColumnValue(columns, ratingIndex);

                if (movieName == null || movieName.isBlank() || ratingValue == null || ratingValue.isBlank()) {
                    continue;
                }

                RatedMovie ratedMovie = new RatedMovie();
                ratedMovie.setName(movieName.trim());
                ratedMovie.setRating(parseLetterboxdRating(ratingValue));
                ratedMovies.add(ratedMovie);
            }

            return ratedMovies;
        }
    }

    private static int findRequiredColumnIndex(List<String> headers, String columnName) {
        for (int i = 0; i < headers.size(); i++) {
            if (columnName.equalsIgnoreCase(headers.get(i).trim())) {
                return i;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "ratings.csv is missing required column: " + columnName);
    }

    private static String getColumnValue(List<String> columns, int index) {
        return index < columns.size() ? columns.get(index) : null;
    }

    private static int parseLetterboxdRating(String ratingValue) {
        try {
            return (int) Math.round(Double.parseDouble(ratingValue.trim()));
        }
        catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid movie rating in ratings.csv: " + ratingValue);
        }
    }

    static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);
            if (currentChar == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentValue.append('"');
                    i++;
                }
                else {
                    inQuotes = !inQuotes;
                }
            }
            else if (currentChar == ',' && !inQuotes) {
                values.add(currentValue.toString());
                currentValue.setLength(0);
            }
            else {
                currentValue.append(currentChar);
            }
        }

        values.add(currentValue.toString());
        return values;
    }

    private static String stripUtf8Bom(String content) {
        if (content != null && !content.isEmpty() && content.charAt(0) == '\uFEFF') {
            return content.substring(1);
        }
        return content;
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
