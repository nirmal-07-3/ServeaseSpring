
import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api } from '../../../core/services/api';
import { Observable, finalize, map } from 'rxjs';

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
  success: boolean;
  message: string;
  data: T;
}

interface FavoriteProvider {
  id: number;
  providerId: number;
  businessName: string;
  description: string | null;
  address: string | null;
  city: string | null;
  averageRating: number | null;
  totalReviews: number | null;
}

@Component({
  selector: 'app-provider-services',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './provider-services.html',
  styleUrl: './provider-services.css'
})
export class ProviderServices implements OnInit {
  categoryId!: number;
  subCategoryId!: number;
  subCategoryName = 'Services';

  services: ProviderService[] = [];
  loading = false;
  error = '';

  favoriteProviderIds = new Set<number>();
  favoritesLoading = false;
  favoriteActionProviderId: number | null = null;
  favoriteError = '';

  constructor(
    private route: ActivatedRoute,
    private api: Api,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadFavorites();

    this.route.paramMap.subscribe(params => {
      const categoryId = Number(params.get('categoryId'));
      const subCategoryId = Number(params.get('subCategoryId'));

      if (
        !categoryId ||
        categoryId <= 0 ||
        !subCategoryId ||
        subCategoryId <= 0
      ) {
        this.error = 'Invalid service selection.';
        this.services = [];
        this.cdr.detectChanges();
        return;
      }

      this.categoryId = categoryId;
      this.subCategoryId = subCategoryId;
      this.loadServices();
    });
  }

  loadServices(): void {
    this.loading = true;
    this.error = '';
    this.services = [];

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
          if (response?.success && Array.isArray(response.data)) {
            this.services = response.data;

            if (
              this.services.length > 0 &&
              this.services[0].subCategoryName
            ) {
              this.subCategoryName =
                this.services[0].subCategoryName;
            }
          } else {
            this.error =
              response?.message ||
              'Unable to load available services.';
          }

          this.cdr.detectChanges();
        },
        error: (error: unknown) => {
          this.services = [];

          const apiError = error as {
            error?: { message?: string };
          };

          this.error =
            apiError?.error?.message ||
            'Unable to load services right now. Please try again.';

          this.cdr.detectChanges();
        }
      });
  }

  loadFavorites(): void {
    this.favoritesLoading = true;
    this.favoriteError = '';

    this.api
      .get<FavoriteProvider[]>('/customer/favorites')
      .pipe(
        finalize(() => {
          this.favoritesLoading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: favorites => {
          this.favoriteProviderIds = new Set(
            Array.isArray(favorites)
              ? favorites.map(favorite => favorite.providerId)
              : []
          );

          this.cdr.detectChanges();
        },
        error: (error: unknown) => {
          const apiError = error as {
            error?: { message?: string };
          };

          this.favoriteError =
            apiError?.error?.message ||
            'Favourite status could not be loaded.';

          this.cdr.detectChanges();
        }
      });
  }

  toggleFavorite(service: ProviderService): void {
    if (
      this.favoritesLoading ||
      this.favoriteActionProviderId !== null
    ) {
      return;
    }

    const providerId = service.providerId;

    if (!providerId || providerId <= 0) {
      this.favoriteError =
        'Provider information is unavailable.';
      return;
    }

    const alreadyFavorite =
      this.favoriteProviderIds.has(providerId);

    this.favoriteActionProviderId = providerId;
    this.favoriteError = '';

    let request: Observable<void>;

    if (alreadyFavorite) {
      request = this.api
        .delete<unknown>(
          `/customer/favorites/${providerId}`
        )
        .pipe(
          map(() => undefined)
        );
    } else {
      request = this.api
        .post<FavoriteProvider>(
          `/customer/favorites/${providerId}`,
          {}
        )
        .pipe(
          map(() => undefined)
        );
    }

    request
      .pipe(
        finalize(() => {
          this.favoriteActionProviderId = null;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: () => {
          const updated = new Set(
            this.favoriteProviderIds
          );

          if (alreadyFavorite) {
            updated.delete(providerId);
          } else {
            updated.add(providerId);
          }

          this.favoriteProviderIds = updated;
          this.cdr.detectChanges();
        },
        error: (error: unknown) => {
          const apiError = error as {
            error?: { message?: string };
          };

          this.favoriteError =
            apiError?.error?.message ||
            'Unable to update favourite. Please try again.';

          this.cdr.detectChanges();
        }
      });
  }

  retry(): void {
    if (!this.loading) {
      this.loadServices();
    }
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

  formatRadius(radius: number): string {
    if (radius === null || radius === undefined) {
      return 'Service area varies';
    }

    return `Up to ${radius} km`;
  }
}
