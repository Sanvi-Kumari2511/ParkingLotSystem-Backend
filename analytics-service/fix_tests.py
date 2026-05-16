import re

# Fix AnalyticsServiceImplTest
with open('src/test/java/com/parkease/analytics/service/AnalyticsServiceImplTest.java', 'r') as f:
    content = f.read()

# Add imports
content = content.replace('import org.springframework.web.client.RestTemplate;', 
'''import com.parkease.analytics.client.*;
import java.util.Arrays;''')

# Replace mocks
mock_str = '''    @Mock private OccupancyLogRepository logRepo;
    @Mock private com.parkease.analytics.repository.AuditLogRepository auditRepo;
    @Mock private BookingServiceClient bookingServiceClient;
    @Mock private PaymentServiceClient paymentServiceClient;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private ParkingLotServiceClient parkingLotServiceClient;
    @Mock private SpotServiceClient spotServiceClient;'''

content = re.sub(r'    @Mock\s+private OccupancyLogRepository logRepo;\s+@Mock\s+private RestTemplate restTemplate;', mock_str, content)

# Clear setUp
content = re.sub(r'        ReflectionTestUtils\.setField\(service, "bookingServiceUrl", "http://booking-service"\);\s+ReflectionTestUtils\.setField\(service, "paymentServiceUrl", "http://payment-service"\);', '', content)

# Fix RevenueReport
content = re.sub(r'        when\(restTemplate\.exchange\(\s+contains\("/api/bookings/lot/1"\),\s+any\(\),\s+any\(\),\s+eq\(Map\[\]\.class\)\s+\)\)\.thenReturn\(ResponseEntity\.ok\(bookings\)\);', 
    '        when(bookingServiceClient.getBookingsByLot(eq(1L), anyString())).thenReturn(Arrays.asList(bookings));', content)

content = re.sub(r'        when\(restTemplate\.exchange\(\s+contains\("/api/bookings/lot/1"\),\s+any\(\),\s+any\(\),\s+eq\(Map\[\]\.class\)\s+\)\)\.thenReturn\(ResponseEntity\.ok\(bookings\)\);', 
    '        when(bookingServiceClient.getBookingsByLot(eq(1L), anyString())).thenReturn(Arrays.asList(bookings));', content)

# Fix PlatformSummary
content = re.sub(r'        when\(restTemplate\.getForObject\(contains\("/api/bookings/admin/all"\), eq\(Map\[\]\.class\)\)\)\s+\.thenReturn\(allBookings\);', 
    '        when(bookingServiceClient.getAllBookings(anyString())).thenReturn(Arrays.asList(allBookings));', content)

content = re.sub(r'        when\(restTemplate\.getForObject\(contains\("/api/payments/admin/all"\), eq\(Map\[\]\.class\)\)\)\s+\.thenReturn\(payments\);', 
    '        when(paymentServiceClient.getAllPayments(anyString())).thenReturn(Arrays.asList(payments));', content)

with open('src/test/java/com/parkease/analytics/service/AnalyticsServiceImplTest.java', 'w') as f:
    f.write(content)


# Fix OccupancyLogSchedulerTest
with open('src/test/java/com/parkease/analytics/service/OccupancyLogSchedulerTest.java', 'r') as f:
    content2 = f.read()

content2 = content2.replace('import org.springframework.web.client.RestTemplate;', 
'''import com.parkease.analytics.client.*;
import java.util.Arrays;''')

mock_str2 = '''    @Mock private AnalyticsService analyticsService;
    @Mock private BookingServiceClient bookingServiceClient;
    @Mock private SpotServiceClient spotServiceClient;'''

content2 = re.sub(r'    @Mock\s+private AnalyticsService analyticsService;\s+@Mock\s+private RestTemplate restTemplate;', mock_str2, content2)

content2 = re.sub(r'    @BeforeEach\s+void setUp\(\) \{\s+ReflectionTestUtils\.setField\(\s+scheduler,\s+"spotServiceUrl",\s+"http://parkingspot-service"\s+\);\s+ReflectionTestUtils\.setField\(\s+scheduler,\s+"bookingServiceUrl",\s+"http://booking-service"\s+\);\s+\}', 
    '''    @BeforeEach
    void setUp() {
    }''', content2)

content2 = re.sub(r'        when\(restTemplate\.getForObject\(\s+contains\("/api/bookings/admin/all"\),\s+eq\(Map\[\]\.class\)\s+\)\)\.thenReturn\(bookings\);', 
    '        when(bookingServiceClient.getDistinctLotIds()).thenReturn(Arrays.asList(1L, 2L));', content2)

content2 = re.sub(r'        when\(restTemplate\.getForObject\(\s+contains\("/api/spots/lot/1/count"\),\s+eq\(Integer\.class\)\s+\)\)\.thenReturn\(1\);', 
    '        when(spotServiceClient.getAvailableSpotCount(1L)).thenReturn(1);', content2)

content2 = re.sub(r'        when\(restTemplate\.getForObject\(\s+contains\("/api/spots/lot/1"\),\s+eq\(Map\[\]\.class\)\s+\)\)\.thenReturn\(spotsLot1\);', 
    '        when(spotServiceClient.getSpotsByLot(1L)).thenReturn(Arrays.asList(spotsLot1));', content2)

content2 = re.sub(r'        when\(restTemplate\.getForObject\(\s+contains\("/api/spots/lot/2/count"\),\s+eq\(Integer\.class\)\s+\)\)\.thenReturn\(1\);', 
    '        when(spotServiceClient.getAvailableSpotCount(2L)).thenReturn(1);', content2)

content2 = re.sub(r'        when\(restTemplate\.getForObject\(\s+contains\("/api/spots/lot/2"\),\s+eq\(Map\[\]\.class\)\s+\)\)\.thenReturn\(spotsLot2\);', 
    '        when(spotServiceClient.getSpotsByLot(2L)).thenReturn(Arrays.asList(spotsLot2));', content2)

content2 = re.sub(r'        when\(restTemplate\.getForObject\(\s+contains\("/api/bookings/admin/all"\),\s+eq\(Map\[\]\.class\)\s+\)\)\.thenReturn\(new Map\[0\]\);', 
    '        when(bookingServiceClient.getDistinctLotIds()).thenReturn(Arrays.asList());', content2)

content2 = re.sub(r'        when\(restTemplate\.getForObject\(\s+contains\("/api/spots/lot/1/count"\),\s+eq\(Integer\.class\)\s+\)\)\.thenThrow\(new RuntimeException\("spot service down"\)\);', 
    '        when(spotServiceClient.getAvailableSpotCount(1L)).thenThrow(new RuntimeException("spot service down"));', content2)

content2 = re.sub(r'        when\(restTemplate\.getForObject\(\s+contains\("/api/bookings/admin/all"\),\s+eq\(Map\[\]\.class\)\s+\)\)\.thenThrow\(new RuntimeException\("booking service down"\)\);', 
    '        when(bookingServiceClient.getDistinctLotIds()).thenThrow(new RuntimeException("booking service down"));', content2)

with open('src/test/java/com/parkease/analytics/service/OccupancyLogSchedulerTest.java', 'w') as f:
    f.write(content2)

