import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';
import { FormsModule } from '@angular/forms';
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
  description: string;
  icon: string;
  image: string;
  displayOrder: number;
  active: boolean;
}

interface Subcategory {
  id: number;
  categoryId: number;
  categoryName: string;
  name: string;
  description: string;
  icon: string;
  displayOrder: number;
  active: boolean;
}

interface CategoryForm {
  name: string;
  description: string;
  icon: string;
  image: string;
  displayOrder: number;
  active: boolean;
}

interface SubcategoryForm {
  categoryId: number | null;
  name: string;
  description: string;
  icon: string;
  displayOrder: number;
  active: boolean;
}

type CatalogTab = 'categories' | 'subcategories';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './categories.html',
  styleUrl: './categories.css'
})
export class Categories implements OnInit {

  activeTab: CatalogTab = 'categories';

  categories: Category[] = [];
  subcategories: Subcategory[] = [];

  loadingCategories = false;
  loadingSubcategories = false;

  savingCategory = false;
  savingSubcategory = false;

  uploadingImage = false;

  deletingCategoryId: number | null = null;
  deletingSubcategoryId: number | null = null;

  errorMessage = '';
  successMessage = '';

  categorySearchTerm = '';
  subcategorySearchTerm = '';

  selectedCategoryFilter: number | null = null;

  showCategoryModal = false;
  showSubcategoryModal = false;

  isEditingCategory = false;
  isEditingSubcategory = false;

  selectedCategoryId: number | null = null;
  selectedSubcategoryId: number | null = null;

  selectedImageFile: File | null = null;
  imagePreview = '';

  categoryForm: CategoryForm = this.getEmptyCategoryForm();

  subcategoryForm: SubcategoryForm =
    this.getEmptySubcategoryForm();

  constructor(
    private api: Api,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadCategories();
  }

  // ============================================================
  // FORM FACTORIES
  // ============================================================

  private getEmptyCategoryForm(): CategoryForm {
    return {
      name: '',
      description: '',
      icon: '',
      image: '',
      displayOrder: 0,
      active: true
    };
  }

  private getEmptySubcategoryForm(): SubcategoryForm {
    return {
      categoryId: null,
      name: '',
      description: '',
      icon: '',
      displayOrder: 0,
      active: true
    };
  }

  // ============================================================
  // TAB SWITCHING
  // ============================================================

  switchTab(tab: CatalogTab): void {
    if (this.activeTab === tab) {
      return;
    }

    this.clearMessages();

    this.activeTab = tab;

    if (
      tab === 'subcategories' &&
      this.subcategories.length === 0
    ) {
      this.loadSubcategories();
    }

    this.cdr.detectChanges();
  }

  // ============================================================
  // CATEGORY API
  // ============================================================

