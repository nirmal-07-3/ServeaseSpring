import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs';

export enum BookingStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export interface BookingResponse {
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
  customerNote: string | null;
  status: BookingStatus;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Component({
  selector: 'app-provider-bookings',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink
  ],
  templateUrl: './bookings.html',
  styleUrl: './bookings.css'
})
export class Bookings implements OnInit {

  bookings: BookingResponse[] = [];

  loading = false;
  actionLoading = false;

  errorMessage = '';
  successMessage = '';

  selectedBooking: BookingResponse | null = null;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    console.log('PROVIDER BOOKINGS COMPONENT INITIALIZED');

    this.loadBookings();
  }


  // ============================================================
  // LOAD BOOKINGS
  // ============================================================

  loadBookings(): void {

    console.log('LOAD BOOKINGS STARTED');

    this.loading = true;
    this.errorMessage = '';

    this.cdr.detectChanges();

    this.http
      .get<ApiResponse<BookingResponse[]>>(
        '/api/provider/bookings'
      )
      .pipe(
        finalize(() => {

          console.log(
            'LOAD BOOKINGS FINALIZE'
          );

          this.loading = false;

          this.cdr.detectChanges();

          console.log(
            'LOADING AFTER FINALIZE:',
            this.loading
          );
        })
      )
      .subscribe({

        next: (response) => {

          console.log(
            'PROVIDER BOOKINGS RESPONSE:',
            response
          );

          if (
            response &&
            response.success === true &&
            Array.isArray(response.data)
          ) {

            this.bookings = response.data;

            console.log(
              'BOOKINGS ARRAY:',
              this.bookings
            );

          } else {

            this.bookings = [];

            this.errorMessage =
              response?.message ||
              'Unable to load bookings.';
          }

          /*
           * IMPORTANT:
           * Force Angular to render the new state.
           */
          this.loading = false;

          this.cdr.detectChanges();

          console.log(
            'LOADING AFTER SUCCESS:',
            this.loading
          );
        },

        error: (error) => {

          console.error(
            'PROVIDER BOOKINGS ERROR:',
            error
          );

          this.bookings = [];

          this.errorMessage =
            error?.error?.message ||
            'Failed to load bookings. Please try again.';

          this.loading = false;

          this.cdr.detectChanges();

          console.log(
            'LOADING AFTER ERROR:',
            this.loading
          );
        }

      });
  }


  // ============================================================
  // REFRESH
  // ============================================================

  refreshBookings(): void {

    if (this.loading || this.actionLoading) {
      return;
    }

    this.successMessage = '';

    this.loadBookings();
  }


  // ============================================================
  // ACCEPT
  // ============================================================

  acceptBooking(
    booking: BookingResponse
  ): void {

    this.performAction(
      booking,
      'accept',
      'Booking accepted successfully.'
    );
  }


  // ============================================================
  // REJECT
  // ============================================================

  rejectBooking(
    booking: BookingResponse
  ): void {

    this.performAction(
      booking,
      'reject',
      'Booking rejected successfully.'
    );
  }


  // ============================================================
  // START
  // ============================================================

  startBooking(
    booking: BookingResponse
  ): void {

    this.performAction(
      booking,
      'start',
      'Booking started successfully.'
    );
  }


  // ============================================================
  // COMPLETE
  // ============================================================

  completeBooking(
    booking: BookingResponse
  ): void {

    this.performAction(
      booking,
      'complete',
      'Booking completed successfully.'
    );
  }


  // ============================================================
  // COMMON ACTION
  // ============================================================

  private performAction(
    booking: BookingResponse,
    action: string,
    successMessage: string
  ): void {

    if (this.actionLoading) {
      return;
    }

    this.actionLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.http
      .patch<ApiResponse<void>>(
        `/api/provider/bookings/${booking.id}/${action}`,
        {}
      )
      .pipe(
        finalize(() => {

          this.actionLoading = false;

          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (response) => {

          if (response?.success) {

            this.successMessage =
              response.message ||
              successMessage;

            /*
             * Reload bookings after successful action.
             */
            this.loadBookings();

          } else {

            this.errorMessage =
              response?.message ||
              `Unable to ${action} booking.`;
          }

          this.cdr.detectChanges();
        },

        error: (error) => {

          console.error(
            `PROVIDER BOOKING ${action.toUpperCase()} ERROR:`,
            error
          );

          this.errorMessage =
            error?.error?.message ||
            `Failed to ${action} booking. Please try again.`;

          this.cdr.detectChanges();
        }

      });
  }


  // ============================================================
  // VIEW DETAILS
  // ============================================================

  viewBooking(
    booking: BookingResponse
  ): void {

    this.selectedBooking = booking;

    this.cdr.detectChanges();
  }


  closeBooking(): void {

    this.selectedBooking = null;

    this.cdr.detectChanges();
  }


  // ============================================================
  // STATUS
  // ============================================================

  getStatusLabel(
    status: BookingStatus
  ): string {

    switch (status) {

      case BookingStatus.PENDING:
        return 'Pending';

      case BookingStatus.ACCEPTED:
        return 'Accepted';

      case BookingStatus.REJECTED:
        return 'Rejected';

      case BookingStatus.IN_PROGRESS:
        return 'In progress';

      case BookingStatus.COMPLETED:
        return 'Completed';

      case BookingStatus.CANCELLED:
        return 'Cancelled';

      default:
        return status;
    }
  }


  getStatusClass(
    status: BookingStatus
  ): string {

    switch (status) {

      case BookingStatus.PENDING:
        return 'pending';

      case BookingStatus.ACCEPTED:
        return 'accepted';

      case BookingStatus.REJECTED:
        return 'rejected';

      case BookingStatus.IN_PROGRESS:
        return 'in-progress';

      case BookingStatus.COMPLETED:
        return 'completed';

      case BookingStatus.CANCELLED:
        return 'cancelled';

      default:
        return '';
    }
  }


  // ============================================================
  // ACTION VISIBILITY
  // ============================================================

  canAccept(
    booking: BookingResponse
  ): boolean {

    return booking.status === BookingStatus.PENDING;
  }


  canReject(
    booking: BookingResponse
  ): boolean {

    return booking.status === BookingStatus.PENDING;
  }


  canStart(
    booking: BookingResponse
  ): boolean {

    return booking.status === BookingStatus.ACCEPTED;
  }


  canComplete(
    booking: BookingResponse
  ): boolean {

    return booking.status === BookingStatus.IN_PROGRESS;
  }


  // ============================================================
  // DATE
  // ============================================================

  formatDate(
    date: string
  ): string {

    if (!date) {
      return '-';
    }

    const parsed = new Date(
      `${date}T00:00:00`
    );

    return parsed.toLocaleDateString(
      'en-IN',
      {
        day: '2-digit',
        month: 'short',
        year: 'numeric'
      }
    );
  }


  // ============================================================
  // TIME
  // ============================================================

  formatTime(
    time: string
  ): string {

    if (!time) {
      return '-';
    }

    const parts = time.split(':');

    if (parts.length < 2) {
      return time;
    }

    const hours = Number(parts[0]);
    const minutes = Number(parts[1]);

    const date = new Date();

    date.setHours(hours);
    date.setMinutes(minutes);
    date.setSeconds(0);

    return date.toLocaleTimeString(
      'en-IN',
      {
        hour: '2-digit',
        minute: '2-digit'
      }
    );
  }


  // ============================================================
  // PRICE
  // ============================================================

  formatPrice(
    price: number
  ): string {

    return new Intl.NumberFormat(
      'en-IN',
      {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      }
    ).format(price || 0);
  }


  // ============================================================
  // STATISTICS
  // ============================================================

  get totalBookings(): number {

    return this.bookings.length;
  }


  get pendingBookings(): number {

    return this.bookings.filter(
      booking =>
        booking.status === BookingStatus.PENDING
    ).length;
  }


  get acceptedBookings(): number {

    return this.bookings.filter(
      booking =>
        booking.status === BookingStatus.ACCEPTED
    ).length;
  }


  get completedBookings(): number {

    return this.bookings.filter(
      booking =>
        booking.status === BookingStatus.COMPLETED
    ).length;
  }

}
