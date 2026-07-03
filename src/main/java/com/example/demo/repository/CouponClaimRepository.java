package com.example.demo.repository;

import com.example.demo.model.CouponClaim;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponClaimRepository extends JpaRepository<CouponClaim, Long> {
    boolean existsByUserAndCouponCode(User user, String couponCode);
}
