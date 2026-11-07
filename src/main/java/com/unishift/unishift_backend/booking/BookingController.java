package com.unishift.unishift_backend.booking;

import com.unishift.unishift_backend.booking.dto.BookingResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/api/v1/jobs/{id}/bookings")
    public ResponseEntity<BookingResponse> bookJob(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        BookingResponse response = bookingService.createBooking(id, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/bookings/me")
    public ResponseEntity<List<BookingResponse>> getMyBookings(@AuthenticationPrincipal UserDetails userDetails) {
        List<BookingResponse> bookings = bookingService.getStudentBookings(userDetails.getUsername());
        return ResponseEntity.ok(bookings);
    }
}
