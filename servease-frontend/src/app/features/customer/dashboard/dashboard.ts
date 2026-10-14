import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { Auth } from '../../../core/services/auth';
import { Api } from '../../../core/services/api';

interface ServiceCategory {
  name: string;
  icon: string;
  description: string;
}

interface RecommendedService {
  title: string;
  provider: string;
  rating: number;
  reviews: number;
  price: string;
  location: string;
  icon: string;
}

interface BookingResponse {
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
  status: string;
}

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
export class Dashboard {

  userName = 'there';

  categories: ServiceCategory[] = [
    {
      name: 'Home Cleaning',
      icon: '✦',
      description: 'Fresh, clean and comfortable'
    },
    {
      name: 'Plumbing',
      icon: '⌁',
      description: 'Fix leaks and water issues'
    },
    {
      name: 'Electrical',
      icon: 'ϟ',
      description: 'Safe electrical solutions'
    },
    {
      name: 'AC Repair',
      icon: '❄',
      description: 'Keep your home comfortable'
    },
    {
      name: 'Painting',
      icon: '◒',
      description: 'Give your space a new look'
    },
    {
      name: 'Appliance Repair',
      icon: '▣',
      description: 'Reliable appliance service'
    }
  ];

  recommendedServices: RecommendedService[] = [
    {
      title: 'Professional Home Cleaning',
      provider: 'CleanCare Services',
      rating: 4.9,
      reviews: 128,
      price: '₹499',
      location: 'Available near you',
      icon: '✦'
    },
    {
      title: 'Expert Plumbing Service',
      provider: 'QuickFix Plumbing',
      rating: 4.8,
      reviews: 94,
      price: '₹299',
      location: 'Available near you',
      icon: '⌁'
    },
    {
      title: 'AC Service & Repair',
      provider: 'CoolPoint Experts',
      rating: 4.9,
      reviews: 76,
      price: '₹399',
      location: 'Available near you',
      icon: '❄'
    }
  ];

  allBookings: BookingResponse[] = [];
  recentBookings: BookingResponse[] = [];

  bookingsLoading = false;
  bookingsError = '';

  constructor(
    private auth: Auth,
    private api: Api,
    private cdr: ChangeDetectorRef
  ) {
    const user = this.auth.getUser();

    if (user?.name) {
      this.userName = user.name;
    }

    this.loadRecentBookings();
  }

  loadRecentBookings(): void {
    this.bookingsLoading = true;
    this.bookingsError = '';

    this.api
      .get<ApiResponse<BookingResponse[]>>('/bookings')
      .pipe(
        finalize(() => {
          this.bookingsLoading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (response) => {
          if (response && Array.isArray(response.data)) {
            this.allBookings = [...response.data];

            this.recentBookings = [...response.data]
              .sort((a, b) => {
                return this.getBookingTimestamp(b) - this.getBookingTimestamp(a);
              })
              .slice(0, 3);
          } else {
            this.allBookings = [];
            this.recentBookings = [];
          }
        },
        error: (error) => {
          console.error('DASHBOARD: bookings error =', error);

          this.allBookings = [];
          this.recentBookings = [];

          this.bookingsError =
            error?.error?.message ||
            'Unable to load your bookings.';
        }
      });
  }

  get totalBookings(): number {
    return this.allBookings.length;
  }

  get pendingBookings(): number {
    return this.allBookings.filter(
      booking => booking.status === 'PENDING'
    ).length;
  }

  get activeBookings(): number {
    return this.allBookings.filter(
      booking =>
        booking.status === 'ACCEPTED' ||
        booking.status === 'IN_PROGRESS'
    ).length;
  }

  get completedBookings(): number {
    return this.allBookings.filter(
      booking => booking.status === 'COMPLETED'
    ).length;
  }

  get upcomingBooking(): BookingResponse | null {
    const now = new Date().getTime();

    const upcoming = this.allBookings
      .filter(booking => {
        const timestamp = this.getBookingTimestamp(booking);

        return (
          timestamp >= now &&
          booking.status !== 'COMPLETED' &&
          booking.status !== 'CANCELLED' &&
          booking.status !== 'REJECTED'
        );
      })
      .sort((a, b) => {
        return this.getBookingTimestamp(a) - this.getBookingTimestamp(b);
      });

    return upcoming.length > 0 ? upcoming[0] : null;
  }

  private getBookingTimestamp(booking: BookingResponse): number {
    if (!booking.bookingDate) {
      return 0;
    }

    const date = booking.bookingDate.substring(0, 10);
    const time = booking.bookingTime || '00:00';

    const timestamp = new Date(
      `${date}T${time}`
    ).getTime();

    return Number.isNaN(timestamp) ? 0 : timestamp;
  }

  getStatusLabel(status: string): string {
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

  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'pending';

      case 'ACCEPTED':
        return 'accepted';

      case 'IN_PROGRESS':
        return 'in-progress';

      case 'COMPLETED':
        return 'completed';

      case 'REJECTED':
        return 'rejected';

      case 'CANCELLED':
        return 'cancelled';

      default:
        return '';
    }
  }

  formatDate(date: string): string {
    if (!date) {
      return '-';
    }

    const parts = date.substring(0, 10).split('-');

    if (parts.length !== 3) {
      return date;
    }

    const year = Number(parts[0]);
    const month = Number(parts[1]) - 1;
    const day = Number(parts[2]);

    const localDate = new Date(year, month, day);

    return localDate.toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric'
    });
  }

  formatTime(time: string): string {
    if (!time) {
      return '-';
    }

    const parts = time.split(':');

    if (parts.length < 2) {
      return time;
    }

    const hours = Number(parts[0]);
    const minutes = Number(parts[1]);

    if (Number.isNaN(hours) || Number.isNaN(minutes)) {
      return time;
    }

    const date = new Date();

    date.setHours(hours);
    date.setMinutes(minutes);
    date.setSeconds(0);

    return date.toLocaleTimeString('en-IN', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('en-IN', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2
    }).format(price || 0);
  }
}
