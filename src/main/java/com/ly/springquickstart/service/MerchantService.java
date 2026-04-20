package com.ly.springquickstart.service;

import com.ly.springquickstart.mapper.MerchantMapper;
import com.ly.springquickstart.pojo.Merchant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantService {

    @Autowired
    private MerchantMapper merchantMapper;

    /** 买家申请成为商家 */
    @Transactional
    public void apply(Long userId, Merchant merchant) {
        Merchant existing = merchantMapper.findByUserId(userId);
        if (existing != null) {
            throw new RuntimeException("您已提交过申请，请勿重复提交");
        }
        merchant.setUserId(userId);
        merchantMapper.insert(merchant);
    }

    /** 获取当前商家信息 */
    public Merchant getMyInfo(Long userId) {
        Merchant m = merchantMapper.findByUserId(userId);
        if (m == null) throw new RuntimeException("商家信息不存在");
        return m;
    }

    /** 更新店铺信息 */
    public void updateShop(Long userId, Merchant updated) {
        Merchant m = merchantMapper.findByUserId(userId);
        if (m == null) throw new RuntimeException("商家信息不存在");
        updated.setId(m.getId());
        merchantMapper.update(updated);
    }

    /** 管理员审核（status: 1=通过 2=拒绝） */
    @Transactional
    public void audit(Long merchantId, int status, String rejectReason) {
        Merchant m = merchantMapper.findById(merchantId);
        if (m == null) throw new RuntimeException("商家不存在");
        merchantMapper.updateStatus(merchantId, status, rejectReason);
        if (status == 1) {
            // 通过审核：升级 users.role 为商家
            merchantMapper.grantMerchantRole(m.getUserId());
        }
    }

    /** 根据 userId 获取 merchantId（controller 常用） */
    public Long getMerchantIdByUser(Long userId) {
        Merchant m = merchantMapper.findByUserId(userId);
        if (m == null || m.getStatus() != 1) {
            throw new RuntimeException("您不是认证商家或审核未通过");
        }
        return m.getId();
    }
}
