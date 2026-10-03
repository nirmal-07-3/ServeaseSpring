import {
  Component,
  OnInit,
  ChangeDetectorRef
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs/operators';


interface Category {

  id: number;

  name: string;

}


interface SubCategory {

  id: number;

  categoryId: number;

  categoryName: string;

  name: string;

  description: string | null;

  icon: string | null;

  displayOrder: number | null;

  active: boolean;

}


interface ApiResponse<T> {

  success: boolean;

  message: string;

  data: T;

}


interface CreateServiceRequest {

  subCategoryId: number;

  title: string;

  description: string;

  basePrice: number;

  estimatedDurationMinutes: number;

  serviceRadiusKm: number;

}


@Component({

  selector: 'app-create-service',

  standalone: true,

  imports: [
    CommonModule,
    FormsModule,
    RouterLink
  ],

  templateUrl: './create-service.html',

  styleUrl: './create-service.css'

})
export class CreateService implements OnInit {


  /*
   * IMPORTANT:
   * Start false.
   * Turn true only while categories request is running.
   */
  loadingCategories = false;

  loadingSubCategories = false;

  saving = false;


  errorMessage = '';

  successMessage = '';


  categories: Category[] = [];

  subCategories: SubCategory[] = [];


  selectedCategoryId: number | null = null;

  selectedSubCategoryId: number | null = null;


  service = {

    title: '',

    description: '',

    basePrice: null as number | null,

    estimatedDurationMinutes:
      null as number | null,

    serviceRadiusKm:
      null as number | null

  };


  constructor(

    private http: HttpClient,

    private router: Router,

    private cdr: ChangeDetectorRef

  ) {}


  ngOnInit(): void {

    this.loadCategories();

  }


  loadCategories(): void {

    if (this.loadingCategories) {

      return;

    }


    this.loadingCategories = true;

    this.errorMessage = '';


    console.log(
      'CREATE SERVICE: Loading categories...'
    );


    this.http
      .get<ApiResponse<Category[]>>(
        '/api/categories'
      )
      .pipe(

        finalize(() => {

          /*
           * ALWAYS stop loading.
           */
          this.loadingCategories = false;


          console.log(
            'CREATE SERVICE: Category loading finished'
          );


          this.cdr.detectChanges();

        })

      )
      .subscribe({

        next: (response) => {

          console.log(
            'CREATE SERVICE: Categories response:',
            response
          );


          if (
            response &&
            response.success &&
            Array.isArray(response.data)
          ) {

            this.categories =
              response.data;

          }

          else {

            this.categories = [];

            this.errorMessage =
              response?.message ||
              'Unable to load categories.';

          }


          this.cdr.detectChanges();

        },


        error: (error) => {

          console.error(
            'CREATE SERVICE: Category loading error:',
            error
          );


          this.categories = [];


          this.errorMessage =
            error?.error?.message ||
            'Unable to load categories.';


          this.cdr.detectChanges();

        }

      });

  }


  onCategoryChange(): void {

    this.selectedSubCategoryId = null;

    this.subCategories = [];

    this.errorMessage = '';


    if (!this.selectedCategoryId) {

      this.cdr.detectChanges();

      return;

    }


    if (this.loadingSubCategories) {

      return;

    }


    this.loadingSubCategories = true;


    const categoryId =
      this.selectedCategoryId;


    console.log(
      'CREATE SERVICE: Loading subcategories:',
      categoryId
    );


    this.http
      .get<ApiResponse<SubCategory[]>>(
        `/api/categories/${categoryId}/subcategories`
      )
      .pipe(

        finalize(() => {

          this.loadingSubCategories = false;

          this.cdr.detectChanges();

        })

      )
      .subscribe({

        next: (response) => {

          console.log(
            'CREATE SERVICE: Subcategories response:',
            response
          );


          if (
            response &&
            response.success &&
            Array.isArray(response.data)
          ) {

            this.subCategories =
              response.data.filter(
                subCategory =>
                  subCategory.active
              );

          }

          else {

            this.subCategories = [];

            this.errorMessage =
              response?.message ||
              'Unable to load subcategories.';

          }


          this.cdr.detectChanges();

        },


        error: (error) => {

          console.error(
            'CREATE SERVICE: Subcategory loading error:',
            error
          );


          this.subCategories = [];


          this.errorMessage =
            error?.error?.message ||
            'Unable to load subcategories.';


          this.cdr.detectChanges();

        }

      });

  }


  createService(): void {

    if (this.saving) {

      return;

    }


    this.errorMessage = '';

    this.successMessage = '';


    // Category

    if (!this.selectedCategoryId) {

      this.errorMessage =
        'Please select a category.';

      return;

    }


    // Subcategory

    if (!this.selectedSubCategoryId) {

      this.errorMessage =
        'Please select a subcategory.';

      return;

    }


    // Title

    if (
      !this.service.title ||
      !this.service.title.trim()
    ) {

      this.errorMessage =
        'Please enter a service title.';

      return;

    }


    // Description

    if (
      !this.service.description ||
      !this.service.description.trim()
    ) {

      this.errorMessage =
        'Please enter a service description.';

      return;

    }


    // Price

    if (
      this.service.basePrice === null ||
      this.service.basePrice <= 0
    ) {

      this.errorMessage =
        'Base price must be greater than 0.';

      return;

    }


    // Duration

    if (
      this.service.estimatedDurationMinutes === null ||
      this.service.estimatedDurationMinutes <= 0
    ) {

      this.errorMessage =
        'Estimated duration must be greater than 0.';

      return;

    }


    // Radius

    if (
      this.service.serviceRadiusKm === null ||
      this.service.serviceRadiusKm <= 0
    ) {

      this.errorMessage =
        'Service radius must be greater than 0.';

      return;

    }


    const request: CreateServiceRequest = {

      subCategoryId:
      this.selectedSubCategoryId,

      title:
        this.service.title.trim(),

      description:
        this.service.description.trim(),

      basePrice:
      this.service.basePrice,

      estimatedDurationMinutes:
      this.service.estimatedDurationMinutes,

      serviceRadiusKm:
      this.service.serviceRadiusKm

    };


    this.saving = true;


    console.log(
      'CREATE SERVICE: Request:',
      request
    );


    this.http
      .post<ApiResponse<any>>(
        '/api/provider/services',
        request
      )
      .pipe(

        finalize(() => {

          this.saving = false;

          this.cdr.detectChanges();

        })

      )
      .subscribe({

        next: (response) => {

          console.log(
            'CREATE SERVICE: Response:',
            response
          );


          if (
            response &&
            response.success
          ) {

            this.successMessage =
              'Service created successfully.';


            this.cdr.detectChanges();


            setTimeout(() => {

              this.router.navigate([
                '/provider/services'
              ]);

            }, 700);

          }

          else {

            this.errorMessage =
              response?.message ||
              'Unable to create service.';

          }


          this.cdr.detectChanges();

        },


        error: (error) => {

          console.error(
            'CREATE SERVICE: Create error:',
            error
          );


          this.errorMessage =
            error?.error?.message ||
            'Unable to create service.';


          this.cdr.detectChanges();

        }

      });

  }


  getSelectedSubCategory():
    SubCategory | undefined {

    if (!this.selectedSubCategoryId) {

      return undefined;

    }


    return this.subCategories.find(

      subCategory =>
        subCategory.id ===
        this.selectedSubCategoryId

    );

  }

}
