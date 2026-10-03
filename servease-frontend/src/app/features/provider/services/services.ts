import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';

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

interface ApiResponse<T> {
  success?: boolean;
  message?: string;
  data?: T;
}

@Component({
  selector: 'app-services',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink
  ],
  templateUrl: './services.html',
  styleUrl: './services.css'
})
export class Services implements OnInit {

  services: ProviderService[] = [];

  loading = false;

  // Used while Activate/Deactivate/Delete request is running
  actionLoading = false;

  // Keeps track of which service is currently being changed
  statusLoadingId: number | null = null;
  deleteLoadingId: number | null = null;

  showDeleteModal = false;
  serviceToDelete: ProviderService | null = null;

  errorMessage = '';
  successMessage = '';

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    console.log('SERVICES COMPONENT INITIALIZED');
    this.loadServices();
  }

  // =========================================================
  // LOAD SERVICES
  // =========================================================

  loadServices(): void {

    console.log('SERVICES: REQUEST START');

    this.loading = true;
    this.errorMessage = '';

    this.http
      .get<ApiResponse<ProviderService[]>>(
        '/api/provider/services'
      )
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({

        next: (response) => {

          console.log(
            'SERVICES: RAW RESPONSE =',
            response
          );

          if (Array.isArray(response?.data)) {

            this.services = response.data;

            console.log(
              'SERVICES: FINAL SERVICES =',
              this.services
            );

            console.log(
              'SERVICES: FINAL COUNT =',
              this.services.length
            );

          } else {

            this.services = [];

            this.errorMessage =
              response?.message ||
              'No services were returned by the server.';
          }
        },

        error: (error) => {

          console.error(
            'SERVICES: Loading error:',
            error
          );

          this.services = [];

          this.errorMessage =
            error?.error?.message ||
            'Unable to load your services.';
        }

      });
  }

  // =========================================================
  // STATISTICS
  // =========================================================

  get activeServices(): number {

    return this.services.filter(
      service => service.active === true
    ).length;
  }

  get inactiveServices(): number {

    return this.services.filter(
      service => service.active !== true
    ).length;
  }

  // =========================================================
  // ACTIVATE / DEACTIVATE
  // =========================================================

  updateStatus(service: ProviderService): void {

    // Don't allow another status request while one is running
    if (
      this.statusLoadingId !== null ||
      this.deleteLoadingId !== null
    ) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    const oldStatus = service.active;
    const newStatus = !oldStatus;

    console.log(
      `SERVICES: Changing service ${service.id} status from ${oldStatus} to ${newStatus}`
    );

    this.statusLoadingId = service.id;
    this.actionLoading = true;

    this.http
      .patch<ApiResponse<void>>(
        `/api/provider/services/${service.id}/status`,
        {
          active: newStatus
        }
      )
      .pipe(
        finalize(() => {

          this.statusLoadingId = null;
          this.actionLoading = false;

          this.cdr.detectChanges();

        })
      )
      .subscribe({

        next: (response) => {

          console.log(
            'SERVICES: STATUS RESPONSE =',
            response
          );

          if (response?.success === false) {

            // Keep old value if backend says request failed
            service.active = oldStatus;

            this.errorMessage =
              response.message ||
              'Unable to update service status.';

            return;
          }

          // IMPORTANT:
          // Update the service directly in the current UI.
          // No refresh required.
          service.active = newStatus;

          this.successMessage =
            response?.message ||
            (
              newStatus
                ? 'Service activated successfully.'
                : 'Service deactivated successfully.'
            );

          console.log(
            'SERVICES: UI STATUS UPDATED =',
            service.active
          );
        },

        error: (error) => {

          console.error(
            'SERVICES: STATUS ERROR =',
            error
          );

          // Restore original status if API failed
          service.active = oldStatus;

          this.errorMessage =
            error?.error?.message ||
            'Unable to update service status.';
        }

      });
  }

  // =========================================================
  // DELETE - OPEN MODAL
  // =========================================================

  deleteService(service: ProviderService): void {

    if (
      this.statusLoadingId !== null ||
      this.deleteLoadingId !== null
    ) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.serviceToDelete = service;
    this.showDeleteModal = true;
  }

  // =========================================================
  // CANCEL DELETE
  // =========================================================

  cancelDelete(): void {

    if (this.deleteLoadingId !== null) {
      return;
    }

    this.showDeleteModal = false;
    this.serviceToDelete = null;
  }

  // =========================================================
  // CONFIRM DELETE
  // =========================================================

  confirmDelete(): void {

    if (
      !this.serviceToDelete ||
      this.deleteLoadingId !== null ||
      this.statusLoadingId !== null
    ) {
      return;
    }

    const service = this.serviceToDelete;

    this.errorMessage = '';
    this.successMessage = '';

    this.deleteLoadingId = service.id;
    this.actionLoading = true;

    console.log(
      'SERVICES: DELETE START =',
      service.id
    );

    this.http
      .delete<ApiResponse<void>>(
        `/api/provider/services/${service.id}`
      )
      .pipe(
        finalize(() => {

          this.deleteLoadingId = null;
          this.actionLoading = false;

          this.cdr.detectChanges();

        })
      )
      .subscribe({

        next: (response) => {

          console.log(
            'SERVICES: DELETE RESPONSE =',
            response
          );

          if (response?.success === false) {

            this.errorMessage =
              response.message ||
              'Unable to delete service.';

            return;
          }

          // Remove immediately from UI
          this.services =
            this.services.filter(
              item => item.id !== service.id
            );

          // Close modal
          this.showDeleteModal = false;
          this.serviceToDelete = null;

          // Show success message
          this.successMessage =
            response?.message ||
            `"${service.title}" deleted successfully.`;

          console.log(
            'SERVICES: SERVICE REMOVED FROM UI'
          );
        },

        error: (error) => {

          console.error(
            'SERVICES: DELETE ERROR =',
            error
          );

          this.errorMessage =
            error?.error?.message ||
            'Unable to delete service.';
        }

      });
  }

  // =========================================================
  // HELPERS
  // =========================================================

  formatDuration(minutes: number): string {

    if (!minutes || minutes <= 0) {
      return '0 min';
    }

    const hours =
      Math.floor(minutes / 60);

    const remainingMinutes =
      minutes % 60;

    if (hours === 0) {
      return `${remainingMinutes} min`;
    }

    if (remainingMinutes === 0) {
      return `${hours}h`;
    }

    return `${hours}h ${remainingMinutes}m`;
  }

  formatRadius(radius: number): string {

    if (
      radius === null ||
      radius === undefined
    ) {
      return '0 km';
    }

    return `${radius} km`;
  }
}
