import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

interface PlatformSettings {
  platformFeeRate: number;
  providerResponseTimeoutMinutes: number;
  onlinePaymentTimeoutMinutes: number;
  updatedAt: string | null;
}

@Component({
  selector: 'app-admin-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.html',
  styleUrl: './settings.css'
})
export class AdminSettings implements OnInit {
  private readonly apiUrl = 'http://localhost:8080/api/admin/settings';

  settings: PlatformSettings = {
    platformFeeRate: 10,
    providerResponseTimeoutMinutes: 15,
    onlinePaymentTimeoutMinutes: 30,
    updatedAt: null
  };

  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private readonly http: HttpClient,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    this.loading = true;
    this.errorMessage = '';

    this.http.get<PlatformSettings>(this.apiUrl)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: response => {
          this.settings = {
            platformFeeRate: response.platformFeeRate,
            providerResponseTimeoutMinutes:
            response.providerResponseTimeoutMinutes,
            onlinePaymentTimeoutMinutes:
            response.onlinePaymentTimeoutMinutes,
            updatedAt: response.updatedAt
          };
        },
        error: error => {
          this.errorMessage =
            error.error?.message ||
            'Unable to load platform settings. Please try again.';
        }
      });
  }

  saveSettings(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (
      this.settings.platformFeeRate < 0 ||
      this.settings.platformFeeRate > 100 ||
      this.settings.providerResponseTimeoutMinutes < 1 ||
      this.settings.providerResponseTimeoutMinutes > 10080 ||
      this.settings.onlinePaymentTimeoutMinutes < 1 ||
      this.settings.onlinePaymentTimeoutMinutes > 10080
    ) {
      this.errorMessage = 'Please enter valid settings before saving.';
      return;
    }

    if (
      !Number.isFinite(Number(this.settings.platformFeeRate)) ||
      !Number.isFinite(Number(this.settings.providerResponseTimeoutMinutes)) ||
      !Number.isFinite(Number(this.settings.onlinePaymentTimeoutMinutes)) ||
      !Number.isInteger(Number(this.settings.providerResponseTimeoutMinutes)) ||
      !Number.isInteger(Number(this.settings.onlinePaymentTimeoutMinutes))
    ) {
      this.errorMessage =
        'Enter a valid fee rate and whole-number timeout values.';
      return;
    }

    this.saving = true;

    const payload = {
      platformFeeRate: Number(this.settings.platformFeeRate),
      providerResponseTimeoutMinutes:
        Number(this.settings.providerResponseTimeoutMinutes),
      onlinePaymentTimeoutMinutes:
        Number(this.settings.onlinePaymentTimeoutMinutes)
    };

    this.http.put<PlatformSettings>(this.apiUrl, payload)
      .pipe(
        finalize(() => {
          this.saving = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: response => {
          this.settings = {
            platformFeeRate: response.platformFeeRate,
            providerResponseTimeoutMinutes:
            response.providerResponseTimeoutMinutes,
            onlinePaymentTimeoutMinutes:
            response.onlinePaymentTimeoutMinutes,
            updatedAt: response.updatedAt
          };

          this.successMessage = 'Platform settings saved successfully.';
        },
        error: error => {
          this.errorMessage =
            error.error?.message ||
            'Unable to save platform settings. Please try again.';
        }
      });
  }
}
