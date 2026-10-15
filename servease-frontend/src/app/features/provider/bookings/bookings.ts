import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Subscription, finalize, interval, switchMap, takeWhile, catchError, of } from 'rxjs';

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
  paymentPreference?: string | null;
  effectivePaymentPreference?: string | null;
  paymentDeadlineAt?: string | null;
  onlinePaymentAllowed?: boolean;
  paymentStatus?: string | null;
  paymentReconciliationRequired?: boolean;
}

export interface BookingCollectionResponse {
  bookingId: number;
  collectionId: number;
  amount: number;
  currency: string;
  qrCodeId: string | null;
  gatewayPaymentId: string | null;
  imageUrl: string | null;
  status: string;
  createdAt: string | null;
  expiresAt: string | null;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Component({
  selector: 'app-provider-bookings',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './bookings.html',
  styleUrl: './bookings.css'
})
export class Bookings implements OnInit, OnDestroy {
  bookings: BookingResponse[] = [];
  loading = false;
  actionLoading = false;
  collectionLoading = false;
  statusLoading = false;
  cashLoading = false;

  errorMessage = '';
  successMessage = '';

  selectedBooking: BookingResponse | null = null;
  collectionBooking: BookingResponse | null = null;
  collection: BookingCollectionResponse | null = null;
  collectionError = '';

  verificationPurpose: 'START' | 'COMPLETION' | null = null;
  verificationCode = '';
  verificationLoading = false;
  verificationRequestLoading = false;
  verificationError = '';
  verificationMessage = '';

  private statusPoll?: Subscription;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadBookings();
  }

  ngOnDestroy(): void {
    this.stopStatusPolling();
  }

