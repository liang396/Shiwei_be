package com.shiwei.seckill.profile.controller;

import com.shiwei.seckill.address.service.AddressService;
import com.shiwei.seckill.auth.model.UserAccount;
import com.shiwei.seckill.common.api.ApiResponse;
import com.shiwei.seckill.common.exception.SentinelBlockedException;
import com.shiwei.seckill.common.sentinel.SentinelSupport;
import com.shiwei.seckill.order.service.OrderService;
import com.shiwei.seckill.profile.service.UserProfileService;
import com.shiwei.seckill.promotion.service.CouponService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {
    @Mock
    private OrderService orderService;
    @Mock
    private AddressService addressService;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private CouponService couponService;
    @Mock
    private SentinelSupport sentinelSupport;

    @InjectMocks
    private ProfileController profileController;

    @Test
    void shouldReturnFallbackOverviewWhenSentinelBlocks() {
        when(sentinelSupport.enter("profile.overview")).thenThrow(new SentinelBlockedException("blocked"));
        UserAccount user = new UserAccount();
        user.setUserId(1L);
        user.setUsername("demo");
        user.setNickname("demo");
        when(userProfileService.currentProfile()).thenReturn(user);

        ApiResponse<?> response = profileController.overview();

        Map<?, ?> data = (Map<?, ?>) response.getData();
        Map<?, ?> stats = (Map<?, ?>) data.get("orderStats");
        assertEquals(0, stats.get("total"));
        assertEquals(Collections.emptyList(), data.get("recentOrders"));
    }
}
