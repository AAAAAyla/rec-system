package com.ly.springquickstart.service;

import com.ly.springquickstart.mapper.AddressMapper;
import com.ly.springquickstart.pojo.Address;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AddressService {

    @Autowired
    private AddressMapper addressMapper;

    public List<Address> list(Long userId) {
        return addressMapper.findByUser(userId);
    }

    @Transactional
    public void add(Long userId, Address address) {
        address.setUserId(userId);
        // 如果是第一条地址，自动设为默认
        List<Address> existing = addressMapper.findByUser(userId);
        if (existing.isEmpty()) address.setIsDefault(1);

        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            addressMapper.clearDefault(userId);
        }
        addressMapper.insert(address);
    }

    @Transactional
    public void update(Long userId, Address address) {
        address.setUserId(userId);
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            addressMapper.clearDefault(userId);
        }
        addressMapper.update(address);
    }

    public void delete(Long userId, Long id) {
        addressMapper.delete(id, userId);
    }

    @Transactional
    public void setDefault(Long userId, Long id) {
        addressMapper.clearDefault(userId);
        addressMapper.setDefault(id);
    }

    public Address getById(Long id) {
        return addressMapper.findById(id);
    }

    public Address getDefault(Long userId) {
        return addressMapper.findDefault(userId);
    }
}
