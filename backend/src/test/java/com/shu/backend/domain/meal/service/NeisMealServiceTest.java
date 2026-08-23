package com.shu.backend.domain.meal.service;

import com.shu.backend.domain.meal.dto.MealDTO;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.school.repository.SchoolRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NeisMealServiceTest {

    @Mock
    private NeisApiClient neisApiClient;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private NeisSchoolSyncService neisSchoolSyncService;

    @InjectMocks
    private NeisMealService neisMealService;

    @Test
    void getMealsRemovesAllergyNumbersAndTrailingAsterisks() {
        School school = School.builder()
                .name("Test High")
                .neisOfficeCode("B10")
                .neisSchoolCode("7010000")
                .build();
        ReflectionTestUtils.setField(school, "id", 1L);

        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(neisApiClient.getMealInfo("B10", "7010000", "20260824", "20260824"))
                .thenReturn(List.of(Map.of(
                        "MLSV_YMD", "20260824",
                        "DDISH_NM", "Rice**<br/>Soup5.6.9.13.*<br/>Chicken(1.5.6.15.)*<br/>Fruit &amp; Yogurt",
                        "CAL_INFO", "650 Kcal"
                )));

        MealDTO.MealListResponse response = neisMealService.getMeals(1L, "20260824", "20260824");

        assertThat(response.isNeisAvailable()).isTrue();
        assertThat(response.getMeals()).hasSize(1);
        assertThat(response.getMeals().getFirst().getDishes())
                .containsExactly("Rice", "Soup", "Chicken", "Fruit & Yogurt");
        assertThat(response.getMeals().getFirst().getCalories()).isEqualTo("650 Kcal");
        verifyNoInteractions(neisSchoolSyncService);
    }

    @Test
    void getMealsKeepsNumbersThatArePartOfDishNames() {
        School school = School.builder()
                .name("Test High")
                .neisOfficeCode("B10")
                .neisSchoolCode("7010000")
                .build();
        ReflectionTestUtils.setField(school, "id", 1L);

        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(neisApiClient.getMealInfo("B10", "7010000", "20260825", "20260825"))
                .thenReturn(List.of(Map.of(
                        "MLSV_YMD", "20260825",
                        "DDISH_NM", "3Color Namul<br/>Vitamin500 Jelly<br/>Yogurt(100ml)<br/>Handmade Cutlet(local)",
                        "CAL_INFO", "700 Kcal"
                )));

        MealDTO.MealListResponse response = neisMealService.getMeals(1L, "20260825", "20260825");

        assertThat(response.getMeals().getFirst().getDishes())
                .containsExactly("3Color Namul", "Vitamin500 Jelly", "Yogurt(100ml)", "Handmade Cutlet(local)");
    }
}
