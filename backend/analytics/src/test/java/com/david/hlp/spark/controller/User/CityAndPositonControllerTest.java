package com.david.hlp.spark.controller.User;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.david.hlp.spark.service.User.SalaryHotJob;
import com.david.hlp.spark.service.User.TwoDimensionalAnalysisChart;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CityAndPositonControllerTest {

    private final SalaryHotJob salaryHotJob = mock(SalaryHotJob.class);
    private final TwoDimensionalAnalysisChart chart = mock(TwoDimensionalAnalysisChart.class);
    private final CityAndPositonController controller =
            new CityAndPositonController(salaryHotJob, chart);

    @Test
    void salaryHotJobLimitIsBoundedBeforeReachingSpark() {
        for (Long limit : new Long[] { null, 0L, -1L, 1001L }) {
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> controller.getSalaryHotJob(limit));
            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        }
        verifyNoInteractions(salaryHotJob);
        controller.getSalaryHotJob(1000L);
        verify(salaryHotJob).analyzeSalaryHotJob(1000L);
    }

    @Test
    void arbitraryColumnNamesCannotReachSparkSql() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.getJobAnalysisData("all", "all", "salary_value", "1=1"));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(chart);
        controller.getJobAnalysisData("珠海", "Java", "experience_value", "degree_value");
        verify(chart).getJobAnalysisData("珠海", "Java", "experience_value", "degree_value");
    }
}
