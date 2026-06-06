package com.shiwei.seckill.address.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiwei.seckill.address.model.AddressRecord;
import com.shiwei.seckill.address.model.AddressSaveReq;
import com.shiwei.seckill.address.service.AddressService;
import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.common.id.SnowflakeIdGenerator;
import com.shiwei.seckill.common.security.AesSecurityUtil;
import com.shiwei.seckill.common.security.DesensitizeUtil;
import com.shiwei.seckill.common.security.RequestRateLimitService;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AddressServiceImpl implements AddressService {
    private static final TypeReference<List<AddressRecord>> ADDRESS_LIST_TYPE = new TypeReference<List<AddressRecord>>() {};

    private final CopyOnWriteArrayList<AddressRecord> records = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path storagePath = Paths.get("data", "addresses.json");

    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Resource
    private AesSecurityUtil aesSecurityUtil;
    @Resource
    private RequestRateLimitService requestRateLimitService;

    @PostConstruct
    public void initDefaults() {
        loadFromFile();
        if (!records.isEmpty()) {
            return;
        }

        createDefault("张三", "13812345678", "北京市朝阳区光华路 8 号", true);
        createDefault("李四", "13900001234", "杭州市西湖区曙光路 99 号", false);
        persist();
    }

    @Override
    public List<AddressRecord> list() {
        List<AddressRecord> result = new ArrayList<>();
        for (AddressRecord item : records) {
            result.add(maskCopy(item));
        }
        result.sort(Comparator.comparing(AddressRecord::getIsDefault).reversed().thenComparing(AddressRecord::getAddressId));
        return result;
    }

    @Override
    public AddressRecord detail(Long addressId) {
        AddressRecord record = records.stream().filter(item -> item.getAddressId().equals(addressId)).findFirst().orElse(null);
        return record == null ? null : maskCopy(record);
    }

    @Override
    public synchronized AddressRecord save(AddressSaveReq req) {
        requestRateLimitService.guard("rate:address:save:1", 20);
        validate(req);

        AddressRecord record = req.getAddressId() == null ? new AddressRecord() : records.stream()
            .filter(item -> item.getAddressId().equals(req.getAddressId()))
            .findFirst()
            .orElse(null);
        if (record == null) {
            throw new BizException("收货地址不存在");
        }

        boolean shouldDefault = Boolean.TRUE.equals(req.getIsDefault()) || records.isEmpty();
        if (req.getAddressId() == null) {
            record.setAddressId(snowflakeIdGenerator.nextId());
            records.add(record);
        }

        record.setConsignee(req.getConsignee().trim());
        record.setMobile(aesSecurityUtil.encrypt(req.getMobile().trim()));
        record.setAddress(aesSecurityUtil.encrypt(req.getAddress().trim()));
        record.setIsDefault(shouldDefault);

        if (shouldDefault) {
            markDefault(record.getAddressId());
        } else if (!records.stream().anyMatch(AddressRecord::getIsDefault)) {
            record.setIsDefault(true);
        }

        persist();
        return maskCopy(record);
    }

    @Override
    public synchronized void delete(Long addressId) {
        AddressRecord target = records.stream().filter(item -> item.getAddressId().equals(addressId)).findFirst().orElse(null);
        if (target == null) {
            throw new BizException("收货地址不存在");
        }

        boolean wasDefault = Boolean.TRUE.equals(target.getIsDefault());
        records.removeIf(item -> item.getAddressId().equals(addressId));
        if (wasDefault && !records.isEmpty()) {
            markDefault(records.get(0).getAddressId());
        }
        persist();
    }

    private void createDefault(String consignee, String mobile, String address, boolean isDefault) {
        AddressRecord record = new AddressRecord();
        record.setAddressId(snowflakeIdGenerator.nextId());
        record.setConsignee(consignee);
        record.setMobile(aesSecurityUtil.encrypt(mobile));
        record.setAddress(aesSecurityUtil.encrypt(address));
        record.setIsDefault(isDefault);
        records.add(record);
    }

    private void markDefault(Long addressId) {
        for (AddressRecord item : records) {
            item.setIsDefault(item.getAddressId().equals(addressId));
        }
    }

    private void validate(AddressSaveReq req) {
        if (req.getConsignee() == null || req.getConsignee().trim().isEmpty()) {
            throw new BizException("请填写收货人");
        }
        if (req.getMobile() == null || req.getMobile().trim().isEmpty()) {
            throw new BizException("请填写联系电话");
        }
        if (req.getAddress() == null || req.getAddress().trim().isEmpty()) {
            throw new BizException("请填写详细地址");
        }
    }

    private void loadFromFile() {
        if (!Files.exists(storagePath)) {
            return;
        }
        try {
            List<AddressRecord> loaded = objectMapper.readValue(storagePath.toFile(), ADDRESS_LIST_TYPE);
            records.clear();
            records.addAll(loaded);
        } catch (IOException e) {
            throw new BizException("收货地址数据加载失败");
        }
    }

    private void persist() {
        try {
            Files.createDirectories(storagePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), records);
        } catch (IOException e) {
            throw new BizException("收货地址保存失败");
        }
    }

    private AddressRecord maskCopy(AddressRecord source) {
        String mobileRaw = aesSecurityUtil.decryptOrRaw(source.getMobile());
        String addressRaw = aesSecurityUtil.decryptOrRaw(source.getAddress());
        AddressRecord copy = new AddressRecord();
        copy.setAddressId(source.getAddressId());
        copy.setConsignee(source.getConsignee());
        copy.setMobile(DesensitizeUtil.mobile(mobileRaw));
        copy.setAddress(DesensitizeUtil.address(addressRaw));
        copy.setMobileRaw(mobileRaw);
        copy.setAddressRaw(addressRaw);
        copy.setIsDefault(source.getIsDefault());
        return copy;
    }
}

