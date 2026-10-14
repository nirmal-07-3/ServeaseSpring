import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import {
  CommonModule
} from '@angular/common';

import {
  ActivatedRoute,
  Router,
  RouterLink
} from '@angular/router';

import {
  FormsModule
} from '@angular/forms';

import {
  Api
} from '../../../core/services/api';

import {
  finalize
} from 'rxjs/operators';

type PaymentPreference = 'PAY_ONLINE' | 'PAY_AT_SERVICE' | '';

interface ProviderService {
  id: number;
  providerId: number;
  providerBusinessName: string;
  subCategoryId: number;
  subCategoryName: string;
  title: string;
  description: string;
  basePrice: number;
  estimatedDurationMinutes: number;
  serviceRadiusKm: number;
  active: boolean;
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
  customerNote: string | null;
  status: string;
  paymentPreference: PaymentPreference;
  estimatedDurationMinutesSnapshot?: number | null;
  platformFeeRateSnapshot?: number | null;
  platformFeeAmountSnapshot?: number | null;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Component({
  selector: 'app-booking',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink
  ],
  templateUrl: './booking.html',
  styleUrl: './booking.css'
})
export class Booking implements OnInit {
  categoryId!: number;
  subCategoryId!: number;
  serviceId!: number;

  service: ProviderService | null = null;

  bookingDate = '';
  bookingTime = '';
  address = '';
  city = '';
  state = '';
  pincode = '';
  customerNote = '';

  paymentPreference: PaymentPreference = '';

  loading = false;
  submitting = false;
  error = '';
  success = '';

  minDate = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: Api,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.setMinimumDate();

    this.route.paramMap.subscribe(params => {
      const categoryId = Number(params.get('categoryId'));
      const subCategoryId = Number(params.get('subCategoryId'));
      const serviceId = Number(params.get('serviceId'));

      if (
        !categoryId ||
        categoryId <= 0 ||
        !subCategoryId ||
        subCategoryId <= 0 ||
        !serviceId ||
        serviceId <= 0
      ) {
        this.error = 'Invalid service selection.';
        return;
      }

      this.categoryId = categoryId;
      this.subCategoryId = subCategoryId;
      this.serviceId = serviceId;

      this.loadService();
    });
  }

  setMinimumDate(): void {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');

    this.minDate = `${year}-${month}-${day}`;
  }

  loadService(): void {
    this.loading = true;
    this.error = '';

    this.api
      .get<ApiResponse<ProviderService[]>>(
        `/subcategories/${this.subCategoryId}/services`
      )
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: response => {
          if (
            response &&
            response.success &&
            Array.isArray(response.data)
          ) {
            const foundService = response.data.find(
              service => service.id === this.serviceId
            );

            if (foundService) {
              this.service = foundService;
            } else {
              this.error = 'The selected service could not be found.';
            }
          } else {
            this.error =
              response?.message ||
              'Unable to load service details.';
          }

          this.cdr.detectChanges();
        },
        error: error => {
          console.error('BOOKING SERVICE ERROR:', error);

          this.error =
            error?.error?.message ||
            'Unable to load service details right now.';

          this.cdr.detectChanges();
        }
      });
  }

  submitBooking(): void {
    if (this.submitting) {
      return;
    }

    this.error = '';
    this.success = '';

    if (!this.validateForm()) {
      return;
    }

    this.submitting = true;

    const request = {
      providerServiceId: this.serviceId,
      bookingDate: this.bookingDate,
      bookingTime: this.bookingTime,
      address: this.address.trim(),
      city: this.city.trim(),
      state: this.state.trim(),
      pincode: this.pincode.trim(),
      customerNote: this.customerNote.trim() || null,
      paymentPreference: this.paymentPreference
    };

    this.api
      .post<ApiResponse<BookingResponse>>(
        '/bookings',
        request
      )
      .pipe(
        finalize(() => {
          this.submitting = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: response => {
          console.log('BOOKING CREATED:', response);

          if (
            response &&
            response.success &&
            response.data
          ) {
            this.success =
              response.message ||
              'Booking created successfully.';

            this.router.navigate([
              '/bookings',
              response.data.id
            ]);
          } else {
            this.error =
              response?.message ||
              'Unable to create booking.';
          }

          this.cdr.detectChanges();
        },
        error: error => {
          console.error('BOOKING CREATION ERROR:', error);

          this.error =
            error?.error?.message ||
            'Unable to create booking right now. Please try again.';

          this.cdr.detectChanges();
        }
      });
  }

  validateForm(): boolean {
    if (!this.bookingDate) {
      this.error = 'Please select a booking date.';
      return false;
    }

    if (this.bookingDate < this.minDate) {
      this.error = 'Booking date cannot be before today.';
      return false;
    }

    if (!this.bookingTime) {
      this.error = 'Please select a booking time.';
      return false;
    }

    if (!this.address.trim()) {
      this.error = 'Please enter your address.';
      return false;
    }

    if (!this.city.trim()) {
      this.error = 'Please enter your city.';
      return false;
    }

    if (!this.state.trim()) {
      this.error = 'Please enter your state.';
      return false;
    }

    if (!this.pincode.trim()) {
      this.error = 'Please enter your pincode.';
      return false;
    }

    if (!/^\d{6}$/.test(this.pincode.trim())) {
      this.error = 'Please enter a valid 6-digit pincode.';
      return false;
    }

    if (
      this.paymentPreference !== 'PAY_ONLINE' &&
      this.paymentPreference !== 'PAY_AT_SERVICE'
    ) {
      this.error = 'Please select a payment method.';
      return false;
    }

    return true;
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('en-IN', {
      maximumFractionDigits: 0
    }).format(price || 0);
  }

  formatDuration(minutes: number): string {
    if (!minutes || minutes <= 0) {
      return 'Duration varies';
    }

    const hours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60;

    if (hours === 0) {
      return `${remainingMinutes} min`;
    }

    if (remainingMinutes === 0) {
      return `${hours} hr`;
    }

    return `${hours} hr ${remainingMinutes} min`;
  }
}