  loadBookings(): void {
    this.loading = true;
    this.errorMessage = '';

    this.http.get<ApiResponse<BookingResponse[]>>('/api/provider/bookings')
      .pipe(finalize(() => {
        this.loading = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: response => {
          if (response?.success && Array.isArray(response.data)) {
            this.bookings = response.data;

            if (this.selectedBooking) {
              const updated = this.bookings.find(
                booking => booking.id === this.selectedBooking?.id
              );

              if (updated) {
                this.selectedBooking = updated;
              }
            }
          } else {
            this.bookings = [];
            this.errorMessage = response?.message || 'Unable to load bookings.';
          }
        },
        error: error => {
          this.bookings = [];
          this.errorMessage =
            error?.error?.message || 'Failed to load bookings. Please try again.';
        }
      });
  }

  refreshBookings(): void {
    if (this.loading || this.actionLoading) return;
    this.successMessage = '';
    this.loadBookings();
  }

  acceptBooking(booking: BookingResponse): void {
    this.performAction(booking, 'accept', 'Booking accepted successfully.');
  }

  rejectBooking(booking: BookingResponse): void {
    this.performAction(booking, 'reject', 'Booking rejected successfully.');
  }

  startBooking(booking: BookingResponse): void {
    if (booking.status !== BookingStatus.ACCEPTED) return;
    this.openVerification(booking, 'START');
  }

  completeBooking(booking: BookingResponse): void {
    if (booking.status !== BookingStatus.IN_PROGRESS) return;
    this.openVerification(booking, 'COMPLETION');
  }

  openVerification(
    booking: BookingResponse,
    purpose: 'START' | 'COMPLETION'
  ): void {
    this.selectedBooking = booking;
    this.verificationPurpose = purpose;
    this.verificationCode = '';
    this.verificationError = '';
    this.verificationMessage = '';
  }

  requestVerificationCode(): void {
    const booking = this.selectedBooking;
    const purpose = this.verificationPurpose;

    if (
      !booking ||
      !purpose ||
      this.verificationRequestLoading ||
      this.verificationLoading
    ) {
      return;
    }

    this.verificationRequestLoading = true;
    this.verificationError = '';
    this.verificationMessage = '';

    this.http.post<ApiResponse<void>>(
      `/api/provider/bookings/${booking.id}/verification/${purpose}/request`,
      {}
    ).pipe(finalize(() => {
      this.verificationRequestLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: response => {
        if (response?.success) {
          this.verificationMessage =
            response.message || 'Verification code sent to the customer.';
        } else {
          this.verificationError =
            response?.message || 'Unable to request the verification code.';
        }
      },
      error: error => {
        this.verificationError =
          error?.error?.message || 'Unable to request the verification code.';
      }
    });
  }

  verifyBookingCode(): void {
    const booking = this.selectedBooking;
    const purpose = this.verificationPurpose;

    if (
      !booking ||
      !purpose ||
      this.verificationLoading ||
      this.verificationRequestLoading
    ) {
      return;
    }

    if (!/^\d{6}$/.test(this.verificationCode)) {
      this.verificationError = 'Enter the 6-digit verification code.';
      return;
    }

    this.verificationLoading = true;
    this.verificationError = '';
    this.verificationMessage = '';

    this.http.post<ApiResponse<void>>(
      `/api/provider/bookings/${booking.id}/verification/${purpose}/verify`,
      { code: this.verificationCode }
    ).pipe(finalize(() => {
      this.verificationLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: response => {
        if (response?.success) {
          this.successMessage =
            response.message || 'Booking verification successful.';

          this.verificationPurpose = null;
          this.verificationCode = '';
          this.verificationError = '';
          this.verificationMessage = '';
          this.selectedBooking = null;

          this.loadBookings();
        } else {
          this.verificationError =
            response?.message || 'Verification failed.';
        }
      },
      error: error => {
        this.verificationError =
          error?.error?.message ||
          'Verification failed. Check the code and try again.';
      }
    });
  }

  private performAction(
    booking: BookingResponse,
    action: string,
    defaultMessage: string
  ): void {
    if (this.actionLoading) return;

    this.actionLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.http.patch<ApiResponse<void>>(
      `/api/provider/bookings/${booking.id}/${action}`,
      {}
    ).pipe(finalize(() => {
      this.actionLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: response => {
        if (response?.success) {
          this.successMessage = response.message || defaultMessage;
          this.loadBookings();
        } else {
          this.errorMessage =
            response?.message || `Unable to ${action} booking.`;
        }
      },
      error: error => {
        this.errorMessage =
          error?.error?.message ||
          `Failed to ${action} booking. Please try again.`;
      }
    });
  }

  canCollectAtService(booking: BookingResponse): boolean {
    const preference =
      booking.effectivePaymentPreference || booking.paymentPreference;

    return booking.status === BookingStatus.IN_PROGRESS &&
      preference === 'PAY_AT_SERVICE';
  }

  openCollection(booking: BookingResponse): void {
    if (!this.canCollectAtService(booking)) return;

    this.stopStatusPolling();
    this.collectionBooking = booking;
    this.collection = null;
    this.collectionError = '';
    this.successMessage = '';
    this.loadCollectionStatus(booking, false);
  }

  closeCollection(): void {
    this.stopStatusPolling();
    this.collectionBooking = null;
    this.collection = null;
    this.collectionError = '';
  }

  createOnlineQr(): void {
    const booking = this.collectionBooking;

    if (!booking || this.collectionLoading || this.cashLoading) return;

    if (
      this.collection?.qrCodeId &&
      !this.isCollectionConfirmed(this.collection) &&
      !this.isQrExpired(this.collection)
    ) {
      this.collectionError =
        'A QR is already associated with this booking. Refresh its status before trying again.';
      return;
    }

    this.collectionLoading = true;
    this.collectionError = '';

    this.http.post<ApiResponse<BookingCollectionResponse>>(
      `/api/provider/bookings/${booking.id}/collection/online-qr`,
      null
    ).pipe(finalize(() => {
      this.collectionLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: response => {
        if (response?.success && response.data) {
          this.collection = response.data;
          this.successMessage =
            'QR created. Payment is not confirmed until Razorpay reports a captured payment.';
          this.startStatusPolling();
        } else {
          this.collectionError =
            response?.message || 'Unable to create the payment QR.';
        }
      },
      error: error => {
        this.collectionError =
          error?.error?.message ||
          'Unable to create the payment QR. Refresh collection status before retrying.';
      }
    });
  }

  refreshCollectionStatus(): void {
    if (!this.collectionBooking || this.statusLoading) return;
    this.loadCollectionStatus(this.collectionBooking, true);
  }

  private loadCollectionStatus(
    booking: BookingResponse,
    showErrors: boolean
  ): void {
    this.statusLoading = true;

    if (showErrors) this.collectionError = '';

    this.http.get<ApiResponse<BookingCollectionResponse>>(
      `/api/provider/bookings/${booking.id}/collection`
    ).pipe(finalize(() => {
      this.statusLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: response => {
        if (response?.success && response.data) {
          this.collection = response.data;

          if (this.isCollectionConfirmed(response.data)) {
            this.successMessage =
              response.message || 'Collection confirmed by the backend.';
            this.stopStatusPolling();
            this.loadBookings();
          }
        } else if (showErrors) {
          this.collectionError =
            response?.message || 'Unable to retrieve collection status.';
        }
      },
      error: error => {
        if (showErrors) {
          this.collectionError =
            error?.error?.message ||
            'No collection status is available yet. Refresh or retry after checking the booking.';
        }
      }
    });
  }

  private startStatusPolling(): void {
    this.stopStatusPolling();

    const booking = this.collectionBooking;
    if (!booking) return;

    this.statusPoll = interval(10000).pipe(
      switchMap(() =>
        this.http.get<ApiResponse<BookingCollectionResponse>>(
          `/api/provider/bookings/${booking.id}/collection`
        ).pipe(catchError(() => of(null)))
      ),
      takeWhile(response => {
        if (!response?.success || !response.data) return true;

        this.collection = response.data;

        if (this.isCollectionConfirmed(response.data)) {
          this.successMessage = 'Payment collection confirmed by Razorpay.';
          this.loadBookings();
          return false;
        }

        return true;
      }, true),
      finalize(() => this.cdr.detectChanges())
    ).subscribe(() => this.cdr.detectChanges());
  }

  private stopStatusPolling(): void {
    this.statusPoll?.unsubscribe();
    this.statusPoll = undefined;
  }

  confirmCashCollection(): void {
    const booking = this.collectionBooking;

    if (!booking || this.cashLoading || this.collectionLoading) return;

    if (
      this.collection?.qrCodeId &&
      !this.isCollectionConfirmed(this.collection) &&
      !this.isQrExpired(this.collection)
    ) {
      this.collectionError =
        'An online QR is still active. Refresh status and reconcile it before confirming cash.';
      return;
    }

    this.cashLoading = true;
    this.collectionError = '';

    this.http.patch<ApiResponse<void>>(
      `/api/provider/bookings/${booking.id}/collection/confirm`,
      { collectionMethod: 'CASH' }
    ).pipe(finalize(() => {
      this.cashLoading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: response => {
        if (response?.success) {
          this.successMessage = response.message || 'Cash collection confirmed.';
          this.loadCollectionStatus(booking, false);
          this.loadBookings();
        } else {
          this.collectionError =
            response?.message || 'Cash collection could not be confirmed.';
        }
      },
      error: error => {
        this.collectionError =
          error?.error?.message ||
          'Cash collection could not be confirmed. Check the collection status before retrying.';
      }
    });
  }

  isCollectionConfirmed(
    collection: BookingCollectionResponse | null
  ): boolean {
    return (collection?.status || '').toUpperCase() === 'CONFIRMED';
  }

  isQrExpired(collection: BookingCollectionResponse | null): boolean {
    if (!collection?.expiresAt) return false;

    const expiry = new Date(collection.expiresAt).getTime();

    return Number.isFinite(expiry) && Date.now() > expiry;
  }

  getCollectionStatusLabel(
    collection: BookingCollectionResponse | null
  ): string {
    if (!collection) return 'Not checked';
    if (this.isCollectionConfirmed(collection)) return 'Confirmed';

    if (this.isQrExpired(collection) && collection.qrCodeId) {
      return 'QR expired — verify status';
    }

    return (collection.status || 'Pending').replace(/_/g, ' ');
  }

  viewBooking(booking: BookingResponse): void {
    this.selectedBooking = booking;
    this.verificationPurpose = null;
    this.verificationCode = '';
    this.verificationError = '';
    this.verificationMessage = '';
  }

  closeBooking(): void {
    if (this.verificationLoading || this.verificationRequestLoading) return;

    this.selectedBooking = null;
    this.verificationPurpose = null;
    this.verificationCode = '';
    this.verificationError = '';
    this.verificationMessage = '';
  }

  getStatusLabel(status: BookingStatus): string {
    switch (status) {
      case BookingStatus.PENDING: return 'Pending';
      case BookingStatus.ACCEPTED: return 'Accepted';
      case BookingStatus.REJECTED: return 'Rejected';
      case BookingStatus.IN_PROGRESS: return 'In progress';
      case BookingStatus.COMPLETED: return 'Completed';
      case BookingStatus.CANCELLED: return 'Cancelled';
      default: return status;
    }
  }

  getStatusClass(status: BookingStatus): string {
    switch (status) {
      case BookingStatus.PENDING: return 'pending';
      case BookingStatus.ACCEPTED: return 'accepted';
      case BookingStatus.REJECTED: return 'rejected';
      case BookingStatus.IN_PROGRESS: return 'in-progress';
      case BookingStatus.COMPLETED: return 'completed';
      case BookingStatus.CANCELLED: return 'cancelled';
      default: return '';
    }
  }

  canAccept(booking: BookingResponse): boolean {
    return booking.status === BookingStatus.PENDING;
  }

  canReject(booking: BookingResponse): boolean {
    return booking.status === BookingStatus.PENDING;
  }

  canStart(booking: BookingResponse): boolean {
    return booking.status === BookingStatus.ACCEPTED;
  }

  canComplete(booking: BookingResponse): boolean {
    return booking.status === BookingStatus.IN_PROGRESS;
  }

  formatDate(date: string): string {
    if (!date) return '-';

    return new Date(`${date}T00:00:00`).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  }

  formatTime(time: string): string {
    if (!time) return '-';

    const parts = time.split(':');
    if (parts.length < 2) return time;

    const date = new Date();
    date.setHours(Number(parts[0]), Number(parts[1]), 0, 0);

    return date.toLocaleTimeString('en-IN', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('en-IN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(price || 0);
  }

  get totalBookings(): number {
    return this.bookings.length;
  }

  get pendingBookings(): number {
    return this.bookings.filter(
      booking => booking.status === BookingStatus.PENDING
    ).length;
  }

  get acceptedBookings(): number {
    return this.bookings.filter(
      booking => booking.status === BookingStatus.ACCEPTED
    ).length;
  }

  get completedBookings(): number {
    return this.bookings.filter(
      booking => booking.status === BookingStatus.COMPLETED
    ).length;
  }
}
