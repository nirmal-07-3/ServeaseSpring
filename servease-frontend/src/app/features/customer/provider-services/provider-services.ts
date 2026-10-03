import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import {
  CommonModule
} from '@angular/common';

import {
  ActivatedRoute,
  RouterLink
} from '@angular/router';

import {
  Api
} from '../../../core/services/api';

import {
  finalize
} from 'rxjs/operators';


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


@Component({

  selector: 'app-provider-services',

  standalone: true,

  imports: [
    CommonModule,
    RouterLink
  ],

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


  constructor(

    private route: ActivatedRoute,

    private api: Api,

    private cdr: ChangeDetectorRef

  ) {}


  ngOnInit(): void {

    this.route.paramMap.subscribe(params => {

      const categoryId =
        Number(params.get('categoryId'));

      const subCategoryId =
        Number(params.get('subCategoryId'));


      if (
        !categoryId ||
        categoryId <= 0 ||
        !subCategoryId ||
        subCategoryId <= 0
      ) {

        this.error =
          'Invalid service selection.';

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

        next: (response) => {

          console.log(
            'PROVIDER SERVICES RESPONSE:',
            response
          );


          if (

            response &&

            response.success &&

            Array.isArray(response.data)

          ) {

            this.services =
              response.data;


            if (
              this.services.length > 0 &&
              this.services[0].subCategoryName
            ) {

              this.subCategoryName =
                this.services[0].subCategoryName;

            }

          }

          else {

            this.error =
              response?.message ||
              'Unable to load available services.';

          }


          this.cdr.detectChanges();

        },


        error: (error) => {

          console.error(
            'PROVIDER SERVICES ERROR:',
            error
          );


          this.services = [];


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

    this.loadServices();

  }


  formatPrice(price: number): string {

    return new Intl.NumberFormat(
      'en-IN',
      {
        maximumFractionDigits: 0
      }
    ).format(price || 0);

  }


  formatDuration(minutes: number): string {

    if (!minutes || minutes <= 0) {

      return 'Duration varies';

    }


    const hours =
      Math.floor(minutes / 60);

    const remainingMinutes =
      minutes % 60;


    if (hours === 0) {

      return `${remainingMinutes} min`;

    }


    if (remainingMinutes === 0) {

      return `${hours} hr`;

    }


    return `${hours} hr ${remainingMinutes} min`;

  }


  formatRadius(radius: number): string {

    if (
      radius === null ||
      radius === undefined
    ) {

      return 'Service area varies';

    }


    return `Up to ${radius} km`;

  }

}
