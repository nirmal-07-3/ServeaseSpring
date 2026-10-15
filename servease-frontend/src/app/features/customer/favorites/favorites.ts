
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { Api } from '../../../core/services/api';

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
  selector: 'app-favorites',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './favorites.html',
  styleUrl: './favorites.css'
})
export class Favorites implements OnInit {
  favorites: FavoriteProvider[] = [];
  loading = false;
  removingProviderId: number | null = null;
  error = '';
  success = '';

  constructor(
    private api: Api,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadFavorites();
  }

  loadFavorites(): void {
    this.loading = true;
    this.error = '';
    this.success = '';

    this.api
      .get<FavoriteProvider[]>('/customer/favorites')
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (response) => {
          this.favorites = Array.isArray(response) ? response : [];
          this.cdr.detectChanges();
        },
        error: (error) => {
          this.favorites = [];
          this.error =
            error?.error?.message ||
            'Unable to load your favourite providers. Please try again.';
          this.cdr.detectChanges();
        }
      });
  }

  removeFavorite(favorite: FavoriteProvider): void {
    if (this.removingProviderId !== null) {
      return;
    }

    this.removingProviderId = favorite.providerId;
    this.error = '';
    this.success = '';

    this.api
      .delete<{ message: string }>(
        `/customer/favorites/${favorite.providerId}`
      )
      .pipe(
        finalize(() => {
          this.removingProviderId = null;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (response) => {
          this.favorites = this.favorites.filter(
            item => item.providerId !== favorite.providerId
          );
          this.success = response?.message || 'Provider removed from favourites.';
          this.cdr.detectChanges();
        },
        error: (error) => {
          this.error =
            error?.error?.message ||
            'Unable to remove this provider. Please try again.';
          this.cdr.detectChanges();
        }
      });
  }

  retry(): void {
    if (!this.loading) {
      this.loadFavorites();
    }
  }

  trackByProviderId(index: number, favorite: FavoriteProvider): number {
    return favorite.providerId;
  }
}
