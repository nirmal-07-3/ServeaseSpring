
import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { Api } from '../../../core/services/api';
import { Auth } from '../../../core/services/auth';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

interface AdminAnalytics {
  totalUsers: number;
  totalProviders: number;
  totalBookings: number;
  platformRevenue: number;
  pendingProviderVerifications: number;
}

interface Stat {
  label: string;
  value: string;
  icon: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  adminName = 'Admin';
  analytics: AdminAnalytics | null = null;
  loading = false;
  errorMessage = '';

  constructor(
    private api: Api,
    private auth: Auth,
    private cdr: ChangeDetectorRef
  ) {
    const user = this.auth.getUser();

    if (user?.name) {
      this.adminName = user.name;
    }
  }

  ngOnInit(): void {
    this.loadAnalytics();
  }

  get stats(): Stat[] {
    const data = this.analytics;

    return [
      {
        label: 'Total Users',
        value: data ? this.formatNumber(data.totalUsers) : this.getUnavailableValue(),
        icon: '◉'
      },
      {
        label: 'Providers',
        value: data ? this.formatNumber(data.totalProviders) : this.getUnavailableValue(),
        icon: '♙'
      },
      {
        label: 'Total Bookings',
        value: data ? this.formatNumber(data.totalBookings) : this.getUnavailableValue(),
        icon: '▣'
      },
      {
        label: 'Platform Revenue',
        value: data ? this.formatCurrency(data.platformRevenue) : this.getUnavailableValue(),
        icon: '₹'
      }
    ];
  }

  get pendingProviders(): number {
    return this.analytics?.pendingProviderVerifications ?? 0;
  }

  loadAnalytics(): void {
    if (this.loading) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.api.get<ApiResponse<AdminAnalytics>>('/admin/analytics/overview')
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (response) => {
          const data = response?.data;

          if (
            response?.success &&
            data &&
            typeof data.totalUsers === 'number' &&
            typeof data.totalProviders === 'number' &&
            typeof data.totalBookings === 'number' &&
            typeof data.platformRevenue === 'number' &&
            typeof data.pendingProviderVerifications === 'number'
          ) {
            this.analytics = data;
            this.errorMessage = '';
          } else {
            this.analytics = null;
            this.errorMessage =
              response?.message || 'The analytics response was invalid.';
          }

          this.cdr.markForCheck();
        },
        error: (error) => {
          this.analytics = null;
          this.errorMessage =
            error?.error?.message ||
            'Unable to load dashboard analytics. Please try again.';
          this.cdr.markForCheck();
        }
      });
  }

  formatNumber(value: number): string {
    return new Intl.NumberFormat('en-IN').format(value);
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2
    }).format(value);
  }

  getAdminInitials(): string {
    if (!this.adminName.trim()) {
      return 'A';
    }

    const parts = this.adminName.trim().split(/\s+/);

    if (parts.length === 1) {
      return parts[0].charAt(0).toUpperCase();
    }

    return (
      parts[0].charAt(0) +
      parts[parts.length - 1].charAt(0)
    ).toUpperCase();
  }

  private getUnavailableValue(): string {
    return this.loading ? 'Loading…' : '—';
  }
}
