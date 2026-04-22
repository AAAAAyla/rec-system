package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Merchant;
import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.service.MerchantService;
import com.ly.springquickstart.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/merchant")
public class MerchantController {

    @Autowired
    private MerchantService merchantService;

    /** 提交入驻申请（任意已登录用户） */
    @PostMapping("/apply")
    public Result apply(@RequestBody Merchant merchant) {
        Long userId = getCurrentUserId();
        merchantService.apply(userId, merchant);
        return Result.success("申请已提交，等待审核");
    }

    /** 查询自己的商家信息 */
    @GetMapping("/me")
    public Result myInfo() {
        Long userId = getCurrentUserId();
        return Result.success(merchantService.getMyInfo(userId));
    }

    /** 更新店铺信息 */
    @PutMapping("/shop")
    public Result updateShop(@RequestBody Merchant merchant) {
        Long userId = getCurrentUserId();
        merchantService.updateShop(userId, merchant);
        return Result.success();
    }

    /**
     * 管理员查看商家申请列表
     * status: 0=待审核 1=正常 2=拒绝 3=封禁
     */
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "0") int status,
                       @RequestParam(defaultValue = "1")  int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(merchantService.listByStatus(status, pageNum, pageSize));
    }

    /**
     * 管理员审核接口
     * body: { "status": 1, "rejectReason": "" }
     */
    @PutMapping("/audit/{merchantId}")
    public Result audit(@PathVariable Long merchantId,
                        @RequestBody Map<String, Object> body) {
        int status = (int) body.get("status");
        String reason = (String) body.getOrDefault("rejectReason", "");
        merchantService.audit(merchantId, status, reason);
        return Result.success();
    }

    // ── 工具方法 ──────────────────────────────────────
    private Long getCurrentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Long.parseLong(claims.get("id").toString());
    }
}
