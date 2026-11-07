package com.unishift.unishift_backend.booking.dto;

import com.unishift.unishift_backend.booking.Booking;
import com.unishift.unishift_backend.booking.BookingStatus;

import java.time.Instant;

public record BookingResponse(
    Long id,
    Long jobId,
    String jobTitle,
    String jobLocation,
    Long studentId,
    String studentName,
    String studentEmail,
    BookingStatus status,
    Instant createdAt
) {
    public static BookingResponse fromEntity(Booking booking) {
        return new BookingResponse(
            booking.getId(),
            booking.getJob() != null ? booking.getJob().getId() : null,
            booking.getJob() != null ? booking.getJob().getTitle() : null,
            booking.getJob() != null ? booking.getJob().getLocation() : null,
            booking.getStudent() != null ? booking.getStudent().getId() : null,
            booking.getStudent() != null ? booking.getStudent().getName() : null,
            booking.getStudent() != null ? booking.getStudent().getEmail() : null,
            booking.getStatus(),
            booking.getCreatedAt()
        );
    }
}
