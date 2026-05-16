package com.parkease.payment.service;

import com.parkease.payment.dto.request.CreateOrderRequest;

import com.parkease.payment.dto.response.OrderResponseDTO;
import com.parkease.payment.dto.response.PaymentResponseDTO;
import com.parkease.payment.entity.Payment;
import com.parkease.payment.entity.PaymentStatus;
import com.parkease.payment.exception.PaymentException;
import com.parkease.payment.mapper.PaymentMapper;
import com.parkease.payment.messaging.NotificationPublisher;
import com.parkease.payment.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.PaymentClient;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository repo;

    @Mock
    private PaymentMapper mapper;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private OrderClient orderClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private ReceiptService receiptService;

    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private PaymentServiceImpl service;

    private Payment payment;
    private PaymentResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "test_secret");

        // IMPORTANT: mock nested Razorpay SDK clients
        razorpayClient.orders = orderClient;
        razorpayClient.payments = paymentClient;

        payment = Payment.builder()
                .paymentId(1L)
                .bookingId(100L)
                .driverEmail("sanvi@test.com")
                .amount(150.0)
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .razorpayOrderId("order_123")
                .razorpayPaymentId("pay_123")
                .build();

        responseDTO = new PaymentResponseDTO();
    }

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setBookingId(100L);
        req.setAmount(150.0);
        req.setDescription("Parking payment");

        Order mockOrder = mock(Order.class);

        when(repo.findByBookingId(100L)).thenReturn(Optional.empty());
        when(orderClient.create(any(JSONObject.class))).thenReturn(mockOrder);
        when(mockOrder.get("id")).thenReturn("order_123");
        when(repo.save(any(Payment.class))).thenReturn(payment);

        OrderResponseDTO result = service.createOrder(req, "sanvi@test.com");

        assertNotNull(result);
        assertEquals("order_123", result.getRazorpayOrderId());
        verify(repo).save(any(Payment.class));
    }

    @Test
    void shouldThrowWhenPaymentAlreadyPaid() {
        payment.setStatus(PaymentStatus.PAID);

        CreateOrderRequest req = new CreateOrderRequest();
        req.setBookingId(100L);
        req.setAmount(150.0);

        when(repo.findByBookingId(100L)).thenReturn(Optional.of(payment));

        assertThrows(PaymentException.class,
                () -> service.createOrder(req, "sanvi@test.com"));
    }


    @Test
    void shouldThrowExceptionWhenCreateOrderFailsInRazorpay() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setBookingId(100L);
        req.setAmount(150.0);

        when(repo.findByBookingId(100L)).thenReturn(Optional.empty());
        when(orderClient.create(any(JSONObject.class))).thenThrow(new com.razorpay.RazorpayException("API error"));

        assertThrows(PaymentException.class, () -> service.createOrder(req, "sanvi@test.com"));
    }


    @Test
    void shouldVerifyPaymentSuccessfully() throws Exception {
        com.parkease.payment.dto.request.VerifyPaymentRequest req = new com.parkease.payment.dto.request.VerifyPaymentRequest();
        req.setRazorpayOrderId("order_123");
        req.setRazorpayPaymentId("pay_123");
        
        // Compute valid signature
        String payload = "order_123|pay_123";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec("test_secret".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        String validSig = java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        req.setRazorpaySignature(validSig);

        com.razorpay.Payment mockRzpPayment = mock(com.razorpay.Payment.class);

        when(repo.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));
        when(paymentClient.fetch("pay_123")).thenReturn(mockRzpPayment);
        when(mockRzpPayment.get("method")).thenReturn("upi");
        when(repo.save(any(Payment.class))).thenReturn(payment);
        when(receiptService.generateReceipt(payment)).thenReturn("C:/receipt.pdf");
        when(mapper.toDTO(any(Payment.class))).thenReturn(responseDTO);

        PaymentResponseDTO result = service.verifyPayment(req);

        assertNotNull(result);
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(com.parkease.payment.entity.PaymentMode.UPI, payment.getMode());
        verify(repo, times(2)).save(payment);
        verify(notificationPublisher).publish(any());
    }

    @Test
    void shouldMapPaymentMethodsCorrectly() throws Exception {
        // Just verify another method branch in mapPaymentMethod
        com.parkease.payment.dto.request.VerifyPaymentRequest req = new com.parkease.payment.dto.request.VerifyPaymentRequest();
        req.setRazorpayOrderId("order_123");
        req.setRazorpayPaymentId("pay_123");
        
        String payload = "order_123|pay_123";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec("test_secret".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        req.setRazorpaySignature(java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8))));

        com.razorpay.Payment mockRzpPayment = mock(com.razorpay.Payment.class);

        when(repo.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));
        when(paymentClient.fetch("pay_123")).thenReturn(mockRzpPayment);
        when(mockRzpPayment.get("method")).thenReturn("card");
        when(repo.save(any(Payment.class))).thenReturn(payment);
        when(mapper.toDTO(any(Payment.class))).thenReturn(responseDTO);

        service.verifyPayment(req);
        assertEquals(com.parkease.payment.entity.PaymentMode.CARD, payment.getMode());
    }


    @Test
    void shouldRegenerateReceiptWhenMissing() {
        payment.setStatus(PaymentStatus.PAID);
        payment.setReceiptPath(null);

        when(repo.findById(1L)).thenReturn(Optional.of(payment));
        when(receiptService.generateReceipt(payment))
                .thenReturn("C:/temp/new_receipt.pdf");

        String result = service.getReceiptPath(1L, "sanvi@test.com");

        assertEquals("C:/temp/new_receipt.pdf", result);
        verify(repo).save(payment);
    }

    @Test
    void shouldGetReceiptPathSuccessfully() {
        payment.setStatus(PaymentStatus.PAID);
        payment.setReceiptPath(null);

        when(repo.findById(1L)).thenReturn(Optional.of(payment));
        when(receiptService.generateReceipt(payment))
                .thenReturn("C:/temp/receipt.pdf");

        String result = service.getReceiptPath(1L, "sanvi@test.com");

        assertEquals("C:/temp/receipt.pdf", result);
    }

    @Test
    void shouldThrowWhenReceiptRequestedByAnotherDriver() {
        payment.setStatus(PaymentStatus.PAID);

        when(repo.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(RuntimeException.class,
                () -> service.getReceiptPath(1L, "other@test.com"));
    }

    // ── Additional service tests ───────────────────────────────────────────────

    @Test
    void shouldGetPaymentByBookingIdSuccessfully() {
        when(repo.findByBookingId(100L)).thenReturn(Optional.of(payment));
        when(mapper.toDTO(payment)).thenReturn(responseDTO);

        PaymentResponseDTO result = service.getPaymentByBookingId(100L);

        assertNotNull(result);
        verify(mapper).toDTO(payment);
    }

    @Test
    void shouldThrowWhenPaymentNotFoundByBookingId() {
        when(repo.findByBookingId(999L)).thenReturn(Optional.empty());

        assertThrows(com.parkease.payment.exception.ResourceNotFoundException.class,
                () -> service.getPaymentByBookingId(999L));
    }

    @Test
    void shouldGetPaymentByIdSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(payment));
        when(mapper.toDTO(payment)).thenReturn(responseDTO);

        PaymentResponseDTO result = service.getPaymentById(1L);

        assertNotNull(result);
    }

    @Test
    void shouldGetMyPaymentsSuccessfully() {
        when(repo.findByDriverEmailOrderByCreatedAtDesc("sanvi@test.com"))
                .thenReturn(List.of(payment));
        when(mapper.toDTO(payment)).thenReturn(responseDTO);

        java.util.List<PaymentResponseDTO> result = service.getMyPayments("sanvi@test.com");

        assertEquals(1, result.size());
    }

    @Test
    void shouldGetAllPaymentsSuccessfully() {
        when(repo.findAll()).thenReturn(List.of(payment));
        when(mapper.toDTO(payment)).thenReturn(responseDTO);

        java.util.List<PaymentResponseDTO> result = service.getAllPayments();

        assertEquals(1, result.size());
    }

    @Test
    void shouldThrowWhenReceiptRequestedForNonPaidPayment() {
        payment.setStatus(PaymentStatus.PENDING);

        when(repo.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(com.parkease.payment.exception.PaymentException.class,
                () -> service.getReceiptPath(1L, "sanvi@test.com"));
    }

    @Test
    void shouldReturnExistingReceiptPath() throws Exception {
        // Create actual temp file so File.exists() returns true
        java.io.File tempFile = java.io.File.createTempFile("receipt", ".pdf");
        tempFile.deleteOnExit();

        payment.setStatus(PaymentStatus.PAID);
        payment.setReceiptPath(tempFile.getAbsolutePath());

        when(repo.findById(1L)).thenReturn(Optional.of(payment));

        String result = service.getReceiptPath(1L, "sanvi@test.com");

        assertEquals(tempFile.getAbsolutePath(), result);
        verify(repo, never()).save(any()); // existing file: no regen needed
    }

    @Test
    void shouldThrowWhenVerifyPaymentHasInvalidSignature() {
        com.parkease.payment.dto.request.VerifyPaymentRequest req =
                new com.parkease.payment.dto.request.VerifyPaymentRequest();
        req.setRazorpayOrderId("order_123");
        req.setRazorpayPaymentId("pay_123");
        req.setRazorpaySignature("wrong_signature");

        when(repo.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));
        when(repo.save(any(Payment.class))).thenReturn(payment);

        assertThrows(PaymentException.class, () -> service.verifyPayment(req));
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }


}