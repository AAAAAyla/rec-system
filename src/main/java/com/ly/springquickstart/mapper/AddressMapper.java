package com.ly.springquickstart.mapper;

import com.ly.springquickstart.pojo.Address;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AddressMapper {

    @Select("SELECT * FROM addresses WHERE user_id = #{userId} ORDER BY is_default DESC, id DESC")
    List<Address> findByUser(Long userId);

    @Select("SELECT * FROM addresses WHERE id = #{id}")
    Address findById(Long id);

    @Select("SELECT * FROM addresses WHERE user_id = #{userId} AND is_default = 1 LIMIT 1")
    Address findDefault(Long userId);

    @Insert("""
        INSERT INTO addresses(user_id, name, phone, province, city, district, detail, is_default)
        VALUES(#{userId}, #{name}, #{phone}, #{province}, #{city}, #{district}, #{detail}, #{isDefault})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Address address);

    @Update("""
        UPDATE addresses SET name=#{name}, phone=#{phone},
        province=#{province}, city=#{city}, district=#{district},
        detail=#{detail}, is_default=#{isDefault}
        WHERE id=#{id} AND user_id=#{userId}
        """)
    void update(Address address);

    @Delete("DELETE FROM addresses WHERE id=#{id} AND user_id=#{userId}")
    void delete(@Param("id") Long id, @Param("userId") Long userId);

    /** 先把该用户所有地址 is_default 清 0，再设指定地址为默认 */
    @Update("UPDATE addresses SET is_default=0 WHERE user_id=#{userId}")
    void clearDefault(Long userId);

    @Update("UPDATE addresses SET is_default=1 WHERE id=#{id}")
    void setDefault(Long id);
}
