import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';
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

interface Payment {
  id: number;
  bookingId: number;
  amount: number;
  status:
    | 'PENDING'
    | 'SUCCESS'
    | 'FAILED'
    | 'REFUNDED';
  paymentMethod:
    | 'UPI'
    | 'CARD'
    | 'NET_BANKING'
    | 'CASH';
  transactionId?: string;
  gatewayOrderId?: string;
  createdAt?: string;
  updatedAt?: string;
}

interface RazorpayOrder {
  paymentId: number;
  bookingId: number;
  razorpayOrderId: string;
  razorpayKeyId: string;
  amount: number;
  currency: string;
  status: string;
}

interface RazorpayPaymentResponse {
  razorpay_payment_id: string;
  razorpay_order_id: string;
  razorpay_signature: string;
}

declare global {
  interface Window {
    Razorpay: any;
  }
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

  payment: Payment | null = null;
  paymentLoading = false;
  paymentProcessing = false;
  paymentError = '';
  paymentSuccess = '';

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

          if (this.booking) {
            this.loadPayment(this.booking.id);
          }

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

  loadPayment(bookingId: number): void {

    this.paymentLoading = true;
    this.paymentError = '';

    this.api
      .get<ApiResponse<Payment>>(
        `/payments/booking/${bookingId}`
      )
      .pipe(
        finalize(() => {
          this.paymentLoading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (response) => {

          this.payment =
            response?.data ?? null;
        },

        error: (err) => {

          if (err?.status === 404) {
            this.payment = null;
            return;
          }

          console.error(
            'Failed to load payment:',
            err
          );

          this.payment = null;

          this.paymentError =
            err?.error?.message ||
            'Unable to load payment information.';
        }

      });
  }

  canPay(): boolean {

    return (
      !!this.booking &&
      this.booking.status === 'ACCEPTED' &&
      this.payment?.status !== 'SUCCESS' &&
      !this.paymentProcessing
    );
  }

  isPaymentSuccessful(): boolean {

    return this.payment?.status === 'SUCCESS';
  }

  payNow(): void {

    if (!this.booking || !this.canPay()) {
      return;
    }

    this.paymentProcessing = true;
    this.paymentError = '';
    this.paymentSuccess = '';

    const bookingId = this.booking.id;

    const createPayment$ = this.payment
      ? this.api.get<ApiResponse<Payment>>(
        `/payments/booking/${bookingId}`
      )
      : this.api.post<ApiResponse<Payment>>(
        '/payments',
        {
          bookingId,
          paymentMethod: 'UPI'
        }
      );

    createPayment$
      .pipe(
        finalize(() => {
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (response) => {

          this.payment =
            response?.data ?? this.payment;

          this.createRazorpayOrder(bookingId);
        },

        error: (err) => {

          console.error(
            'Failed to create payment:',
            err
          );

          this.paymentProcessing = false;

          this.paymentError =
            err?.error?.message ||
            'Unable to start the payment.';

          this.cdr.detectChanges();
        }

      });
  }

  private createRazorpayOrder(
    bookingId: number
  ): void {

    this.api
      .post<ApiResponse<RazorpayOrder>>(
        `/payments/razorpay/order/${bookingId}`,
        {}
      )
      .pipe(
        finalize(() => {
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (response) => {

          const order = response?.data;

          if (
            !order?.razorpayOrderId ||
            !order?.razorpayKeyId
          ) {

            this.paymentProcessing = false;

            this.paymentError =
              'Razorpay order information is incomplete.';

            this.cdr.detectChanges();

            return;
          }

          this.openRazorpayCheckout(order);
        },

        error: (err) => {

          console.error(
            'Failed to create Razorpay order:',
            err
          );

          this.paymentProcessing = false;

          this.paymentError =
            err?.error?.message ||
            'Unable to create the Razorpay order.';

          this.cdr.detectChanges();
        }

      });
  }

  private openRazorpayCheckout(
    order: RazorpayOrder
  ): void {

    if (!window.Razorpay) {

      this.paymentProcessing = false;

      this.paymentError =
        'Razorpay Checkout could not be loaded. Please refresh and try again.';

      this.cdr.detectChanges();

      return;
    }

    const options = {

      key: order.razorpayKeyId,

      amount:
        Math.round(order.amount * 100),

      currency:
        order.currency || 'INR',

      name: 'Servease',

      description:
        this.booking?.serviceTitle ||
        'Service Booking',

      order_id:
      order.razorpayOrderId,

      prefill: {},

      theme: {
        color: '#111827'
      },

      modal: {

        ondismiss: () => {

          this.paymentProcessing = false;

          this.paymentError =
            'Payment window was closed. You can try again.';

          this.cdr.detectChanges();
        }

      },

      handler: (
        response: RazorpayPaymentResponse
      ) => {

        this.verifyRazorpayPayment(
          response
        );
      }

    };

    try {

      const razorpay =
        new window.Razorpay(options);

      razorpay.on(
        'payment.failed',
        (response: any) => {

          console.error(
            'Razorpay payment failed:',
            response
          );

          this.paymentProcessing = false;

          this.paymentError =
            response?.error?.description ||
            'Razorpay payment failed. Please try again.';

          this.cdr.detectChanges();
        }
      );

      razorpay.open();

    } catch (err) {

      console.error(
        'Unable to open Razorpay Checkout:',
        err
      );

      this.paymentProcessing = false;

      this.paymentError =
        'Unable to open Razorpay Checkout.';

      this.cdr.detectChanges();
    }
  }

  private verifyRazorpayPayment(
    response: RazorpayPaymentResponse
  ): void {

    this.api
      .post<ApiResponse<Payment>>(
        '/payments/razorpay/verify',
        {
          razorpayPaymentId:
          response.razorpay_payment_id,

          razorpayOrderId:
          response.razorpay_order_id,

          razorpaySignature:
          response.razorpay_signature
        }
      )
      .pipe(
        finalize(() => {

          this.paymentProcessing = false;

          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (verifyResponse) => {

          this.payment =
            verifyResponse?.data ??
            this.payment;

          this.paymentSuccess =
            'Payment completed successfully.';

          this.paymentError = '';

          if (this.booking) {
            this.loadPayment(
              this.booking.id
            );
          }

          this.cdr.detectChanges();
        },

        error: (err) => {

          console.error(
            'Payment verification failed:',
            err
          );

          this.paymentError =
            err?.error?.message ||
            'Payment was completed, but verification failed. Please contact support before paying again.';

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
