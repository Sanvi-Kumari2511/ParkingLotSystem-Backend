package com.parkease.notification.controller;

import com.parkease.notification.dto.request.BroadcastRequest;
import com.parkease.notification.dto.request.SendNotificationRequest;
import com.parkease.notification.dto.response.ApiResponse;
import com.parkease.notification.dto.response.NotificationResponseDTO;
import com.parkease.notification.entity.NotificationChannel;
import com.parkease.notification.entity.NotificationType;
import com.parkease.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService service;

    @Mock
    private Authentication auth;

    @InjectMocks
    private NotificationController controller;

    private NotificationResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        responseDTO = new NotificationResponseDTO();
    }

    // ── POST /api/notifications/send ─────────────────────────────────────────

    @Test
    void shouldSendNotificationAndReturn201() {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientEmail("sanvi@test.com");
        req.setType(NotificationType.BOOKING_CONFIRMED);
        req.setChannel(NotificationChannel.IN_APP);
        req.setTitle("Booking Confirmed");
        req.setMessage("Your booking is confirmed");

        when(service.send(req)).thenReturn(responseDTO);

        var response = controller.send(req);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).send(req);
    }

    // ── GET /api/notifications/my ────────────────────────────────────────────

    @Test
    void shouldReturnMyNotifications() {
        when(auth.getPrincipal()).thenReturn("sanvi@test.com");
        when(service.getMyNotifications("sanvi@test.com"))
                .thenReturn(List.of(responseDTO));

        var response = controller.getMyNotifications(auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(service).getMyNotifications("sanvi@test.com");
    }

    // ── GET /api/notifications/my/unread ─────────────────────────────────────

    @Test
    void shouldReturnUnreadNotifications() {
        when(auth.getPrincipal()).thenReturn("sanvi@test.com");
        when(service.getUnread("sanvi@test.com"))
                .thenReturn(List.of(responseDTO));

        var response = controller.getUnread(auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(service).getUnread("sanvi@test.com");
    }

    // ── GET /api/notifications/my/count ──────────────────────────────────────

    @Test
    void shouldReturnUnreadCount() {
        when(auth.getPrincipal()).thenReturn("sanvi@test.com");
        when(service.getUnreadCount("sanvi@test.com")).thenReturn(3);

        var response = controller.getUnreadCount(auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Map.of("unreadCount", 3), response.getBody());
        verify(service).getUnreadCount("sanvi@test.com");
    }

    @Test
    void shouldReturnZeroUnreadCount() {
        when(auth.getPrincipal()).thenReturn("sanvi@test.com");
        when(service.getUnreadCount("sanvi@test.com")).thenReturn(0);

        var response = controller.getUnreadCount(auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(0, response.getBody().get("unreadCount"));
    }

    // ── PUT /api/notifications/{id}/read ─────────────────────────────────────

    @Test
    void shouldMarkNotificationAsRead() {
        when(auth.getPrincipal()).thenReturn("sanvi@test.com");
        when(service.markAsRead(1L, "sanvi@test.com")).thenReturn(responseDTO);

        var response = controller.markAsRead(1L, auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).markAsRead(1L, "sanvi@test.com");
    }

    // ── PUT /api/notifications/read-all ──────────────────────────────────────

    @Test
    void shouldMarkAllAsRead() {
        when(auth.getPrincipal()).thenReturn("sanvi@test.com");
        when(service.markAllAsRead("sanvi@test.com")).thenReturn(5);

        var response = controller.markAllAsRead(auth);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("5"));
        verify(service).markAllAsRead("sanvi@test.com");
    }

    // ── DELETE /api/notifications/{id} ───────────────────────────────────────

    @Test
    void shouldDeleteNotification() {
        when(auth.getPrincipal()).thenReturn("sanvi@test.com");
        doNothing().when(service).deleteNotification(1L, "sanvi@test.com");

        var response = controller.delete(1L, auth);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        verify(service).deleteNotification(1L, "sanvi@test.com");
    }

    // ── POST /api/notifications/broadcast ────────────────────────────────────

    @Test
    void shouldBroadcastToRecipients() {
        BroadcastRequest req = new BroadcastRequest();
        req.setTitle("System Notice");
        req.setMessage("Maintenance at midnight");

        List<String> emails = List.of("a@test.com", "b@test.com", "c@test.com");
        when(service.broadcast(req, emails)).thenReturn(3);

        var response = controller.broadcast(req, emails);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().getMessage().contains("3"));
        verify(service).broadcast(req, emails);
    }
}
