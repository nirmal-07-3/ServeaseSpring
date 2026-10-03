import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs/operators';


interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}


export interface ProviderApprovalResponse {

  id: number;

  businessName: string;

  description: string | null;

  city: string | null;

  state: string | null;

  country: string | null;

  pincode: string | null;

  experienceYears: number | null;

  verificationStatus: string;

  profileCompleted: boolean;
}


export interface ProviderDocument {

  id: number;

  documentType: string;

  documentNumber: string;

  documentUrl: string;

  verified: boolean;

  uploadedAt: string;
}


export interface ProviderAdminDetails {

  id: number;

  businessName: string;

  description: string | null;

  city: string | null;

  state: string | null;

  country: string | null;

  pincode: string | null;

  experienceYears: number | null;

  latitude: number | null;

  longitude: number | null;

  profileCompleted: boolean;

  averageRating: number | null;

  totalReviews: number | null;

  totalBookings: number | null;

  verificationStatus: string | null;

  documents: ProviderDocument[];
}


@Component({

  selector: 'app-admin-providers',

  standalone: true,

  imports: [
    CommonModule,
    RouterLink
  ],

  templateUrl: './providers.html',

  styleUrl: './providers.css'

})


export class Providers implements OnInit {


  // ============================================================
  // DATA
  // ============================================================

  providers: ProviderApprovalResponse[] = [];

  selectedProvider:
    ProviderAdminDetails | null = null;


  // ============================================================
  // LOADING STATES
  // ============================================================

  loading = false;

  detailsLoading = false;

  providerActionLoading = false;

  documentActionLoadingId:
    number | null = null;


  // ============================================================
  // MESSAGES
  // ============================================================

  errorMessage = '';

  successMessage = '';


  // ============================================================
  // MODALS
  // ============================================================

  showDetails = false;

  showActionModal = false;

  actionType:
    'approve' | 'reject' | null = null;

  providerToAction:
    ProviderApprovalResponse | null = null;


  constructor(

    private http: HttpClient,

    private cdr: ChangeDetectorRef

  ) {}


  // ============================================================
  // INIT
  // ============================================================

  ngOnInit(): void {

    this.loadPendingProviders();

  }


  // ============================================================
  // LOAD PENDING PROVIDERS
  // ============================================================

  loadPendingProviders(): void {

    console.log(
      'ADMIN PROVIDERS: Loading started'
    );


    this.loading = true;

    this.errorMessage = '';

    this.successMessage = '';


    this.cdr.detectChanges();


    this.http

      .get<
        ApiResponse<
          ProviderApprovalResponse[]
        >
      >(
        '/api/admin/providers/pending'
      )

      .pipe(

        finalize(() => {

          console.log(
            'ADMIN PROVIDERS: Loading finalized'
          );


          this.loading = false;


          /*
           * Explicitly trigger Angular view
           * update.
           */
          this.cdr.detectChanges();

        })

      )

      .subscribe({

        next: (response) => {

          console.log(
            'ADMIN PROVIDERS: Response received',
            response
          );


          if (
            response &&
            response.success &&
            Array.isArray(response.data)
          ) {

            console.log(
              'ADMIN PROVIDERS: Providers received =',
              response.data.length
            );


            this.providers =
              response.data;


            console.log(
              'ADMIN PROVIDERS: Providers assigned =',
              this.providers
            );


          } else {

            console.warn(
              'ADMIN PROVIDERS: Invalid response',
              response
            );


            this.providers = [];


            this.errorMessage =
              response?.message ||
              'Unable to load pending providers.';

          }


          /*
           * Force the UI to immediately
           * reflect the received data.
           */
          this.cdr.detectChanges();

        },


        error: (error) => {

          console.error(
            'ADMIN PROVIDERS: LOAD ERROR',
            error
          );


          this.providers = [];


          this.errorMessage =
            error?.error?.message ||
            'Unable to load pending providers. Please try again.';


          this.cdr.detectChanges();

        }

      });

  }


  // ============================================================
  // REFRESH
  // ============================================================

  refreshProviders(): void {

    if (
      this.loading ||
      this.detailsLoading ||
      this.providerActionLoading
    ) {

      return;

    }


    this.loadPendingProviders();

  }


