package com.ly.springquickstart.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface ImMapper {

    @Select("""
        SELECT s.*, 
          CASE WHEN #{userType}='user' THEN m.shop_name ELSE u.username END AS chat_name
        FROM im_sessions s
        LEFT JOIN merchants m ON s.merchant_id = m.id
        LEFT JOIN users u ON s.user_id = u.id
        WHERE CASE WHEN #{userType}='user' THEN s.user_id=#{userId} ELSE s.merchant_id=#{merchantId} END
        ORDER BY s.updated_at DESC
        """)
    List<Map<String, Object>> findSessions(@Param("userType") String userType,
                                            @Param("userId") Long userId,
                                            @Param("merchantId") Long merchantId);

    @Select("SELECT * FROM im_sessions WHERE user_id=#{userId} AND merchant_id=#{merchantId}")
    Map<String, Object> findSession(@Param("userId") Long userId, @Param("merchantId") Long merchantId);

    @Select("SELECT * FROM im_sessions WHERE id=#{id}")
    Map<String, Object> findSessionById(Long id);

    @Insert("INSERT INTO im_sessions(user_id, merchant_id, last_message, unread_user, unread_merchant) VALUES(#{userId},#{merchantId},'',0,0)")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insertSession(Map<String, Object> session);

    @Update("UPDATE im_sessions SET last_message=#{content}, updated_at=NOW(), " +
            "unread_user = CASE WHEN #{senderType}='merchant' THEN unread_user+1 ELSE unread_user END, " +
            "unread_merchant = CASE WHEN #{senderType}='user' THEN unread_merchant+1 ELSE unread_merchant END " +
            "WHERE id=#{sessionId}")
    void updateSessionLastMessage(@Param("sessionId") Long sessionId,
                                   @Param("content") String content,
                                   @Param("senderType") String senderType);

    @Insert("INSERT INTO im_messages(session_id, sender_type, content, type) VALUES(#{sessionId},#{senderType},#{content},#{type})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insertMessage(Map<String, Object> msg);

    @Select("SELECT * FROM im_messages WHERE session_id=#{sessionId} ORDER BY created_at ASC LIMIT #{offset},#{size}")
    List<Map<String, Object>> findMessages(@Param("sessionId") Long sessionId,
                                            @Param("offset") int offset,
                                            @Param("size") int size);

    @Select("SELECT COUNT(*) FROM im_messages WHERE session_id=#{sessionId}")
    int countMessages(Long sessionId);

    @Update("UPDATE im_sessions SET unread_user=0 WHERE id=#{sessionId}")
    void clearUserUnread(Long sessionId);

    @Update("UPDATE im_sessions SET unread_merchant=0 WHERE id=#{sessionId}")
    void clearMerchantUnread(Long sessionId);
}
