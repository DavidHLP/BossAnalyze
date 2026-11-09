package com.david.hlp.spark.service.Log;

import com.david.hlp.commons.entity.logs.IpStatistics;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GeoStatsTest {
    @Test void countsCountriesIndependentlyOfCoordinatesAndUnknownsRemainInDenominator() {
        var stats = IpStatistics.builder().ipStats(Map.of(
                "synthetic-a", IpStatistics.IpInfo.builder().count(3).countryName("Testland").build(),
                "synthetic-b", IpStatistics.IpInfo.builder().count(2).countryName("Testland").latitude(12.0).longitude(34.0).build(),
                "synthetic-d", IpStatistics.IpInfo.builder().count(2).countryName("Testland").latitude(12.0).longitude(34.0).build(),
                "synthetic-c", IpStatistics.IpInfo.builder().count(3).countryName("").build())).build();
        var result = LogServiceImp.buildGeoStats(stats);
        var country = (Map<?, ?>) ((Map<?, ?>) result.get("countryStats")).get("Testland");
        assertEquals(7L, country.get("visits"));
        assertEquals(3L, country.get("uniqueVisitors"));
        assertEquals(70.0, country.get("percentage"));
        var points = (List<?>) result.get("geoPoints");
        assertEquals(1, points.size());
        assertEquals(4L, ((Map<?, ?>) points.get(0)).get("value"));
        assertEquals(Map.of("countryStats", Map.of(), "geoPoints", List.of()), LogServiceImp.buildGeoStats(null));
    }
}
