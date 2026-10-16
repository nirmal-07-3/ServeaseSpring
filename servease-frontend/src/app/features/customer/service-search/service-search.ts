
import { CommonModule } from '@angular/common';
import { HttpParams } from '@angular/common/http';
import {
  ChangeDetectorRef,
  Component,
  NgZone,
  OnInit
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { Api } from '../../../core/services/api';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

interface Category {
  id: number;
  name: string;
}

interface SubCategory {
  id: number;
  name: string;
}

interface ServiceItem {
  id: number;
  title: string;
  description: string;
  basePrice: number;
  estimatedDurationMinutes: number;
  providerId: number;
  providerBusinessName: string;
  subCategoryId: number;
  subCategoryName: string;
  active: boolean;
  serviceRadiusKm: number;
}

@Component({
  selector: 'app-service-search',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './service-search.html',
  styleUrl: './service-search.css'
})
export class ServiceSearch implements OnInit {
  categories: Category[] = [];
  subcategories: SubCategory[] = [];
  services: ServiceItem[] = [];

  keyword = '';
  selectedCategoryId = '';
  selectedSubCategoryId = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  sort = 'newest';

  loading = false;
  categoriesLoading = false;
  subcategoriesLoading = false;

  errorMessage = '';
  categoryError = '';
  subcategoryError = '';

  private latestSearchId = 0;
  private latestSubcategoryRequestId = 0;

  constructor(
    private api: Api,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    this.searchServices();
  }

  private refreshView(): void {
    this.cdr.detectChanges();
  }

  loadCategories(): void {
    this.categoriesLoading = true;
    this.categoryError = '';
    this.refreshView();

    this.api.get<ApiResponse<Category[]>>('/categories')
      .pipe(
        finalize(() => {
          this.ngZone.run(() => {
            this.categoriesLoading = false;
            this.refreshView();
          });
        })
      )
      .subscribe({
        next: (response) => {
          this.ngZone.run(() => {
            this.categories = response.data ?? [];
            this.refreshView();
          });
        },
        error: () => {
          this.ngZone.run(() => {
            this.categoryError =
              'Unable to load categories. Please try again.';
            this.refreshView();
          });
        }
      });
  }

  onCategoryChange(): void {
    this.selectedSubCategoryId = '';
    this.subcategories = [];
    this.subcategoryError = '';

    const categoryId = Number(this.selectedCategoryId);
    const requestId = ++this.latestSubcategoryRequestId;

    if (!categoryId) {
      this.subcategoriesLoading = false;
      this.searchServices();
      this.refreshView();
      return;
    }

    this.subcategoriesLoading = true;
    this.refreshView();

    this.api.get<ApiResponse<SubCategory[]>>(
      `/categories/${categoryId}/subcategories`
    )
      .pipe(
        finalize(() => {
          this.ngZone.run(() => {
            if (requestId === this.latestSubcategoryRequestId) {
              this.subcategoriesLoading = false;
              this.refreshView();
            }
          });
        })
      )
      .subscribe({
        next: (response) => {
          this.ngZone.run(() => {
            if (requestId !== this.latestSubcategoryRequestId) {
              return;
            }

            this.subcategories = response.data ?? [];
            this.refreshView();
          });
        },
        error: () => {
          this.ngZone.run(() => {
            if (requestId !== this.latestSubcategoryRequestId) {
              return;
            }

            this.subcategoryError =
              'Unable to load subcategories. Please try again.';
            this.refreshView();
          });
        }
      });

    this.searchServices();
  }

  searchServices(): void {
    const requestId = ++this.latestSearchId;

    this.loading = true;
    this.errorMessage = '';
    this.refreshView();

    let params = new HttpParams().set('sort', this.sort);

    const trimmedKeyword = this.keyword.trim();

    if (trimmedKeyword) {
      params = params.set('q', trimmedKeyword);
    }

    if (this.selectedCategoryId) {
      params = params.set('categoryId', this.selectedCategoryId);
    }

    if (this.selectedSubCategoryId) {
      params = params.set('subCategoryId', this.selectedSubCategoryId);
    }

    if (this.minPrice !== null && this.minPrice !== undefined) {
      params = params.set('minPrice', String(this.minPrice));
    }

    if (this.maxPrice !== null && this.maxPrice !== undefined) {
      params = params.set('maxPrice', String(this.maxPrice));
    }

    this.api.get<ApiResponse<ServiceItem[]>>(
      '/services/search',
      params
    )
      .pipe(
        finalize(() => {
          this.ngZone.run(() => {
            if (requestId === this.latestSearchId) {
              this.loading = false;
              this.refreshView();
            }
          });
        })
      )
      .subscribe({
        next: (response) => {
          this.ngZone.run(() => {
            if (requestId !== this.latestSearchId) {
              return;
            }

            this.services = response.data ?? [];
            this.errorMessage = '';
            this.refreshView();
          });
        },
        error: () => {
          this.ngZone.run(() => {
            if (requestId !== this.latestSearchId) {
              return;
            }

            this.services = [];
            this.errorMessage =
              'Unable to search services. Please try again.';
            this.refreshView();
          });
        }
      });
  }

  resetFilters(): void {
    this.keyword = '';
    this.selectedCategoryId = '';
    this.selectedSubCategoryId = '';
    this.subcategories = [];
    this.minPrice = null;
    this.maxPrice = null;
    this.sort = 'newest';
    this.subcategoryError = '';

    ++this.latestSubcategoryRequestId;

    this.searchServices();
    this.refreshView();
  }

  trackByServiceId(index: number, service: ServiceItem): number {
    return service.id;
  }
}
