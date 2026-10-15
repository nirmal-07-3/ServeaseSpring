
import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { Api } from '../../../core/services/api';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

type BookingStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

type PaymentPreference = 'PAY_ONLINE' | 'PAY_AT_SERVICE';
type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | null;
type ConfirmationType = 'switch' | 'cancel' | null;
type FeedbackType = 'success' | 'error' | null;

interface Booking {
  id: number;
  customerId: number;
  providerId: number;
  providerBusinessName: string;
  providerServiceId: number;
  serviceTitle: string;
  subCategoryId: number;
  subCategoryName: string;
  bookingDate: string;
  bookingTime: string;
  agreedPrice: number;
  address: string;
  city: string;
  state: string;
  pincode: string;
  customerNote?: string;
  status: BookingStatus;
  paymentPreference?: PaymentPreference | null;
  effectivePaymentPreference?: PaymentPreference | null;
  paymentStatus?: PaymentStatus;
  paymentReconciliationRequired?: boolean;
}

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-bookings.html',
  styleUrl: './my-bookings.css'
})
export class MyBookings implements OnInit {
  bookings: Booking[] = [];
  loading = false;
  error = '';
  cancellingId: number | null = null;
  switchingPaymentPreferenceId: number | null = null;

  confirmationType: ConfirmationType = null;
  confirmationBooking: Booking | null = null;
  feedbackMessage = '';
  feedbackType: FeedbackType = null;

  constructor(
    private api: Api,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadBookings();
  }

  loadBookings(): void {
    this.loading = true;
    this.error = '';

    this.api.get<ApiResponse<Booking[]>>('/bookings')
      .pipe(finalize(() => {
        this.loading = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: response => {
          this.bookings = response?.data ?? [];
          this.cdr.detectChanges();
        },
        error: err => {
          console.error('Failed to load bookings:', err);
          this.bookings = [];
          this.error = err?.error?.message ||
            'Unable to load your bookings. Please try again.';
          this.cdr.detectChanges();
        }
      });
  }

  getStatusLabel(status: BookingStatus): string {
    switch (status) {
      case 'PENDING': return 'Pending';
      case 'ACCEPTED': return 'Accepted';
      case 'IN_PROGRESS': return 'In Progress';
      case 'COMPLETED': return 'Completed';
      case 'REJECTED': return 'Rejected';
      case 'CANCELLED': return 'Cancelled';
      default: return status;
    }
  }

  getStatusClass(status: BookingStatus): string {
    return status.toLowerCase().replace('_', '-');
  }

  isAppointmentStarted(booking: Booking): boolean {
    if (!booking.bookingDate || !booking.bookingTime) {
      return true;
    }

    const date = booking.bookingDate.slice(0, 10);
    const time = booking.bookingTime.slice(0, 8);
    const appointment = new Date(`${date}T${time}`);

    if (Number.isNaN(appointment.getTime())) {
      return true;
    }

    return Date.now() >= appointment.getTime();
  }

  canCancel(booking: Booking): boolean {
    return booking.status === 'PENDING' &&
      this.cancellingId === null;
  }

  canSwitchToPayAtService(booking: Booking): boolean {
    return booking.status === 'ACCEPTED' &&
      booking.paymentPreference === 'PAY_ONLINE' &&
      booking.effectivePaymentPreference !== 'PAY_AT_SERVICE' &&
      booking.paymentStatus !== 'SUCCESS' &&
      booking.paymentReconciliationRequired !== true &&
      !this.isAppointmentStarted(booking) &&
      this.switchingPaymentPreferenceId === null;
  }

  openSwitchConfirmation(booking: Booking): void {
    if (!this.canSwitchToPayAtService(booking)) {
      this.showFeedback(
        'error',
        'This booking is no longer eligible to switch to Pay at Service. The appointment may already have started.'
      );
      return;
    }

    this.confirmationBooking = booking;
    this.confirmationType = 'switch';
    this.cdr.detectChanges();
  }

  openCancelConfirmation(booking: Booking): void {
    if (!this.canCancel(booking)) {
      return;
    }

    this.confirmationBooking = booking;
    this.confirmationType = 'cancel';
    this.cdr.detectChanges();
  }

  closeConfirmation(): void {
    this.confirmationType = null;
    this.confirmationBooking = null;
    this.cdr.detectChanges();
  }

  confirmAction(): void {
    const booking = this.confirmationBooking;
    const action = this.confirmationType;

    if (!booking || !action) {
      return;
    }

    this.closeConfirmation();

    if (action === 'switch') {
      this.switchToPayAtService(booking);
    } else {
      this.cancelBooking(booking);
    }
  }

  showFeedback(type: FeedbackType, message: string): void {
    if (!type) {
      return;
    }

    this.feedbackType = type;
    this.feedbackMessage = message;
    this.cdr.detectChanges();
  }

  closeFeedback(): void {
    this.feedbackType = null;
    this.feedbackMessage = '';
    this.cdr.detectChanges();
  }

  switchToPayAtService(booking: Booking): void {
    if (!this.canSwitchToPayAtService(booking) ||
        this.switchingPaymentPreferenceId !== null) {
      return;
    }

    this.switchingPaymentPreferenceId = booking.id;

    this.api.patch<ApiResponse<Booking>>(
      `/bookings/${booking.id}/payment-preference/pay-at-service`
    )
      .pipe(finalize(() => {
        this.switchingPaymentPreferenceId = null;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: response => {
          if (response?.data) {
            this.bookings = this.bookings.map(item =>
              item.id === booking.id
                ? { ...item, ...response.data }
                : item
            );
          }

          this.showFeedback(
            'success',
            response?.message ||
              'Payment preference updated successfully.'
          );

          this.loadBookings();
        },
        error: err => {
          console.error('Failed to switch payment preference:', err);

          this.showFeedback(
            'error',
            err?.error?.message ||
              'Unable to switch to Pay at Service. Please refresh your bookings and try again.'
          );

          this.loadBookings();
        }
      });
  }

  cancelBooking(booking: Booking): void {
    if (!this.canCancel(booking) || this.cancellingId !== null) {
      return;
    }

    this.cancellingId = booking.id;

    this.api.patch<ApiResponse<void>>(
      `/bookings/${booking.id}/cancel`
)
.pipe(finalize(() => {
  this.cancellingId = null;
  this.cdr.detectChanges();
}))
  .subscribe({
    next: response => {
      this.showFeedback(
        'success',
        response?.message ||
        'Your booking has been cancelled successfully.'
      );
      this.loadBookings();
    },
    error: err => {
      console.error('Failed to cancel booking:', err);
      this.showFeedback(
        'error',
        err?.error?.message ||
        'Unable to cancel the booking. Please try again.'
      );
    }
  });
}

formatDate(date: string): string {
  if (!date) {
    return '';
  }

  return new Date(`${date.slice(0, 10)}T00:00:00`)
    .toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric'
    });
}

formatTime(time: string): string {
  if (!time) {
    return '';
  }

  const [hours, minutes] = time.split(':');
  const date = new Date();
  date.setHours(Number(hours), Number(minutes), 0, 0);

  return date.toLocaleTimeString('en-IN', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true
  });
}

getProgressStep(status: BookingStatus): number {
  switch (status) {
    case 'PENDING': return 1;
    case 'ACCEPTED': return 2;
    case 'IN_PROGRESS': return 3;
    case 'COMPLETED': return 4;
    default: return 0;
  }
}
}
