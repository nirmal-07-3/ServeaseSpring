import {
  ChangeDetectorRef,
  Component
} from '@angular/core';

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


  // ============================================================
  // EXISTING CATEGORY DATA
  // ============================================================

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


  // ============================================================
  // EXISTING RECOMMENDED SERVICES
  // ============================================================

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


  // ============================================================
  // RECENT BOOKINGS
  // ============================================================

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


  // ============================================================
  // LOAD RECENT BOOKINGS
  // ============================================================

  loadRecentBookings(): void {

    console.log(
      'DASHBOARD: loading recent bookings'
    );

    this.bookingsLoading = true;

    this.bookingsError = '';


    this.api
      .get<ApiResponse<BookingResponse[]>>('/bookings')

      .pipe(

        finalize(() => {

          console.log(
            'DASHBOARD: bookings finalize'
          );

          this.bookingsLoading = false;

          this.cdr.detectChanges();

        })

      )

      .subscribe({

        next: (response) => {

          console.log(
            'DASHBOARD: bookings response =',
            response
          );


          if (
            response &&
            Array.isArray(response.data)
          ) {

            this.recentBookings =
              response.data.slice(0, 3);

          } else {

            this.recentBookings = [];

          }


          console.log(
            'DASHBOARD: recentBookings =',
            this.recentBookings
          );

        },


        error: (error) => {

          console.error(
            'DASHBOARD: bookings error =',
            error
          );


          this.recentBookings = [];


          this.bookingsError =
            error?.error?.message ||
            'Unable to load recent bookings.';

        }

      });

  }


  // ============================================================
  // STATUS HELPERS
  // ============================================================

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


  // ============================================================
  // DATE
  // ============================================================

  formatDate(date: string): string {

    if (!date) {
      return '-';
    }


    const parts = date.split('-');


    if (parts.length !== 3) {
      return date;
    }


    const year = Number(parts[0]);
    const month = Number(parts[1]) - 1;
    const day = Number(parts[2]);


    const localDate =
      new Date(year, month, day);


    return localDate.toLocaleDateString(
      'en-IN',
      {
        day: 'numeric',
        month: 'short',
        year: 'numeric'
      }
    );

  }


  // ============================================================
  // TIME
  // ============================================================

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

  formatPrice(price: number): string {

    return new Intl.NumberFormat(
      'en-IN',
      {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
      }
    ).format(price || 0);

  }

}
