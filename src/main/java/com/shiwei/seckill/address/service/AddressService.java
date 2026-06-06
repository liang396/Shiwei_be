package com.shiwei.seckill.address.service;

import com.shiwei.seckill.address.model.AddressRecord;
import com.shiwei.seckill.address.model.AddressSaveReq;

import java.util.List;

public interface AddressService {
    List<AddressRecord> list();

    AddressRecord detail(Long addressId);

    AddressRecord save(AddressSaveReq req);

    void delete(Long addressId);
}

