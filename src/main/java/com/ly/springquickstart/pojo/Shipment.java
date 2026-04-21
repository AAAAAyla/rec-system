// src/main/java/com/ly/springquickstart/pojo/Shipment.java
package com.ly.springquickstart.pojo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Shipment {
    private Long id;
    private Long orderId;
    private String expressCompany;
    private String trackingNo;
    private LocalDateTime shipTime;
    /** 0=运输中 1=已签收 */
    private Integer status;

    // 非数据库列，查询后手动组装
    private List<ShipmentTrack> tracks;
}