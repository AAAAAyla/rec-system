package com.ly.springquickstart.mapper;

import com.ly.springquickstart.pojo.Shipment;
import com.ly.springquickstart.pojo.ShipmentTrack;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ShipmentMapper {

    @Insert("""
        INSERT INTO shipments(order_id, express_company, tracking_no, ship_time, status)
        VALUES(#{orderId}, #{expressCompany}, #{trackingNo}, NOW(), 0)
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Shipment shipment);

    @Select("SELECT * FROM shipments WHERE order_id=#{orderId}")
    Shipment findByOrderId(Long orderId);

    @Insert("""
        INSERT INTO shipment_tracks(shipment_id, location, description, track_time)
        VALUES(#{shipmentId}, #{location}, #{description}, #{trackTime})
        """)
    void insertTrack(ShipmentTrack track);

    @Select("SELECT * FROM shipment_tracks WHERE shipment_id=#{shipmentId} ORDER BY track_time DESC")
    List<ShipmentTrack> findTracks(Long shipmentId);

    @Update("UPDATE shipments SET status=1 WHERE id=#{id}")
    void markReceived(Long id);
}
