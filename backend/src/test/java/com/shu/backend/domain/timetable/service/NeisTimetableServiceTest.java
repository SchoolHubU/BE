package com.shu.backend.domain.timetable.service;

import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.timetable.dto.TimetableDTO;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.Gender;
import com.shu.backend.domain.user.enums.Grade;
import com.shu.backend.global.neis.NeisApiClient;
import com.shu.backend.global.neis.NeisSchoolSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NeisTimetableServiceTest {

    @Mock
    private NeisApiClient neisApiClient;

    @Mock
    private NeisSchoolSyncService neisSchoolSyncService;

    @InjectMocks
    private NeisTimetableService neisTimetableService;

    @Test
    void getTimetableFallsBackToSecondSemesterForLateAugust() {
        School school = School.builder()
                .name("Test High")
                .neisOfficeCode("B10")
                .neisSchoolCode("7010000")
                .build();
        ReflectionTestUtils.setField(school, "id", 1L);
        User user = User.builder()
                .username("tester")
                .email("tester@example.com")
                .password("password")
                .nickname("tester")
                .school(school)
                .gender(Gender.MALE)
                .grade(Grade.FIRST)
                .phoneNumber("01000000000")
                .build();

        when(neisApiClient.getTimetableDate("B10", "7010000", "2026", "1", "1", "3", "20260824"))
                .thenReturn(List.of());
        when(neisApiClient.getTimetableDate("B10", "7010000", "2026", "2", "1", "3", "20260824"))
                .thenReturn(List.of(Map.of(
                        "ALL_TI_YMD", "20260824",
                        "PERIO", "1",
                        "ITRT_CNTNT", "Korean"
                )));

        TimetableDTO.WeekResponse response = neisTimetableService.getTimetable(user, "3\uBC18", "20260824", "20260824");

        assertThat(response.isNeisAvailable()).isTrue();
        assertThat(response.getClassRoom()).isEqualTo("3");
        assertThat(response.getPeriods()).hasSize(1);
        assertThat(response.getPeriods().getFirst().getDate()).isEqualTo("20260824");
        assertThat(response.getPeriods().getFirst().getDayOfWeek()).isEqualTo(1);
        assertThat(response.getPeriods().getFirst().getPeriod()).isEqualTo(1);
        assertThat(response.getPeriods().getFirst().getSubject()).isEqualTo("Korean");
        verify(neisApiClient).getTimetableDate("B10", "7010000", "2026", "1", "1", "3", "20260824");
        verify(neisApiClient).getTimetableDate("B10", "7010000", "2026", "2", "1", "3", "20260824");
        verifyNoInteractions(neisSchoolSyncService);
    }
}
