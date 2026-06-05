package com.shiwei.seckill.address.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiwei.seckill.address.model.AddressRecord;
import com.shiwei.seckill.address.model.AddressSaveReq;
import com.shiwei.seckill.address.service.AddressService;
import com.shiwei.seckill.common.exception.BizException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AddressServiceImpl implements AddressService {
    private static final TypeReference<List<AddressRecord>> ADDRESS_LIST_TYPE = new TypeReference<List<AddressRecord>>() {};

    private final CopyOnWriteArrayList<AddressRecord> records = new CopyOnWriteArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1L);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path storagePath = Paths.get("data", "addresses.json");

    @PostConstruct
    public void initDefaults() {
        loadFromFile();
        if (!records.isEmpty()) {
            syncIdGenerator();
            return;
        }

        createDefault("张三", "13812345678", "北京市朝阳区光华路1号", true);
        createDefault("李四", "13900001234", "杭州市西湖区曙光路79号", false);
        persist();
    }

    @Override
    public List<AddressRecord> list() {
        List<AddressRecord> result = new ArrayList<>(records);
        result.sort(Comparator.comparing(AddressRecord::getIsDefault).reversed().thenComparing(AddressRecord::getAddressId));
        return result;
    }

    @Override
    public AddressRecord detail(Long addressId) {
        return records.stream().filter(item -> item.getAddressId().equals(addressId)).findFirst().orElse(null);
    }

    @Override
    public synchronized AddressRecord save(AddressSaveReq req) {
        validate(req);

        AddressRecord record = req.getAddressId() == null ? new AddressRecord() : detail(req.getAddressId());
        if (record == null) {
            throw new BizException("收货地址不存在");
        }

        boolean shouldDefault = Boolean.TRUE.equals(req.getIsDefault()) || records.isEmpty();
        if (req.getAddressId() == null) {
            record.setAddressId(idGenerator.getAndIncrement());
            records.add(record);
        }

        record.setConsignee(req.getConsignee().trim());
        record.setMobile(req.getMobile().trim());
        record.setAddress(req.getAddress().trim());
        record.setIsDefault(shouldDefault);

        if (shouldDefault) {
            markDefault(record.getAddressId());
        } else if (!records.stream().anyMatch(AddressRecord::getIsDefault)) {
            record.setIsDefault(true);
        }

        persist();
        return record;
    }

    @Override
    public synchronized void delete(Long addressId) {
        AddressRecord target = detail(addressId);
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
        record.setAddressId(idGenerator.getAndIncrement());
        record.setConsignee(consignee);
        record.setMobile(mobile);
        record.setAddress(address);
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
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), list());
        } catch (IOException e) {
            throw new BizException("收货地址保存失败");
        }
    }

    private void syncIdGenerator() {
        long maxId = records.stream().map(AddressRecord::getAddressId).filter(id -> id != null).max(Long::compareTo).orElse(0L);
        idGenerator.set(maxId + 1);
    }
}