  // ============================================================
  // VIEW PROVIDER DETAILS
  // ============================================================

  viewProvider(
    provider: ProviderApprovalResponse
  ): void {

    if (
      this.detailsLoading ||
      this.providerActionLoading
    ) {

      return;

    }


    this.detailsLoading = true;

    this.errorMessage = '';

    this.selectedProvider = null;

    this.showDetails = true;


    this.cdr.detectChanges();


    this.http

      .get<
        ApiResponse<
          ProviderAdminDetails
        >
      >(
        `/api/admin/providers/${provider.id}`
      )

      .pipe(

        finalize(() => {

          this.detailsLoading = false;

          this.cdr.detectChanges();

        })

      )

      .subscribe({

        next: (response) => {

          console.log(
            'ADMIN PROVIDERS: Details response',
            response
          );


          if (
            response &&
            response.success &&
            response.data
          ) {

            this.selectedProvider =
              response.data;

          } else {

            this.showDetails = false;

            this.errorMessage =
              response?.message ||
              'Unable to load provider details.';

          }


          this.cdr.detectChanges();

        },


        error: (error) => {

          console.error(
            'ADMIN PROVIDERS: DETAILS ERROR',
            error
          );


          this.showDetails = false;


          this.errorMessage =
            error?.error?.message ||
            'Unable to load provider details. Please try again.';


          this.cdr.detectChanges();

        }

      });

  }


  // ============================================================
  // CLOSE DETAILS
  // ============================================================

  closeDetails(): void {

    if (
      this.detailsLoading ||
      this.providerActionLoading
    ) {

      return;

    }


    this.showDetails = false;

    this.selectedProvider = null;


    this.cdr.detectChanges();

  }


  // ============================================================
  // OPEN APPROVE MODAL
  // ============================================================

  openApproveModal(
    provider: ProviderApprovalResponse
  ): void {

    if (
      this.providerActionLoading ||
      provider.verificationStatus !== 'PENDING'
    ) {

      return;

    }


    this.providerToAction = provider;

    this.actionType = 'approve';

    this.showActionModal = true;


    this.cdr.detectChanges();

  }


  // ============================================================
  // OPEN REJECT MODAL
  // ============================================================

  openRejectModal(
    provider: ProviderApprovalResponse
  ): void {

    if (
      this.providerActionLoading ||
      provider.verificationStatus !== 'PENDING'
    ) {

      return;

    }


    this.providerToAction = provider;

    this.actionType = 'reject';

    this.showActionModal = true;


    this.cdr.detectChanges();

  }


  // ============================================================
  // CLOSE ACTION MODAL
  // ============================================================

  closeActionModal(): void {

    if (this.providerActionLoading) {

      return;

    }


    this.showActionModal = false;

    this.actionType = null;

    this.providerToAction = null;


    this.cdr.detectChanges();

  }


  // ============================================================
  // CONFIRM PROVIDER ACTION
  // ============================================================

  confirmProviderAction(): void {

    if (
      !this.providerToAction ||
      !this.actionType ||
      this.providerActionLoading
    ) {

      return;

    }


    const provider =
      this.providerToAction;


    const action =
      this.actionType;


    this.providerActionLoading = true;

    this.errorMessage = '';


    const endpoint =

      action === 'approve'

        ? `/api/admin/providers/${provider.id}/approve`

        : `/api/admin/providers/${provider.id}/reject`;


    this.cdr.detectChanges();


    this.http

      .patch<
        ApiResponse<
          ProviderApprovalResponse
        >
      >(
        endpoint,
        {}
      )

      .pipe(

        finalize(() => {

          this.providerActionLoading = false;

          this.cdr.detectChanges();

        })

      )

      .subscribe({

        next: (response) => {

          if (
            response &&
            response.success
          ) {

            this.successMessage =

              response.message ||

              (
                action === 'approve'

                  ? 'Provider approved successfully.'

                  : 'Provider rejected successfully.'
              );


            this.providers =

              this.providers.filter(

                item =>
                  item.id !== provider.id

              );


            if (
              this.selectedProvider?.id ===
              provider.id
            ) {

              this.showDetails = false;

              this.selectedProvider = null;

            }


            this.showActionModal = false;

            this.actionType = null;

            this.providerToAction = null;


          } else {

            this.errorMessage =
              response?.message ||
              'Unable to update provider status.';

          }


          this.cdr.detectChanges();

        },


        error: (error) => {

          console.error(
            'ADMIN PROVIDERS: ACTION ERROR',
            error
          );


          this.errorMessage =

            error?.error?.message ||

            (
              action === 'approve'

                ? 'Unable to approve provider.'

                : 'Unable to reject provider.'
            );


          this.cdr.detectChanges();

        }

      });

  }


