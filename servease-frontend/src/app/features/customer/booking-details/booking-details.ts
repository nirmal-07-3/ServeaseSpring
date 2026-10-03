import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import {
  ActivatedRoute,
  Router,
  RouterLink
} from '@angular/router';
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
  selector: 'app-booking-details',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink
  ],
  templateUrl: './booking-details.html',
  styleUrl: './booking-details.css'
})
export class BookingDetails implements OnInit {

  booking: Booking | null = null;

  loading = false;
  error = '';

  constructor(
    private api: Api,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {

    const id =
      this.route.snapshot.paramMap.get('id');

    if (!id) {
      this.error = 'Invalid booking ID.';
      return;
    }

    this.loadBooking(Number(id));
  }

  loadBooking(id: number): void {

    this.loading = true;
    this.error = '';

    this.api
      .get<ApiResponse<Booking>>(
        `/bookings/${id}`
      )
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (response) => {
          this.booking =
            response?.data ?? null;

          this.cdr.detectChanges();
        },

        error: (err) => {

          console.error(
            'Failed to load booking:',
            err
          );

          this.booking = null;

          this.error =
            err?.error?.message ||
            'Unable to load booking details.';

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

  isNormalTrackingStatus(
    status: Booking['status']
  ): boolean {

    return [
      'PENDING',
      'ACCEPTED',
      'IN_PROGRESS',
      'COMPLETED'
    ].includes(status);
  }

  formatDate(date: string): string {

    if (!date) {
      return '';
    }

    return new Date(
      `${date}T00:00:00`
    ).toLocaleDateString(
      'en-IN',
      {
        day: 'numeric',
        month: 'long',
        year: 'numeric'
      }
    );
  }

  formatTime(time: string): string {

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

  goBack(): void {
    this.router.navigate(['/bookings']);
  }
}
