// src/main/java/com/ly/springquickstart/pojo/ShipmentTrack.java
package com.ly.springquickstart.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ShipmentTrack {
    private Long id;
    private Long shipmentId;
    private String location;
    private String description;
    private LocalDateTime trackTime;
}