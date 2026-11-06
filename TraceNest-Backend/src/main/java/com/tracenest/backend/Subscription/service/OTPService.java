package com.tracenest.backend.Subscription.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OTPService {

    // Store OTP in memory mapping mobileNumber -> OTP
    // In production, this should be Redis with a TTL
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String generateOtp(String mobileNumber) {
        String otp = String.format("%06d", random.nextInt(999999));
        otpStorage.put(mobileNumber, otp);
        return otp;
    }

    public boolean verifyOtp(String mobileNumber, String otp) {
        String storedOtp = otpStorage.get(mobileNumber);
        if (storedOtp != null && storedOtp.equals(otp)) {
            otpStorage.remove(mobileNumber); // Valid once
            return true;
        }
        return false;
    }
    
    public void setVerified(String mobileNumber) {
        otpStorage.put(mobileNumber + "_verified", "true");
    }
    
    public boolean isVerified(String mobileNumber) {
        return "true".equals(otpStorage.get(mobileNumber + "_verified"));
    }
    
    public void clearVerification(String mobileNumber) {
        otpStorage.remove(mobileNumber + "_verified");
    }
}
