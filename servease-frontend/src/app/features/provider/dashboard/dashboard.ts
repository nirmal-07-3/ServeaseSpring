import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs';
import { Auth } from '../../../core/services/auth';



import {
  BookingStatus,
  BookingResponse
} from '../bookings/bookings';



interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {

  providerName = 'Provider';

  bookings: BookingResponse[] = [];

  loading = false;
  errorMessage = '';

  constructor(
    private auth: Auth,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {
    const user = this.auth.getUser();

    if (user?.name) {
      this.providerName = user.name;
    }
  }

  ngOnInit(): void {

    this.loadProviderData();
  }


  // ============================================================
  // LOAD PROVIDER DATA
  // ============================================================

  loadProviderData(): void {

    this.loading = true;
    this.errorMessage = '';

    this.http
      .get<ApiResponse<BookingResponse[]>>(
        '/api/provider/bookings'
      )
      .pipe(
        finalize(() => {

          this.loading = false;

          this.cdr.detectChanges();

          console.log(
            'PROVIDER DASHBOARD LOADING:',
            this.loading
          );

        })
      )
      .subscribe({

        next: (response) => {

          console.log(
            'PROVIDER DASHBOARD RESPONSE:',
            response
          );

          if (
            response &&
            response.success === true &&
            Array.isArray(response.data)
          ) {

            this.bookings = response.data;

          } else {

            this.bookings = [];

            this.errorMessage =
              response?.message ||
              'Unable to load provider data.';
          }

          this.cdr.detectChanges();
        },

        error: (error) => {

          console.error(
            'PROVIDER DASHBOARD ERROR:',
            error
          );

          this.bookings = [];

          this.errorMessage =
            error?.error?.message ||
            'Failed to load dashboard data. Please try again.';

          this.cdr.detectChanges();
        }

      });
  }


  // ============================================================
  // REFRESH
  // ============================================================

  refreshDashboard(): void {

    if (this.loading) {
      return;
    }

    this.loadProviderData();
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


  get inProgressBookings(): number {

    return this.bookings.filter(
      booking =>
        booking.status === BookingStatus.IN_PROGRESS
    ).length;
  }


  get completedBookings(): number {

    return this.bookings.filter(
      booking =>
        booking.status === BookingStatus.COMPLETED
    ).length;
  }


  // ============================================================
  // TOTAL EARNINGS
  // ============================================================

  get totalEarnings(): number {

    return this.bookings
      .filter(
        booking =>
          booking.status === BookingStatus.COMPLETED
      )
      .reduce(
        (total, booking) =>
          total + Number(booking.agreedPrice || 0),
        0
      );
  }


  // ============================================================
  // RECENT BOOKINGS
  // ============================================================

  get recentBookings(): BookingResponse[] {

    return [...this.bookings]
      .sort((a, b) => {

        const dateA =
          new Date(
            `${a.bookingDate}T${a.bookingTime}`
          ).getTime();

        const dateB =
          new Date(
            `${b.bookingDate}T${b.bookingTime}`
          ).getTime();

        return dateB - dateA;

      })
      .slice(0, 5);
  }


  // ============================================================
  // TODAY'S BOOKINGS
  // ============================================================

  get todaysBookings(): BookingResponse[] {

    const today =
      new Date();

    const todayString =
      this.formatDateForComparison(today);

    return [...this.bookings]
      .filter(
        booking =>
          booking.bookingDate === todayString
      )
      .sort((a, b) =>
        a.bookingTime.localeCompare(
          b.bookingTime
        )
      );
  }


  // ============================================================
  // DATE COMPARISON
  // ============================================================

  private formatDateForComparison(
    date: Date
  ): string {

    const year =
      date.getFullYear();

    const month =
      String(
        date.getMonth() + 1
      ).padStart(2, '0');

    const day =
      String(
        date.getDate()
      ).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }


  // ============================================================
  // STATUS LABEL
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


  // ============================================================
  // STATUS CLASS
  // ============================================================

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
  // DATE
  // ============================================================

  formatDate(
    date: string
  ): string {

    if (!date) {
      return '-';
    }

    const parsed =
      new Date(
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

    const parts =
      time.split(':');

    if (parts.length < 2) {
      return time;
    }

    const hours =
      Number(parts[0]);

    const minutes =
      Number(parts[1]);

    const date =
      new Date();

    date.setHours(
      hours,
      minutes,
      0,
      0
    );

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
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
      }
    ).format(
      Number(price || 0)
    );
  }

}
