/**
 * @file subscription.service.ts
 * @description Client service managing public community alert registrations with SMS OTP verification.
 */

import { api } from "./api";

/**
 * Payload data for creating a new public alert subscription.
 */
export interface SubscriptionData {
  fullName: string;
  email: string;
  mobileNumber?: string;
  address?: string;
  city?: string;
  lat?: number;
  lng?: number;
}

/**
 * Dispatches a 6-digit one-time password to the target mobile number.
 *
 * @param mobileNumber Phone number with country code.
 * @returns Promise with API response message.
 */
export const sendOtp = async (mobileNumber: string) => {
  const res = await api.post("/subscribe/send-otp", { mobileNumber });
  return res.data;
};

/**
 * Verifies the 6-digit OTP code against the server cache.
 *
 * @param mobileNumber Target phone number.
 * @param otp 6-digit code string.
 * @returns Promise with verification confirmation.
 */
export const verifyOtp = async (mobileNumber: string, otp: string) => {
  const res = await api.post("/subscribe/verify-otp", { mobileNumber, otp });
  return res.data;
};

/**
 * Registers an anonymous alert subscription with geographic coordinates.
 *
 * @param data Verified subscription payload.
 * @returns Promise with subscription confirmation.
 */
export const subscribeForAlerts = async (data: SubscriptionData) => {
  const res = await api.post("/subscribe", data);
  return res.data;
};
