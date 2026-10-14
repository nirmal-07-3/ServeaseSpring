import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs/operators';

import { Auth } from '../../../core/services/auth';

interface ProviderBooking {
  id?: number;
  customerName?: string;
  serviceName?: string;
  serviceTitle?: string;
  bookingDate?: string;
  bookingTime?: string;
  agreedPrice?: number;
  status?: string;
  address?: string;
  city?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {

  providerName = 'Provider';

  bookings: ProviderBooking[] = [];

  loading = false;
  errorMessage = '';

  sidebarOpen = false;

  constructor(
    private auth: Auth,
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}

  ngOnInit(): void {
    const user = this.auth.getUser();

    if (user?.name) {
      this.providerName = user.name;
    }

    this.loadProviderData();
  }

  loadProviderData(): void {
    this.loading = true;
    this.errorMessage = '';

    this.http.get<any>('/api/provider/bookings')
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (response) => {
          if (
            response?.success === true &&
            Array.isArray(response.data)
          ) {
            this.bookings = response.data;
          } else {
            this.bookings = [];

            this.errorMessage =
              response?.message ||
              'Unable to load provider bookings.';
          }
        },

        error: (error) => {
          console.error(
            'Error loading provider dashboard:',
            error
          );

          this.bookings = [];

          this.errorMessage =
            error?.error?.message ||
            'Unable to load dashboard data. Please try again.';
        }
      });
  }

  refreshDashboard(): void {
    if (this.loading) {
      return;
    }

    this.loadProviderData();
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
  }

  logout(): void {
    this.auth.logout();
    this.closeSidebar();
    this.router.navigate(['/login']);
  }

  getProviderInitials(): string {
    if (!this.providerName) {
      return 'P';
    }

    const parts = this.providerName
      .trim()
      .split(/\s+/);

    if (parts.length === 1) {
      return parts[0]
        .charAt(0)
        .toUpperCase();
    }

    return (
      parts[0].charAt(0) +
      parts[parts.length - 1].charAt(0)
    ).toUpperCase();
  }

  get totalBookings(): number {
    return this.bookings.length;
  }

  get pendingBookings(): number {
    return this.bookings.filter(
      booking => booking.status === 'PENDING'
    ).length;
  }

  get acceptedBookings(): number {
    return this.bookings.filter(
      booking => booking.status === 'ACCEPTED'
    ).length;
  }

  get inProgressBookings(): number {
    return this.bookings.filter(
      booking => booking.status === 'IN_PROGRESS'
    ).length;
  }

  get completedBookings(): number {
    return this.bookings.filter(
      booking => booking.status === 'COMPLETED'
    ).length;
  }

  get totalEarnings(): number {
    return this.bookings
      .filter(
        booking => booking.status === 'COMPLETED'
      )
      .reduce((total, booking) => {
        return total +
          Number(booking.agreedPrice || 0);
      }, 0);
  }

  get recentBookings(): ProviderBooking[] {
    return [...this.bookings]
      .sort((a, b) => {
        const dateA = new Date(
          `${a.bookingDate || ''}T${a.bookingTime || '00:00'}`
        ).getTime();

        const dateB = new Date(
          `${b.bookingDate || ''}T${b.bookingTime || '00:00'}`
        ).getTime();

        return dateB - dateA;
      })
      .slice(0, 5);
  }

  get todaysBookings(): ProviderBooking[] {
    const today = new Date();

    const todayString = [
      today.getFullYear(),
      String(today.getMonth() + 1).padStart(2, '0'),
      String(today.getDate()).padStart(2, '0')
    ].join('-');

    return this.bookings
      .filter(booking => {
        return (
          this.formatDateForComparison(
            booking.bookingDate
          ) === todayString
        );
      })
      .sort((a, b) => {
        const timeA =
          a.bookingTime || '00:00';

        const timeB =
          b.bookingTime || '00:00';

        return timeA.localeCompare(timeB);
      });
  }

  private formatDateForComparison(
    date: any
  ): string {

    if (!date) {
      return '';
    }

    if (typeof date === 'string') {
      return date.substring(0, 10);
    }

    const parsedDate = new Date(date);

    if (
      Number.isNaN(
        parsedDate.getTime()
      )
    ) {
      return '';
    }

    return [
      parsedDate.getFullYear(),
      String(
        parsedDate.getMonth() + 1
      ).padStart(2, '0'),
      String(
        parsedDate.getDate()
      ).padStart(2, '0')
    ].join('-');
  }

  getStatusLabel(
    status: string | undefined
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
        return status || 'Unknown';
    }
  }

  getStatusClass(
    status: string | undefined
  ): string {

    switch (status) {

      case 'PENDING':
        return 'status-pending';

      case 'ACCEPTED':
        return 'status-accepted';

      case 'IN_PROGRESS':
        return 'status-progress';

      case 'COMPLETED':
        return 'status-completed';

      case 'REJECTED':
        return 'status-rejected';

      case 'CANCELLED':
        return 'status-cancelled';

      default:
        return 'status-default';
    }
  }

  formatDate(date: any): string {

    if (!date) {
      return '—';
    }

    const parsedDate = new Date(date);

    if (
      Number.isNaN(
        parsedDate.getTime()
      )
    ) {
      return date;
    }

    return parsedDate.toLocaleDateString(
      'en-IN',
      {
        day: '2-digit',
        month: 'short',
        year: 'numeric'
      }
    );
  }

  formatTime(
    time: string | undefined | null
  ): string {

    if (!time) {
      return '—';
    }

    const parts = time.split(':');

    if (parts.length < 2) {
      return time;
    }

    const hours = Number(parts[0]);
    const minutes = Number(parts[1]);

    if (
      Number.isNaN(hours) ||
      Number.isNaN(minutes)
    ) {
      return time;
    }

    const date = new Date();

    date.setHours(
      hours,
      minutes,
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

  formatPrice(price: any): string {

    const amount = Number(price || 0);

    return amount.toLocaleString(
      'en-IN',
      {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
      }
    );
  }
}
