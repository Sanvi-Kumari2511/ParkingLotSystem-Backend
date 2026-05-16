package com.parkease.payment.controller;

import com.parkease.payment.dto.request.CreateOrderRequest;

import com.parkease.payment.dto.request.VerifyPaymentRequest;
import com.parkease.payment.dto.response.ApiResponse;
import com.parkease.payment.dto.response.OrderResponseDTO;
import com.parkease.payment.dto.response.PaymentResponseDTO;
import com.parkease.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService service;

    @Mock
    private Authentication auth;

    @InjectMocks
    private PaymentController controller;

    private PaymentResponseDTO responseDTO;
    private OrderResponseDTO orderResponseDTO;

    @BeforeEach
    void setUp() {
        responseDTO = new PaymentResponseDTO();
        orderResponseDTO = OrderResponseDTO.builder()
                .paymentId(1L)
                .razorpayOrderId("order_123")
                .amount(150.0)
                .currency("INR")
                .status("created")
                .razorpayKeyId("rzp_test_key")
                .build();
    }

    // ── POST /api/payments/order ──────────────────────────────────────────────

    @Test
    void shouldCreateOrderAndReturn201() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setBookingId(100L);
        req.setAmount(150.0);
        req.setDescription("Parking fee");

        when(auth.getPrincipal()).thenReturn("sanvi@test.com");
        when(service.createOrder(req, "sanvi@test.com")).thenReturn(orderResponseDTO);

        var response = controller.createOrder(req, auth);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(orderResponseDTO, response.getBody());
        verify(service).createOrder(req, "sanvi@test.com");
    }

    // ── POST /api/payments/verify ─────────────────────────────────────────────

    @Test
    void shouldVerifyPayment() {
        VerifyPaymentRequest req = new VerifyPaymentRequest();
        req.setRazorpayOrderId("order_123");
        req.setRazorpayPaymentId("pay_abc");
        req.setRazorpaySignature("valid_sig");

        when(service.verifyPayment(req)).thenReturn(responseDTO);

        var response = controller.verifyPayment(req);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).verifyPayment(req);
    }

    // ── GET /api/payments/my ──────────────────────────────────────────────────

    @Test
    void shouldGetMyPayments() {
        when(auth.getPrincipal()).thenReturn("sanvi@test.com");
        when(service.getMyPayments("sanvi@test.com")).thenReturn(List.of(responseDTO));

        var response = controller.getMyPayments(auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        verify(service).getMyPayments("sanvi@test.com");
    }

    // ── GET /api/payments/booking/{bookingId} ─────────────────────────────────

    @Test
    void shouldGetPaymentByBooking() {
        when(service.getPaymentByBookingId(100L)).thenReturn(responseDTO);

        var response = controller.getByBooking(100L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).getPaymentByBookingId(100L);
    }

    // ── GET /api/payments/{paymentId} ─────────────────────────────────────────

    @Test
    void shouldGetPaymentById() {
        when(service.getPaymentById(1L)).thenReturn(responseDTO);

        var response = controller.getById(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(responseDTO, response.getBody());
        verify(service).getPaymentById(1L);
    }

    // ── GET /api/payments/{paymentId}/receipt — file exists ───────────────────

    @Test
    void shouldDownloadReceiptWhenFileExists() throws IOException {
        when(auth.getPrincipal()).thenReturn("sanvi@test.com");

        // Create a real temp file so File.exists() returns true
        File tempFile = File.createTempFile("receipt_test", ".pdf");
        tempFile.deleteOnExit();

        when(service.getReceiptPath(1L, "sanvi@test.com")).thenReturn(tempFile.getAbsolutePath());

        var response = controller.downloadReceipt(1L, auth);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        verify(service).getReceiptPath(1L, "sanvi@test.com");
    }

    // ── GET /api/payments/{paymentId}/receipt — file missing ──────────────────

    @Test
    void shouldReturn404WhenReceiptFileMissing() {
        when(auth.getPrincipal()).thenReturn("sanvi@test.com");
        when(service.getReceiptPath(1L, "sanvi@test.com"))
                .thenReturn("/non/existent/path/receipt.pdf");

        var response = controller.downloadReceipt(1L, auth);

        assertEquals(404, response.getStatusCode().value());
    }

    // ── GET /api/payments/admin/all ───────────────────────────────────────────

    @Test
    void shouldGetAllPayments() {
        when(service.getAllPayments()).thenReturn(List.of(responseDTO, responseDTO));

        var response = controller.getAllPayments();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, response.getBody().size());
        verify(service).getAllPayments();
    }

    // ── POST /api/payments/webhook ────────────────────────────────────────────

    @Test
    void shouldHandleWebhook() {
        var response = controller.handleWebhook("{\"event\":\"payment.captured\"}", "rzp_sig");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
    }
}
