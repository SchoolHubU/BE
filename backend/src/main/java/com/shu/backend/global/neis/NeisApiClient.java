package com.shu.backend.global.neis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NeisApiClient {

    private static final String HIGH_SCHOOL = "\uACE0\uB4F1\uD559\uAD50";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String baseUrl;

    public NeisApiClient(
            @Value("${neis.api.key}") String apiKey,
            @Value("${neis.api.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public List<Map<String, Object>> getMealInfo(
            String officeCode, String schoolCode,
            String fromDate, String toDate) {

        return fetchPagedRows("mealServiceDietInfo", page -> UriComponentsBuilder.fromUriString(baseUrl + "/mealServiceDietInfo")
                .queryParam("KEY", apiKey)
                .queryParam("Type", "json")
                .queryParam("pIndex", page)
                .queryParam("pSize", 100)
                .queryParam("ATPT_OFCDC_SC_CODE", officeCode)
                .queryParam("SD_SCHUL_CODE", schoolCode)
                .queryParam("MLSV_FROM_YMD", fromDate)
                .queryParam("MLSV_TO_YMD", toDate)
                .queryParam("MMEAL_SC_CODE", "2")
                .encode().build().toUriString());
    }

    private List<Map<String, Object>> fetchPagedRows(String rootKey, java.util.function.IntFunction<String> urlFactory) {
        if (!hasApiKey()) {
            log.warn("[NEIS] API key is not configured. Skip {} request.", rootKey);
            return List.of();
        }

        final int maxPages = 50;
        ArrayList<Map<String, Object>> allRows = new ArrayList<>();
        Set<String> seenRows = new HashSet<>();

        for (int page = 1; page <= maxPages; page++) {
            List<Map<String, Object>> rows = parseNeisResponse(urlFactory.apply(page), rootKey);
            if (rows.isEmpty()) {
                break;
            }

            boolean addedAny = false;
            for (Map<String, Object> row : rows) {
                String signature = row.toString();
                if (seenRows.add(signature)) {
                    allRows.add(row);
                    addedAny = true;
                }
            }

            if (!addedAny) {
                log.warn("[NEIS] {} 페이지 반복 응답 감지: page={}", rootKey, page);
                break;
            }
        }

        return allRows;
    }

    public List<Map<String, Object>> getTimetable(
            String officeCode, String schoolCode,
            String ay, String sem,
            String grade, String classNm,
            String fromDate, String toDate) {

        return fetchPagedRows("hisTimetable", page -> timetableBuilder(page, officeCode, schoolCode, ay, sem, grade, classNm)
                .queryParam("TI_FROM_YMD", fromDate)
                .queryParam("TI_TO_YMD", toDate)
                .encode().build().toUriString());
    }

    public List<Map<String, Object>> getTimetablePeriod(
            String officeCode, String schoolCode,
            String ay, String sem,
            String grade, String classNm,
            String date, int period) {

        List<Map<String, Object>> exactRows = filterTimetableRows(parseNeisResponse(timetableBuilder(1, officeCode, schoolCode, ay, sem, grade, classNm)
                .queryParam("ALL_TI_YMD", date)
                .queryParam("PERIO", String.valueOf(period))
                .encode().build().toUriString(), "hisTimetable"), date, period);

        if (!exactRows.isEmpty()) {
            return exactRows;
        }

        return filterTimetableRows(parseNeisResponse(timetableBuilder(1, officeCode, schoolCode, ay, sem, grade, classNm)
                .queryParam("TI_FROM_YMD", date)
                .queryParam("TI_TO_YMD", date)
                .queryParam("PERIO", String.valueOf(period))
                .encode().build().toUriString(), "hisTimetable"), date, period);
    }

    public List<Map<String, Object>> getTimetableDate(
            String officeCode, String schoolCode,
            String ay, String sem,
            String grade, String classNm,
            String date) {

        return fetchPagedRows("hisTimetable", page -> timetableBuilder(page, officeCode, schoolCode, ay, sem, grade, classNm)
                .queryParam("ALL_TI_YMD", date)
                .encode().build().toUriString()).stream()
                .filter(row -> date.equals(String.valueOf(row.getOrDefault("ALL_TI_YMD", ""))))
                .collect(Collectors.toList());
    }

    private UriComponentsBuilder timetableBuilder(
            int page,
            String officeCode,
            String schoolCode,
            String ay,
            String sem,
            String grade,
            String classNm) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + "/hisTimetable")
                .queryParam("KEY", apiKey)
                .queryParam("Type", "json")
                .queryParam("pIndex", String.valueOf(page))
                .queryParam("pSize", "1000")
                .queryParam("ATPT_OFCDC_SC_CODE", officeCode)
                .queryParam("SD_SCHUL_CODE", schoolCode)
                .queryParam("AY", ay)
                .queryParam("GRADE", grade)
                .queryParam("CLASS_NM", classNm);

        if (sem != null && !sem.isBlank()) {
            builder.queryParam("SEM", sem);
        }

        return builder;
    }

    private List<Map<String, Object>> filterTimetableRows(List<Map<String, Object>> rows, String date, int period) {
        String periodValue = String.valueOf(period);
        return rows.stream()
                .filter(row -> date.equals(String.valueOf(row.getOrDefault("ALL_TI_YMD", ""))))
                .filter(row -> periodValue.equals(String.valueOf(row.getOrDefault("PERIO", "")).trim()))
                .collect(Collectors.toList());
    }

    /**
     * 학교 정보 조회 — 학교명으로 NEIS 코드 자동 검색.
     *
     * RestTemplate.exchange() 는 응답 타입(Map.class)에 따라 Jackson 컨버터가
     * Accept: application/json 을 강제 설정해 WebtoB 서버가 500을 반환함.
     * HttpURLConnection 을 직접 사용해 브라우저와 동일한 헤더만 전송한다.
     */
    public List<Map<String, Object>> getSchoolInfo(String schoolName) {
        if (!hasApiKey()) {
            log.warn("[NEIS] API key is not configured. Skip schoolInfo request: school={}", schoolName);
            return List.of();
        }

        try {
            String encodedName = URLEncoder.encode(schoolName, StandardCharsets.UTF_8);
            String urlStr = baseUrl + "/schoolInfo?KEY=" + apiKey + "&Type=json&SCHUL_NM=" + encodedName;

            log.info("[NEIS] schoolInfo 요청: {}", urlStr);

            HttpURLConnection conn = openConnection(urlStr);
            try {
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setRequestProperty("Accept", "*/*");
                conn.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(15_000);

                int code = conn.getResponseCode();
                log.info("[NEIS] schoolInfo HTTP 응답코드: {}", code);
                if (code != 200) {
                    log.warn("[NEIS] schoolInfo 비정상 응답: HTTP {}", code);
                    return List.of();
                }

                String body;
                try (InputStream is = conn.getInputStream()) {
                    body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> response = objectMapper.readValue(body, Map.class);
                List<Map<String, Object>> rows = extractRows(response, "schoolInfo", urlStr);

                log.info("[NEIS] schoolInfo 전체 응답 행 수: {}", rows.size());
                rows.forEach(r -> log.info("[NEIS]   - 학교명={}, 종류={}, officeCode={}, schoolCode={}",
                        r.get("SCHUL_NM"), r.get("SCHUL_KND_SC_NM"),
                        r.get("ATPT_OFCDC_SC_CODE"), r.get("SD_SCHUL_CODE")));

                List<Map<String, Object>> filtered = rows.stream()
                        .filter(r -> "고등학교".equals(r.get("SCHUL_KND_SC_NM")))
                        .collect(Collectors.toList());

                log.info("[NEIS] 고등학교 필터링 후: {}건", filtered.size());
                return filtered;
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            log.warn("[NEIS] schoolInfo 호출 실패: school={}, error={}", schoolName, e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getHighSchoolInfoPage(int page, int size) {
        if (!hasApiKey()) {
            log.warn("[NEIS] API key is not configured. Skip high school import request.");
            return List.of();
        }

        String url = UriComponentsBuilder.fromUriString(baseUrl + "/schoolInfo")
                .queryParam("KEY", apiKey)
                .queryParam("Type", "json")
                .queryParam("pIndex", page)
                .queryParam("pSize", size)
                .queryParam("SCHUL_KND_SC_NM", HIGH_SCHOOL)
                .encode()
                .build()
                .toUriString();

        return parseNeisResponse(url, "schoolInfo").stream()
                .filter(row -> HIGH_SCHOOL.equals(String.valueOf(row.getOrDefault("SCHUL_KND_SC_NM", ""))))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseNeisResponse(String url, String rootKey) {
        try {
            HttpURLConnection conn = openConnection(url);
            try {
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setRequestProperty("Accept", "*/*");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(15_000);

                int code = conn.getResponseCode();
                if (code != 200) {
                    log.warn("[NEIS] {} HTTP {}", rootKey, code);
                    return List.of();
                }
                String body;
                try (InputStream is = conn.getInputStream()) {
                    body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
                Map<String, Object> response = objectMapper.readValue(body, Map.class);
                return extractRows(response, rootKey, url);
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            log.warn("[NEIS] API 호출 실패: url={}, error={}", url, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRows(Map<String, Object> response, String rootKey, String logRef) {
        if (response == null) {
            log.warn("[NEIS] 응답 null: ref={}", logRef);
            return List.of();
        }
        List<Map<String, Object>> root = (List<Map<String, Object>>) response.get(rootKey);
        if (root == null) {
            log.warn("[NEIS] '{}' 키 없음. 전체 응답: {}", rootKey, response);
            return List.of();
        }
        if (root.size() < 2) {
            log.warn("[NEIS] 데이터 블록 부족 (size={}). head={}", root.size(), root.get(0));
            return List.of();
        }
        Map<String, Object> headBlock = root.get(0);
        List<Map<String, Object>> headRows = (List<Map<String, Object>>) headBlock.get("head");
        Integer totalCount = null;
        if (headRows != null && !headRows.isEmpty()) {
            log.info("[NEIS] head: rootKey={}, head={}", rootKey, headRows);
            totalCount = extractTotalCount(headRows);
        }
        Map<String, Object> dataBlock = root.get(1);
        List<Map<String, Object>> rows = (List<Map<String, Object>>) dataBlock.get("row");
        log.info("[NEIS] 파싱 완료: rootKey={}, rows={}", rootKey, rows != null ? rows.size() : 0);
        if (totalCount != null && rows != null && totalCount > rows.size() && rows.size() == 5) {
            log.warn("[NEIS] {} 응답이 5건으로 제한되었습니다. total={}, rows={}. 인증키 인식 또는 URL 인코딩을 확인하세요.",
                    rootKey, totalCount, rows.size());
        }
        return rows != null ? rows : List.of();
    }

    private Integer extractTotalCount(List<Map<String, Object>> headRows) {
        for (Map<String, Object> headRow : headRows) {
            Object value = headRow.get("list_total_count");
            if (value == null) {
                continue;
            }
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    private HttpURLConnection openConnection(String url) throws Exception {
        URL targetUrl = URI.create(url).toURL();
        return (HttpURLConnection) targetUrl.openConnection();
    }
}
