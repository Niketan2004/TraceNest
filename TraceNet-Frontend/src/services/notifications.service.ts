/**
 * @file notifications.service.ts
 * @description Client service for retrieving in-app notifications and updating read statuses.
 */

import { api } from "./api";

/**
 * In-app notification payload model.
 */
export interface AppNotification {
  id: string;
  type: string;
  title: string;
  body: string;
  reportId?: string;
  read: boolean;
  createdAt: string;
}

/**
 * Paginated notification response structure with unread count.
 */
export interface NotificationsResponse {
  items: AppNotification[];
  unreadCount: number;
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

/**
 * Fetches notifications for the current authenticated user.
 *
 * @param unreadOnly Filter only unread messages if true.
 * @param page Zero-indexed page number.
 * @param size Slice size.
 * @returns Promise resolving to {@link NotificationsResponse}.
 */
export async function getNotifications(
  unreadOnly = false,
  page = 0,
  size = 30,
): Promise<NotificationsResponse> {
  const res = await api.get(`/notifications?unreadOnly=${unreadOnly}&page=${page}&size=${size}`);
  return res.data;
}

/**
 * Marks a single notification as read by its identifier.
 *
 * @param id Notification primary key.
 */
export async function markAsRead(id: string): Promise<void> {
  await api.post(`/notifications/${id}/read`);
}

/**
 * Marks all notifications for the authenticated user as read in a single call.
 */
export async function markAllAsRead(): Promise<void> {
  await api.post(`/notifications/read-all`);
}
