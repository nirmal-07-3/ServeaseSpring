import {
  Component,
  OnInit,
  ChangeDetectorRef
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import { HttpClient } from '@angular/common/http';

import {
  ActivatedRoute,
  Router,
  RouterLink
} from '@angular/router';

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
  selector: 'app-edit-service',

  standalone: true,

  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink
  ],

  templateUrl: './edit-service.html',

  styleUrl: './edit-service.css'
})
export class EditService implements OnInit {

  serviceId!: number;

  service: ProviderService | null = null;

  serviceForm!: FormGroup;

  loading = false;

  saving = false;

  errorMessage = '';

  successMessage = '';


  constructor(
    private fb: FormBuilder,

    private http: HttpClient,

    private route: ActivatedRoute,

    private router: Router,

    private cdr: ChangeDetectorRef
  ) {}


  ngOnInit(): void {

    console.log(
      'EDIT SERVICE: COMPONENT INITIALIZED'
    );


    /*
     * Create form first.
     */

    this.serviceForm = this.fb.group({

      title: [
        '',
        [
          Validators.required,
          Validators.minLength(2),
          Validators.maxLength(100)
        ]
      ],

      description: [
        '',
        [
          Validators.required,
          Validators.minLength(10),
          Validators.maxLength(1000)
        ]
      ],

      basePrice: [
        '',
        [
          Validators.required,
          Validators.min(1)
        ]
      ],

      estimatedDurationMinutes: [
        '',
        [
          Validators.required,
          Validators.min(1)
        ]
      ],

      serviceRadiusKm: [
        '',
        [
          Validators.required,
          Validators.min(1)
        ]
      ]

    });


    /*
     * Get service ID from URL.
     *
     * Example:
     * /provider/services/edit/2
     */

    const id =
      this.route.snapshot.paramMap.get('id');


    console.log(
      'EDIT SERVICE: URL ID =',
      id
    );


    if (!id) {

      this.errorMessage =
        'Service ID was not provided.';

      this.loading = false;

      this.cdr.detectChanges();

      return;
    }


    this.serviceId = Number(id);


    if (Number.isNaN(this.serviceId)) {

      this.errorMessage =
        'Invalid service ID.';

      this.loading = false;

      this.cdr.detectChanges();

      return;
    }


    this.loadService();
  }


  loadService(): void {

    console.log(
      'EDIT SERVICE: REQUEST START',
      this.serviceId
    );


    this.loading = true;

    this.errorMessage = '';

    this.service = null;

    this.cdr.detectChanges();


    this.http
      .get<ApiResponse<ProviderService>>(
        `/api/provider/services/${this.serviceId}`
      )

      .pipe(

        /*
         * IMPORTANT:
         * Loading must ALWAYS stop,
         * even if something goes wrong.
         */

        finalize(() => {

          this.loading = false;

          this.cdr.detectChanges();

          console.log(
            'EDIT SERVICE: REQUEST FINISHED'
          );

        })

      )

      .subscribe({

        next: (response) => {

          console.log(
            'EDIT SERVICE: RAW RESPONSE =',
            response
          );


          /*
           * Validate API response.
           */

          if (
            !response ||
            response.success === false ||
            !response.data
          ) {

            this.errorMessage =
              response?.message ||
              'Unable to load service.';

            this.cdr.detectChanges();

            return;
          }


          /*
           * Store service.
           */

          this.service = response.data;


          console.log(
            'EDIT SERVICE: SERVICE =',
            this.service
          );


          /*
           * Populate form.
           */

          this.serviceForm.patchValue({

            title:
            this.service.title,

            description:
            this.service.description,

            basePrice:
            this.service.basePrice,

            estimatedDurationMinutes:
            this.service.estimatedDurationMinutes,

            serviceRadiusKm:
            this.service.serviceRadiusKm

          });


          console.log(
            'EDIT SERVICE: FORM VALUE =',
            this.serviceForm.value
          );


          /*
           * IMPORTANT for your Angular setup:
           * explicitly update the UI.
           */

          this.cdr.detectChanges();

        },


        error: (error) => {

          console.error(
            'EDIT SERVICE: LOAD ERROR =',
            error
          );


          this.service = null;


          this.errorMessage =
            error?.error?.message ||
            'Unable to load service.';


          this.cdr.detectChanges();

        }

      });
  }


  saveChanges(): void {

    if (this.saving) {
      return;
    }


    this.errorMessage = '';

    this.successMessage = '';


    /*
     * Validate form.
     */

    if (this.serviceForm.invalid) {

      this.serviceForm.markAllAsTouched();

      this.errorMessage =
        'Please correct the highlighted fields.';

      this.cdr.detectChanges();

      return;
    }


    this.saving = true;

    this.cdr.detectChanges();


    /*
     * Build update request.
     */

    const request = {

      title:
      this.serviceForm.value.title,

      description:
      this.serviceForm.value.description,

      basePrice:
        Number(
          this.serviceForm.value.basePrice
        ),

      estimatedDurationMinutes:
        Number(
          this.serviceForm.value
            .estimatedDurationMinutes
        ),

      serviceRadiusKm:
        Number(
          this.serviceForm.value
            .serviceRadiusKm
        )

    };


    console.log(
      'EDIT SERVICE: UPDATE REQUEST =',
      request
    );


    this.http
      .put<ApiResponse<ProviderService>>(
        `/api/provider/services/${this.serviceId}`,
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
            'EDIT SERVICE: UPDATE RESPONSE =',
            response
          );


          if (
            !response ||
            response.success === false
          ) {

            this.errorMessage =
              response?.message ||
              'Unable to update service.';

            this.cdr.detectChanges();

            return;
          }


          /*
           * Update local service data.
           */

          if (response.data) {

            this.service =
              response.data;

          }


          this.successMessage =
            'Service updated successfully.';


          this.cdr.detectChanges();


          /*
           * Return to My Services.
           */

          setTimeout(() => {

            this.router.navigate(
              ['/provider/services'],
              {
                state: {
                  successMessage:
                    'Service updated successfully.'
                }
              }
            );

          }, 700);

        },


        error: (error) => {

          console.error(
            'EDIT SERVICE: UPDATE ERROR =',
            error
          );


          this.errorMessage =
            error?.error?.message ||
            'Unable to update service.';


          this.cdr.detectChanges();

        }

      });
  }


  isInvalid(controlName: string): boolean {

    const control =
      this.serviceForm.get(controlName);


    return !!(
      control &&
      control.invalid &&
      control.touched
    );
  }


  get title() {

    return this.serviceForm.get(
      'title'
    );
  }


  get description() {

    return this.serviceForm.get(
      'description'
    );
  }


  get basePrice() {

    return this.serviceForm.get(
      'basePrice'
    );
  }


  get estimatedDurationMinutes() {

    return this.serviceForm.get(
      'estimatedDurationMinutes'
    );
  }


  get serviceRadiusKm() {

    return this.serviceForm.get(
      'serviceRadiusKm'
    );
  }

}
