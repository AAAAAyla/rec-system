package com.ly.springquickstart.service;

import com.ly.springquickstart.mapper.MerchantMapper;
import com.ly.springquickstart.pojo.Merchant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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

    /** 获取当前商家信息（未申请则返回 null） */
    public Merchant getMyInfo(Long userId) {
        return merchantMapper.findByUserId(userId);
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

    /** 管理员查看商家列表（按状态过滤） */
    public Map<String, Object> listByStatus(int status, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Merchant> rows = merchantMapper.findByStatus(status, offset, pageSize);
        int total = merchantMapper.countByStatus(status);
        return Map.of("rows", rows, "total", total);
    }

    /** 根据 userId 获取 merchantId（controller 常用） */
    public Long getMerchantIdByUser(Long userId) {
        Merchant m = merchantMapper.findByUserId(userId);
        if (m != null && m.getStatus() == 1) {
            return m.getId();
        }
        // 管理员可能没有 status=1 的记录但仍有 merchants 行
        if (m != null) {
            return m.getId();
        }
        throw new RuntimeException("您不是认证商家或审核未通过");
    }
}
