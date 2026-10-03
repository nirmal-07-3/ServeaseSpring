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

  status:
    | 'PENDING'
    | 'ACCEPTED'
    | 'REJECTED'
    | 'IN_PROGRESS'
    | 'COMPLETED'
    | 'CANCELLED';
}

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink
  ],
  templateUrl: './my-bookings.html',
  styleUrl: './my-bookings.css'
})
export class MyBookings implements OnInit {

  bookings: Booking[] = [];

  loading = false;
  error = '';
  cancellingId: number | null = null;

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

    this.api
      .get<ApiResponse<Booking[]>>('/bookings')
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (response) => {
          this.bookings = response?.data ?? [];
          this.cdr.detectChanges();
        },

        error: (err) => {
          console.error(
            'Failed to load bookings:',
            err
          );

          this.bookings = [];

          this.error =
            err?.error?.message ||
            'Unable to load your bookings. Please try again.';

          this.cdr.detectChanges();
        }

      });
  }

  getStatusLabel(
    status: Booking['status']
  ): string {

    switch (status) {

      case 'PENDING':
        return 'Pending';

      case 'ACCEPTED':
        return 'Accepted';

      case 'IN_PROGRESS':
        return 'In Progress';

      case 'COMPLETED':
        return 'Completed';

      case 'REJECTED':
        return 'Rejected';

      case 'CANCELLED':
        return 'Cancelled';

      default:
        return status;
    }
  }

  getStatusClass(
    status: Booking['status']
  ): string {

    return status
      .toLowerCase()
      .replace('_', '-');
  }

  canCancel(
    booking: Booking
  ): boolean {

    return booking.status === 'PENDING';
  }

  cancelBooking(
    booking: Booking
  ): void {

    if (
      !this.canCancel(booking) ||
      this.cancellingId !== null
    ) {
      return;
    }

    const confirmed = window.confirm(
      'Are you sure you want to cancel this booking?'
    );

    if (!confirmed) {
      return;
    }

    this.cancellingId = booking.id;

    this.api
      .patch<ApiResponse<void>>(
        `/bookings/${booking.id}/cancel`
      )
      .pipe(
        finalize(() => {
          this.cancellingId = null;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: () => {
          this.loadBookings();
        },

        error: (err) => {
          console.error(
            'Failed to cancel booking:',
            err
          );

          alert(
            err?.error?.message ||
            'Unable to cancel the booking. Please try again.'
          );
        }

      });
  }

  formatDate(
    date: string
  ): string {

    if (!date) {
      return '';
    }

    const parsedDate =
      new Date(`${date}T00:00:00`);

    return parsedDate.toLocaleDateString(
      'en-IN',
      {
        day: 'numeric',
        month: 'short',
        year: 'numeric'
      }
    );
  }

  formatTime(
    time: string
  ): string {

    if (!time) {
      return '';
    }

    const [hours, minutes] =
      time.split(':');

    const date = new Date();

    date.setHours(
      Number(hours),
      Number(minutes),
      0,
      0
    );

    return date.toLocaleTimeString(
      'en-IN',
      {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
      }
    );
  }

  getProgressStep(
    status: Booking['status']
  ): number {

    switch (status) {

      case 'PENDING':
        return 1;

      case 'ACCEPTED':
        return 2;

      case 'IN_PROGRESS':
        return 3;

      case 'COMPLETED':
        return 4;

      default:
        return 0;
    }
  }
}
