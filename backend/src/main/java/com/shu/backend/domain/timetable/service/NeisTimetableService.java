package com.shu.backend.domain.timetable.service;

import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.timetable.dto.TimetableDTO;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.Grade;
import com.shu.backend.global.neis.NeisApiClient;
import com.shu.backend.global.neis.NeisSchoolSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NeisTimetableService {

    private final NeisApiClient neisApiClient;
    private final NeisSchoolSyncService neisSchoolSyncService;

    @Cacheable(
            value = "timetable",
            key = "'v7_' + #user.school.id + '_' + #user.grade.name() + '_' + #classRoom + '_' + #from + '_' + #to",
            unless = "#result.periods.isEmpty()"
    )
    public TimetableDTO.WeekResponse getTimetable(
            User user, String classRoom, String from, String to) {

        String normalizedClassRoom = normalizeClassRoom(classRoom);
        String gradeNum = toNeisGrade(user.getGrade());
        if (gradeNum == null) {
            return TimetableDTO.WeekResponse.builder()
                    .grade("0").classRoom(normalizedClassRoom).periods(List.of()).neisAvailable(false).build();
        }

        // NEIS 코드 없으면 학교명으로 자동 조회·저장 시도
        School school = user.getSchool();
        if (school.getNeisOfficeCode() == null || school.getNeisSchoolCode() == null) {
            school = neisSchoolSyncService.syncIfMissing(school);
        }

        if (school.getNeisOfficeCode() == null || school.getNeisSchoolCode() == null) {
            return TimetableDTO.WeekResponse.builder()
                    .grade(gradeNum).classRoom(normalizedClassRoom).periods(List.of()).neisAvailable(false).build();
        }

        LocalDate fromDate = LocalDate.parse(from, DateTimeFormatter.BASIC_ISO_DATE);
        LocalDate toDate = LocalDate.parse(to, DateTimeFormatter.BASIC_ISO_DATE);

        List<Map<String, Object>> rows = fetchWeekByDateAndPeriod(
                school, gradeNum, normalizedClassRoom, fromDate, toDate);

        List<TimetableDTO.Period> periods = rows.stream()
                .map(row -> TimetableDTO.Period.builder()
                        .date(stringValue(row.get("ALL_TI_YMD")))
                        .dayOfWeek(parseDayOfWeek(stringValue(row.get("ALL_TI_YMD"))))
                        .period(parseIntSafe(row.get("PERIO")))
                        .subject(stringValue(row.get("ITRT_CNTNT")))
                        .build())
                .filter(period -> period.getDayOfWeek() >= 1 && period.getDayOfWeek() <= 5)
                .filter(period -> period.getPeriod() > 0)
                .sorted(Comparator
                        .comparing(TimetableDTO.Period::getDate)
                        .thenComparingInt(TimetableDTO.Period::getPeriod))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                period -> period.getDate() + "_" + period.getPeriod(),
                                period -> period,
                                (first, ignored) -> first,
                                LinkedHashMap::new
                        ),
                        map -> new ArrayList<>(map.values())
                ));

        return TimetableDTO.WeekResponse.builder()
                .grade(gradeNum)
                .classRoom(normalizedClassRoom)
                .periods(periods)
                .neisAvailable(true)
                .build();
    }

    private String toNeisGrade(Grade grade) {
        return switch (grade) {
            case FIRST -> "1";
            case SECOND -> "2";
            case THIRD -> "3";
            default -> null; // GRADUATED
        };
    }

    private int parseDayOfWeek(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) return 0;
        LocalDate d = LocalDate.parse(yyyymmdd, java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        return d.getDayOfWeek().getValue(); // 1=월 ~ 7=일
    }

    private int parseIntSafe(Object value) {
        try { return Integer.parseInt(stringValue(value).trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private List<Map<String, Object>> fetchWeekByDateAndPeriod(
            School school,
            String gradeNum,
            String classRoom,
            LocalDate fromDate,
            LocalDate toDate) {

        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        LocalDate date = fromDate;

        while (!date.isAfter(toDate)) {
            int dayOfWeek = date.getDayOfWeek().getValue();
            if (dayOfWeek >= 1 && dayOfWeek <= 5) {
                String dateParam = date.format(DateTimeFormatter.BASIC_ISO_DATE);
                TimetableRows dayRows = fetchDateWithSemesterFallback(
                        school, gradeNum, classRoom, date, dateParam);

                if (!dayRows.rows().isEmpty()) {
                    rows.addAll(dayRows.rows());
                    for (int period = 1; period <= 8; period++) {
                        rows.addAll(neisApiClient.getTimetablePeriod(
                                school.getNeisOfficeCode(),
                                school.getNeisSchoolCode(),
                                toAcademicYear(date), dayRows.semester(), gradeNum, classRoom, dateParam, period));
                    }
                } else {
                    for (int period = 1; period <= 8; period++) {
                        rows.addAll(fetchPeriodWithSemesterFallback(
                                school, gradeNum, classRoom, date, dateParam, period));
                    }
                }
            }
            date = date.plusDays(1);
        }

        return rows;
    }

    private TimetableRows fetchDateWithSemesterFallback(
            School school,
            String gradeNum,
            String classRoom,
            LocalDate date,
            String dateParam) {

        String ay = toAcademicYear(date);
        for (String semester : semesterCandidates(date)) {
            List<Map<String, Object>> rows = neisApiClient.getTimetableDate(
                    school.getNeisOfficeCode(),
                    school.getNeisSchoolCode(),
                    ay, semester, gradeNum, classRoom, dateParam
            );

            if (!rows.isEmpty()) {
                log.debug("[NEIS] timetable loaded: schoolId={}, date={}, ay={}, sem={}, grade={}, class={}, rows={}",
                        school.getId(), dateParam, ay, semesterLabel(semester), gradeNum, classRoom, rows.size());
                return new TimetableRows(semester, rows);
            }
        }

        return new TimetableRows("", List.of());
    }

    private List<Map<String, Object>> fetchPeriodWithSemesterFallback(
            School school,
            String gradeNum,
            String classRoom,
            LocalDate date,
            String dateParam,
            int period) {

        String ay = toAcademicYear(date);
        for (String semester : semesterCandidates(date)) {
            List<Map<String, Object>> rows = neisApiClient.getTimetablePeriod(
                    school.getNeisOfficeCode(),
                    school.getNeisSchoolCode(),
                    ay, semester, gradeNum, classRoom, dateParam, period
            );

            if (!rows.isEmpty()) {
                log.debug("[NEIS] timetable period loaded: schoolId={}, date={}, ay={}, sem={}, grade={}, class={}, period={}, rows={}",
                        school.getId(), dateParam, ay, semesterLabel(semester), gradeNum, classRoom, period, rows.size());
                return rows;
            }
        }

        return List.of();
    }

    private String toAcademicYear(LocalDate date) {
        return String.valueOf(date.getMonthValue() <= 2 ? date.getYear() - 1 : date.getYear());
    }

    private List<String> semesterCandidates(LocalDate date) {
        String preferred = preferredSemester(date);
        String alternate = "1".equals(preferred) ? "2" : "1";
        return List.of(preferred, alternate, "");
    }

    private String preferredSemester(LocalDate date) {
        int month = date.getMonthValue();
        return month >= 3 && month <= 8 ? "1" : "2";
    }

    private String normalizeClassRoom(String classRoom) {
        if (classRoom == null) {
            return "";
        }

        String normalized = classRoom.trim()
                .replaceAll("\\s+", "")
                .replace('０', '0')
                .replace('１', '1')
                .replace('２', '2')
                .replace('３', '3')
                .replace('４', '4')
                .replace('５', '5')
                .replace('６', '6')
                .replace('７', '7')
                .replace('８', '8')
                .replace('９', '9');

        if (normalized.endsWith("반")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private String semesterLabel(String semester) {
        return semester == null || semester.isBlank() ? "none" : semester;
    }

    private record TimetableRows(String semester, List<Map<String, Object>> rows) {
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
