package com.ly.springquickstart.mapper;

import com.ly.springquickstart.pojo.Merchant;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MerchantMapper {

    @Select("SELECT * FROM merchants WHERE user_id = #{userId}")
    Merchant findByUserId(Long userId);

    @Select("SELECT * FROM merchants WHERE id = #{id}")
    Merchant findById(Long id);

    @Select("SELECT * FROM merchants WHERE status = #{status} ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Merchant> findByStatus(@Param("status") int status,
                                @Param("offset") int offset,
                                @Param("size") int size);

    @Select("SELECT COUNT(*) FROM merchants WHERE status = #{status}")
    int countByStatus(int status);

    @Insert("""
        INSERT INTO merchants(user_id, shop_name, shop_desc, avatar, license_url, contact_phone, status)
        VALUES(#{userId}, #{shopName}, #{shopDesc}, #{avatar}, #{licenseUrl}, #{contactPhone}, 0)
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Merchant merchant);

    @Update("""
        UPDATE merchants SET shop_name=#{shopName}, shop_desc=#{shopDesc},
        avatar=#{avatar}, contact_phone=#{contactPhone}, update_time=NOW()
        WHERE id=#{id}
        """)
    void update(Merchant merchant);

    /** 管理员审核 */
    @Update("UPDATE merchants SET status=#{status}, reject_reason=#{rejectReason}, update_time=NOW() WHERE id=#{id}")
    void updateStatus(@Param("id") Long id,
                      @Param("status") int status,
                      @Param("rejectReason") String rejectReason);

    /** 同步更新 users.role = 1（商家） */
    @Update("UPDATE users SET role=1 WHERE id=#{userId}")
    void grantMerchantRole(Long userId);
}