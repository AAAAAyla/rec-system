package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Address;
import com.ly.springquickstart.pojo.Result;
import com.ly.springquickstart.service.AddressService;
import com.ly.springquickstart.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @GetMapping
    public Result list() {
        return Result.success(addressService.list(uid()));
    }

    @PostMapping
    public Result add(@RequestBody Address address) {
        addressService.add(uid(), address);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody Address address) {
        address.setId(id);
        addressService.update(uid(), address);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        addressService.delete(uid(), id);
        return Result.success();
    }

    /** 设为默认地址 */
    @PutMapping("/{id}/default")
    public Result setDefault(@PathVariable Long id) {
        addressService.setDefault(uid(), id);
        return Result.success();
    }

    private Long uid() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Long.parseLong(claims.get("id").toString());
    }
}