  loadCategories(): void {
    this.loadingCategories = true;
    this.errorMessage = '';

    this.cdr.detectChanges();

    this.api
      .get<ApiResponse<Category[]>>('/admin/categories')
      .pipe(
        finalize(() => {
          this.loadingCategories = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (response) => {
          if (response?.success) {
            this.categories = Array.isArray(response.data)
              ? response.data
              : [];
          } else {
            this.categories = [];

            this.errorMessage =
              response?.message ||
              'Failed to load categories.';
          }

          this.cdr.detectChanges();
        },

        error: (error) => {
          this.categories = [];

          this.errorMessage =
            error?.error?.message ||
            error?.message ||
            'Failed to load categories.';

          this.cdr.detectChanges();
        }
      });
  }

  get filteredCategories(): Category[] {
    const search =
      this.categorySearchTerm.trim().toLowerCase();

    if (!search) {
      return this.categories;
    }

    return this.categories.filter(category => {
      const name = category.name || '';
      const description = category.description || '';

      return (
        name.toLowerCase().includes(search) ||
        description.toLowerCase().includes(search)
      );
    });
  }

  openCreateCategory(): void {
    this.clearMessages();

    this.isEditingCategory = false;
    this.selectedCategoryId = null;

    this.categoryForm = this.getEmptyCategoryForm();

    this.selectedImageFile = null;
    this.imagePreview = '';

    this.showCategoryModal = true;

    this.cdr.detectChanges();
  }

  openEditCategory(category: Category): void {
    this.clearMessages();

    this.isEditingCategory = true;
    this.selectedCategoryId = category.id;

    this.categoryForm = {
      name: category.name || '',
      description: category.description || '',
      icon: category.icon || '',
      image: category.image || '',
      displayOrder: category.displayOrder ?? 0,
      active: category.active ?? true
    };

    this.selectedImageFile = null;
    this.imagePreview =
      this.getImageUrl(category.image);

    this.showCategoryModal = true;

    this.cdr.detectChanges();
  }

  closeCategoryModal(): void {
    if (
      this.savingCategory ||
      this.uploadingImage
    ) {
      return;
    }

    this.showCategoryModal = false;
    this.selectedImageFile = null;
    this.imagePreview = '';

    this.cdr.detectChanges();
  }

  saveCategory(): void {
    this.clearMessages();

    if (!this.categoryForm.name.trim()) {
      this.errorMessage =
        'Category name is required.';

      this.cdr.detectChanges();
      return;
    }

    if (
      this.savingCategory ||
      this.uploadingImage
    ) {
      return;
    }

    this.savingCategory = true;

    const request = {
      name: this.categoryForm.name.trim(),
      description:
        this.categoryForm.description.trim(),
      icon: this.categoryForm.icon.trim(),
      image: this.categoryForm.image || '',
      displayOrder:
        Number(this.categoryForm.displayOrder),
      active: this.categoryForm.active
    };

    this.cdr.detectChanges();

    if (
      this.isEditingCategory &&
      this.selectedCategoryId !== null
    ) {
      this.updateCategory(request);
    } else {
      this.createCategory(request);
    }
  }

  private createCategory(request: {
    name: string;
    description: string;
    icon: string;
    image: string;
    displayOrder: number;
    active: boolean;
  }): void {

    const createRequest = {
      name: request.name,
      description: request.description,
      icon: request.icon,
      image: request.image,
      displayOrder: request.displayOrder
    };

    this.api
      .post<ApiResponse<Category>>(
        '/admin/categories',
        createRequest
      )
      .pipe(
        finalize(() => {
          this.savingCategory = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (response) => {
          this.showCategoryModal = false;

          this.successMessage =
            response?.message ||
            'Category created successfully.';

          this.cdr.detectChanges();

          this.loadCategories();
        },

        error: (error) => {
          this.errorMessage =
            error?.error?.message ||
            error?.message ||
            'Failed to create category.';

          this.cdr.detectChanges();
        }
      });
  }

  private updateCategory(request: {
    name: string;
    description: string;
    icon: string;
    image: string;
    displayOrder: number;
    active: boolean;
  }): void {

    this.api
      .put<ApiResponse<Category>>(
        `/admin/categories/${this.selectedCategoryId}`,
        request
      )
      .pipe(
        finalize(() => {
          this.savingCategory = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (response) => {
          this.showCategoryModal = false;

          this.successMessage =
            response?.message ||
            'Category updated successfully.';

          this.cdr.detectChanges();

          this.loadCategories();
        },

        error: (error) => {
          this.errorMessage =
            error?.error?.message ||
            error?.message ||
            'Failed to update category.';

          this.cdr.detectChanges();
        }
      });
  }

  deleteCategory(category: Category): void {
    this.clearMessages();

    const confirmed = window.confirm(
      `Delete "${category.name}"?`
    );

    if (!confirmed) {
      return;
    }

    this.deletingCategoryId = category.id;

    this.cdr.detectChanges();

    this.api
      .delete<ApiResponse<void>>(
        `/admin/categories/${category.id}`
      )
      .pipe(
        finalize(() => {
          this.deletingCategoryId = null;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (response) => {
          this.successMessage =
            response?.message ||
            'Category deleted successfully.';

          this.loadCategories();
        },

        error: (error) => {
          this.errorMessage =
            error?.error?.message ||
            error?.message ||
            'Failed to delete category.';
        }
      });
  }

  // ============================================================
  // CATEGORY IMAGE
  // ============================================================

  onImageSelected(event: Event): void {
    const input =
      event.target as HTMLInputElement;

    if (
      !input.files ||
      input.files.length === 0
    ) {
      return;
    }

    const file = input.files[0];

    if (!file.type.startsWith('image/')) {
      this.errorMessage =
        'Please select a valid image file.';

      input.value = '';

      this.cdr.detectChanges();
      return;
    }

    this.selectedImageFile = file;

    const reader = new FileReader();

    reader.onload = () => {
      this.imagePreview =
        reader.result as string;

      this.cdr.detectChanges();
    };

    reader.readAsDataURL(file);

    this.uploadCategoryImage(file);
  }

  private uploadCategoryImage(file: File): void {
    this.clearMessages();

    this.uploadingImage = true;

    const formData = new FormData();
    formData.append('file', file);

    this.cdr.detectChanges();

    this.api
      .post<ApiResponse<string>>(
        '/admin/categories/image',
        formData
      )
      .pipe(
        finalize(() => {
          this.uploadingImage = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (response) => {
          if (
            response?.success &&
            response.data
          ) {
            this.categoryForm.image =
              response.data;

            this.successMessage =
              response.message ||
              'Image uploaded successfully.';
          } else {
            this.errorMessage =
              response?.message ||
              'Image upload failed.';
          }

          this.cdr.detectChanges();
        },

        error: (error) => {
          this.categoryForm.image = '';
          this.selectedImageFile = null;

          this.errorMessage =
            error?.error?.message ||
            error?.message ||
            'Failed to upload category image.';

          this.cdr.detectChanges();
        }
      });
  }

  // ============================================================
  // SUBCATEGORY API
  // ============================================================

  loadSubcategories(): void {
    this.loadingSubcategories = true;
    this.errorMessage = '';

    this.cdr.detectChanges();

    this.api
      .get<ApiResponse<Subcategory[]>>(
        '/admin/subcategories'
      )
      .pipe(
        finalize(() => {
          this.loadingSubcategories = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (response) => {
          if (response?.success) {
            this.subcategories =
              Array.isArray(response.data)
                ? response.data
                : [];
          } else {
            this.subcategories = [];

            this.errorMessage =
              response?.message ||
              'Failed to load subcategories.';
          }

          this.cdr.detectChanges();
        },

        error: (error) => {
          this.subcategories = [];

          this.errorMessage =
            error?.error?.message ||
            error?.message ||
            'Failed to load subcategories.';

          this.cdr.detectChanges();
        }
      });
  }

  get filteredSubcategories(): Subcategory[] {
    const search =
      this.subcategorySearchTerm
        .trim()
        .toLowerCase();

    return this.subcategories.filter(
      subcategory => {

        const matchesSearch =
          !search ||
          (subcategory.name || '')
            .toLowerCase()
            .includes(search) ||
          (subcategory.description || '')
            .toLowerCase()
            .includes(search) ||
          (subcategory.categoryName || '')
            .toLowerCase()
            .includes(search);

        const matchesCategory =
          this.selectedCategoryFilter === null ||
          subcategory.categoryId ===
          this.selectedCategoryFilter;

        return (
          matchesSearch &&
          matchesCategory
        );
      }
    );
  }

  openCreateSubcategory(): void {
    this.clearMessages();

    this.isEditingSubcategory = false;
    this.selectedSubcategoryId = null;

    this.subcategoryForm =
      this.getEmptySubcategoryForm();

    this.showSubcategoryModal = true;

    this.cdr.detectChanges();
  }

  openEditSubcategory(
    subcategory: Subcategory
  ): void {

    this.clearMessages();

    this.isEditingSubcategory = true;
    this.selectedSubcategoryId =
      subcategory.id;

    this.subcategoryForm = {
      categoryId: subcategory.categoryId,
      name: subcategory.name || '',
      description:
        subcategory.description || '',
      icon: subcategory.icon || '',
      displayOrder:
        subcategory.displayOrder ?? 0,
      active:
        subcategory.active ?? true
    };

    this.showSubcategoryModal = true;

    this.cdr.detectChanges();
  }

  closeSubcategoryModal(): void {
    if (this.savingSubcategory) {
      return;
    }

    this.showSubcategoryModal = false;

    this.cdr.detectChanges();
  }

  saveSubcategory(): void {
    this.clearMessages();

    if (
      this.subcategoryForm.categoryId === null
    ) {
      this.errorMessage =
        'Please select a category.';

      this.cdr.detectChanges();
      return;
    }

    if (!this.subcategoryForm.name.trim()) {
      this.errorMessage =
        'Subcategory name is required.';

      this.cdr.detectChanges();
      return;
    }

    if (this.savingSubcategory) {
      return;
    }

    this.savingSubcategory = true;

    const request = {
      categoryId:
      this.subcategoryForm.categoryId,

      name:
        this.subcategoryForm.name.trim(),

      description:
        this.subcategoryForm.description.trim(),

      icon:
        this.subcategoryForm.icon.trim(),

      displayOrder:
        Number(
          this.subcategoryForm.displayOrder
        ),

      active:
      this.subcategoryForm.active
    };

    this.cdr.detectChanges();

    if (
      this.isEditingSubcategory &&
      this.selectedSubcategoryId !== null
    ) {
      this.updateSubcategory(request);
    } else {
      this.createSubcategory(request);
    }
  }

  private createSubcategory(request: {
    categoryId: number;
    name: string;
    description: string;
    icon: string;
    displayOrder: number;
    active: boolean;
  }): void {

    const createRequest = {
      categoryId: request.categoryId,
      name: request.name,
      description: request.description,
      icon: request.icon,
      displayOrder: request.displayOrder
    };

    this.api
      .post<ApiResponse<Subcategory>>(
        '/admin/subcategories',
        createRequest
      )
      .pipe(
        finalize(() => {
          this.savingSubcategory = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (response) => {
          this.showSubcategoryModal = false;

          this.successMessage =
            response?.message ||
            'Subcategory created successfully.';

          this.cdr.detectChanges();

          this.loadSubcategories();
        },

        error: (error) => {
          this.errorMessage =
            error?.error?.message ||
            error?.message ||
            'Failed to create subcategory.';

          this.cdr.detectChanges();
        }
      });
  }

  private updateSubcategory(request: {
    categoryId: number;
    name: string;
    description: string;
    icon: string;
    displayOrder: number;
    active: boolean;
  }): void {

    this.api
      .put<ApiResponse<Subcategory>>(
        `/admin/subcategories/${this.selectedSubcategoryId}`,
        request
      )
      .pipe(
        finalize(() => {
          this.savingSubcategory = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (response) => {
          this.showSubcategoryModal = false;

          this.successMessage =
            response?.message ||
            'Subcategory updated successfully.';

          this.cdr.detectChanges();

          this.loadSubcategories();
        },

        error: (error) => {
          this.errorMessage =
            error?.error?.message ||
            error?.message ||
            'Failed to update subcategory.';

          this.cdr.detectChanges();
        }
      });
  }

  deleteSubcategory(
    subcategory: Subcategory
  ): void {

    this.clearMessages();

    const confirmed = window.confirm(
      `Delete "${subcategory.name}"?`
    );

    if (!confirmed) {
      return;
    }

    this.deletingSubcategoryId =
      subcategory.id;

    this.cdr.detectChanges();

    this.api
      .delete<ApiResponse<void>>(
        `/admin/subcategories/${subcategory.id}`
      )
      .pipe(
        finalize(() => {
          this.deletingSubcategoryId = null;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (response) => {
          this.successMessage =
            response?.message ||
            'Subcategory deleted successfully.';

          this.loadSubcategories();
        },

        error: (error) => {
          this.errorMessage =
            error?.error?.message ||
            error?.message ||
            'Failed to delete subcategory.';
        }
      });
  }

  // ============================================================
  // HELPERS
  // ============================================================

  getImageUrl(
    imagePath: string | null | undefined
  ): string {

    if (!imagePath) {
      return '';
    }

    if (
      imagePath.startsWith('http://') ||
      imagePath.startsWith('https://')
    ) {
      return imagePath;
    }

    return `http://localhost:8080/uploads/${imagePath}`;
  }

  getIconText(
    icon: string | null | undefined
  ): string {
    return icon || '◈';
  }

  trackByCategoryId(
    index: number,
    category: Category
  ): number {
    return category.id;
  }

  trackBySubcategoryId(
    index: number,
    subcategory: Subcategory
  ): number {
    return subcategory.id;
  }

  clearCategoryFilter(): void {
    this.selectedCategoryFilter = null;
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
