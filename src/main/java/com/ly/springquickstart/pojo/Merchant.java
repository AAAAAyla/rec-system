package com.ly.springquickstart.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Merchant {
    private Long id;
    private Long userId;
    private String shopName;
    private String shopDesc;
    private String avatar;
    private String licenseUrl;
    private String contactPhone;
    /** 0=待审核 1=正常 2=拒绝 3=封禁 */
    private Integer status;
    private String rejectReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}