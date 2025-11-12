package com.siseg.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.siseg.dto.geocoding.Coordinates;

public class PolylineDecoder {
    
    private PolylineDecoder() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static List<Coordinates> decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Coordinates> coordinates = new ArrayList<>();
        int index = 0;
        int lat = 0;
        int lon = 0;
        
        while (index < encoded.length()) {
            int b;
            int shift = 0;
            int result = 0;
            
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            
            int dlat = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lat += dlat;
            
            shift = 0;
            result = 0;
            
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            
            int dlon = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
            lon += dlon;
            
            BigDecimal latitude = BigDecimal.valueOf(lat / 1e5);
            BigDecimal longitude = BigDecimal.valueOf(lon / 1e5);
            
            coordinates.add(new Coordinates(latitude, longitude));
        }
        
        return coordinates;
    }
}