  // ============================================================
  // VERIFY DOCUMENT
  // ============================================================

  verifyDocument(
    document: ProviderDocument
  ): void {

    if (
      document.verified ||
      this.documentActionLoadingId !== null ||
      this.providerActionLoading
    ) {

      return;

    }


    this.documentActionLoadingId =
      document.id;

    this.errorMessage = '';


    this.cdr.detectChanges();


    this.http

      .patch<
        ApiResponse<ProviderDocument>
      >(
        `/api/admin/providers/documents/${document.id}/verify`,
        {}
      )

      .pipe(

        finalize(() => {

          this.documentActionLoadingId =
            null;


          this.cdr.detectChanges();

        })

      )

      .subscribe({

        next: (response) => {

          console.log(
            'ADMIN PROVIDERS: Document verification response',
            response
          );


          if (
            response &&
            response.success &&
            response.data
          ) {

            /*
             * Update the actual object returned
             * by the backend.
             */
            document.verified =
              response.data.verified;


            this.successMessage =

              response.message ||

              'Provider document verified successfully.';

          } else {

            this.errorMessage =
              response?.message ||
              'Unable to verify document.';

          }


          this.cdr.detectChanges();

        },


        error: (error) => {

          console.error(
            'ADMIN PROVIDERS: DOCUMENT VERIFY ERROR',
            error
          );


          this.errorMessage =
            error?.error?.message ||
            'Unable to verify provider document.';


          this.cdr.detectChanges();

        }

      });

  }


  // ============================================================
  // DOCUMENT LABEL
  // ============================================================

  getDocumentLabel(
    documentType: string
  ): string {

    switch (documentType) {

      case 'AADHAR':
        return 'Aadhaar';

      case 'PAN':
        return 'PAN';

      case 'GST':
        return 'GST';

      case 'SHOP_LICENSE':
        return 'Shop License';

      case 'OTHER':
        return 'Other';

      default:
        return documentType || 'Document';

    }

  }


  // ============================================================
  // MASK DOCUMENT NUMBER
  // ============================================================

  maskDocumentNumber(
    documentNumber: string
  ): string {

    if (!documentNumber) {

      return '-';

    }


    if (documentNumber.length <= 4) {

      return documentNumber;

    }


    const visible =
      documentNumber.slice(-4);


    return `•••• ${visible}`;

  }


  // ============================================================
  // STATUS LABEL
  // ============================================================

  getStatusLabel(
    status: string | null
  ): string {

    switch (status) {

      case 'APPROVED':
        return 'Approved';

      case 'REJECTED':
        return 'Rejected';

      case 'SUSPENDED':
        return 'Suspended';

      case 'PENDING':
      default:
        return 'Pending';

    }

  }


  // ============================================================
  // STATUS CLASS
  // ============================================================

  getStatusClass(
    status: string | null
  ): string {

    switch (status) {

      case 'APPROVED':
        return 'approved';

      case 'REJECTED':
        return 'rejected';

      case 'SUSPENDED':
        return 'suspended';

      case 'PENDING':
      default:
        return 'pending';

    }

  }


  // ============================================================
  // FORMAT DATE
  // ============================================================

  formatDate(
    value: string
  ): string {

    if (!value) {

      return '-';

    }


    const date =
      new Date(value);


    if (
      Number.isNaN(
        date.getTime()
      )
    ) {

      return value;

    }


    return date.toLocaleDateString(
      'en-IN',
      {
        day: '2-digit',
        month: 'short',
        year: 'numeric'
      }
    );

  }


  // ============================================================
  // FORMAT RATING
  // ============================================================

  formatRating(
    rating: number | null
  ): string {

    if (
      rating === null ||
      rating === undefined
    ) {

      return '0.0';

    }


    return rating.toFixed(1);

  }

}
