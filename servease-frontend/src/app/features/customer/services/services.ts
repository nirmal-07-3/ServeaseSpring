import {
  Component,
  OnInit,
  ChangeDetectorRef
} from '@angular/core';

import {
  CommonModule
} from '@angular/common';

import {
  RouterLink
} from '@angular/router';

import {
  HttpClient
} from '@angular/common/http';

import {
  finalize
} from 'rxjs/operators';


interface Category {
  id: number;
  name: string;
  description: string;
  icon: string;
  image: string;
  displayOrder: number;
  active: boolean;
}


interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
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

  categories: Category[] = [];

  loading = false;

  error = '';


  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}


  ngOnInit(): void {

    console.log(
      'CUSTOMER SERVICES: COMPONENT INITIALIZED'
    );

    this.loadCategories();

  }


  loadCategories(): void {

    console.log(
      'CUSTOMER SERVICES: REQUEST START'
    );

    this.loading = true;
    this.error = '';

    this.cdr.detectChanges();


    this.http

      .get<ApiResponse<Category[]>>(
        '/api/categories'
      )

      .pipe(

        finalize(() => {

          console.log(
            'CUSTOMER SERVICES: FINALIZE'
          );

          this.loading = false;

          this.cdr.detectChanges();

          console.log(
            'CUSTOMER SERVICES: LOADING =',
            this.loading
          );

        })

      )

      .subscribe({

        next: (response) => {

          console.log(
            'CUSTOMER SERVICES: RESPONSE =',
            response
          );


          if (
            response &&
            response.success &&
            Array.isArray(response.data)
          ) {

            this.categories =
              response.data;

            console.log(
              'CUSTOMER SERVICES: CATEGORY COUNT =',
              this.categories.length
            );

          }

          else {

            this.categories = [];

            this.error =
              response?.message ||
              'Unable to load service categories.';

          }


          this.cdr.detectChanges();

        },


        error: (error) => {

          console.error(
            'CUSTOMER SERVICES: ERROR =',
            error
          );


          this.categories = [];

          this.error =
            error?.error?.message ||
            'Unable to load services right now. Please try again.';


          this.cdr.detectChanges();

        }

      });

  }


  retry(): void {

    if (this.loading) {
      return;
    }

    this.loadCategories();

  }

}
