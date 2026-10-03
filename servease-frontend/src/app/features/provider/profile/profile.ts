import {
  Component,
  OnInit,
  ChangeDetectorRef
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

import { Auth } from '../../../core/services/auth';


interface ProviderProfile {

  id: number;

  businessName: string;

  description: string | null;

  experienceYears: number | null;

  verificationStatus: string;

  profileCompleted: boolean;

  averageRating: number;

  totalReviews: number;

  totalBookings: number;

  address: string | null;

  city: string | null;

  state: string | null;

  country: string | null;

  pincode: string | null;

}


interface ApiResponse<T> {

  success: boolean;

  message: string;

  data: T;

}


@Component({

  selector: 'app-profile',

  standalone: true,

  imports: [

    CommonModule,

    FormsModule,

    RouterLink

  ],

  templateUrl: './profile.html',

  styleUrl: './profile.css'

})


export class Profile implements OnInit {


  loading = true;

  saving = false;


  errorMessage = '';

  successMessage = '';


  profile: ProviderProfile = {

    id: 0,

    businessName: '',

    description: '',

    experienceYears: 0,

    verificationStatus: 'PENDING',

    profileCompleted: false,

    averageRating: 0,

    totalReviews: 0,

    totalBookings: 0,

    address: '',

    city: '',

    state: '',

    country: 'India',

    pincode: ''

  };


  constructor(

    private http: HttpClient,

    private auth: Auth,

    private cdr: ChangeDetectorRef

  ) {}


  ngOnInit(): void {

    this.loadProfile();

  }


  // ============================================================
  // LOAD PROFILE
  // ============================================================

  loadProfile(): void {

    this.loading = true;

    this.errorMessage = '';

    this.successMessage = '';


    console.log('PROFILE: Loading started');

    console.log('PROFILE: Calling /api/providers/me');


    this.http
      .get<ApiResponse<ProviderProfile>>(
        '/api/providers/me'
      )
      .subscribe({

        next: (response) => {

          console.log(
            'PROFILE: Response received',
            response
          );


          if (
            response &&
            response.success &&
            response.data
          ) {

            const data = response.data;


            // Safely convert backend null values
            // into values the form can work with.

            this.profile = {

              id: data.id ?? 0,

              businessName:
                data.businessName ?? '',

              description:
                data.description ?? '',

              experienceYears:
                data.experienceYears ?? 0,

              verificationStatus:
                data.verificationStatus ?? 'PENDING',

              profileCompleted:
                data.profileCompleted ?? false,

              averageRating:
                data.averageRating ?? 0,

              totalReviews:
                data.totalReviews ?? 0,

              totalBookings:
                data.totalBookings ?? 0,

              address:
                data.address ?? '',

              city:
                data.city ?? '',

              state:
                data.state ?? '',

              country:
                data.country ?? 'India',

              pincode:
                data.pincode ?? ''

            };


            console.log(
              'PROFILE: Profile loaded',
              this.profile
            );

          } else {

            this.errorMessage =
              response?.message ||
              'Unable to load provider profile.';

          }


          // IMPORTANT
          // Stop the loading screen.

          this.loading = false;


          console.log(
            'PROFILE: Loading finished:',
            this.loading
          );


          // IMPORTANT
          // Force Angular to update the UI.

          this.cdr.detectChanges();

        },


        error: (error) => {

          console.error(
            'PROFILE: Error loading profile',
            error
          );


          this.errorMessage =
            error?.error?.message ||
            'Unable to load your profile.';


          this.loading = false;


          // Force UI update even when request fails.

          this.cdr.detectChanges();

        },


        complete: () => {

          console.log(
            'PROFILE: HTTP request completed'
          );

        }

      });

  }


  // ============================================================
  // SAVE PROFILE
  // ============================================================

  saveProfile(): void {

    this.errorMessage = '';

    this.successMessage = '';


    // Business name

    if (
      !this.profile.businessName ||
      !this.profile.businessName.trim()
    ) {

      this.errorMessage =
        'Please enter your business name.';

      return;

    }


    // Description

    if (
      !this.profile.description ||
      !this.profile.description.trim()
    ) {

      this.errorMessage =
        'Please enter a description.';

      return;

    }


    // Experience

    if (
      this.profile.experienceYears === null ||
      this.profile.experienceYears < 0
    ) {

      this.errorMessage =
        'Please enter valid experience.';

      return;

    }


    // Address

    if (
      !this.profile.address ||
      !this.profile.address.trim()
    ) {

      this.errorMessage =
        'Please enter your address.';

      return;

    }


    // City

    if (
      !this.profile.city ||
      !this.profile.city.trim()
    ) {

      this.errorMessage =
        'Please enter your city.';

      return;

    }


    // State

    if (
      !this.profile.state ||
      !this.profile.state.trim()
    ) {

      this.errorMessage =
        'Please enter your state.';

      return;

    }


    // Pincode

    if (
      !this.profile.pincode ||
      !this.profile.pincode.trim()
    ) {

      this.errorMessage =
        'Please enter your pincode.';

      return;

    }


    this.saving = true;


    const request = {

      businessName:
        this.profile.businessName.trim(),

      description:
        this.profile.description.trim(),

      experienceYears:
      this.profile.experienceYears,

      address:
        this.profile.address.trim(),

      city:
        this.profile.city.trim(),

      state:
        this.profile.state.trim(),

      pincode:
        this.profile.pincode.trim(),

      latitude: null,

      longitude: null

    };


    console.log(
      'PROFILE: Updating profile',
      request
    );


    this.http
      .put<ApiResponse<ProviderProfile>>(
        '/api/providers/me',
        request
      )
      .subscribe({

        next: (response) => {

          console.log(
            'PROFILE: Update response',
            response
          );


          this.saving = false;


          if (
            response &&
            response.success &&
            response.data
          ) {

            const data = response.data;


            this.profile = {

              id: data.id ?? 0,

              businessName:
                data.businessName ?? '',

              description:
                data.description ?? '',

              experienceYears:
                data.experienceYears ?? 0,

              verificationStatus:
                data.verificationStatus ?? 'PENDING',

              profileCompleted:
                data.profileCompleted ?? false,

              averageRating:
                data.averageRating ?? 0,

              totalReviews:
                data.totalReviews ?? 0,

              totalBookings:
                data.totalBookings ?? 0,

              address:
                data.address ?? '',

              city:
                data.city ?? '',

              state:
                data.state ?? '',

              country:
                data.country ?? 'India',

              pincode:
                data.pincode ?? ''

            };


            // ====================================================
            // UPDATE LOGGED-IN USER NAME
            // ====================================================
            // The dashboard reads the name from Auth/localStorage.
            // Keep it synchronized with the updated business name.

            this.auth.updateUserName(
              data.businessName ??
              this.profile.businessName
            );


            this.successMessage =
              'Profile updated successfully.';


            console.log(
              'PROFILE: Update successful',
              this.profile
            );

          } else {

            this.errorMessage =
              response?.message ||
              'Unable to update profile.';

          }


          this.cdr.detectChanges();

        },


        error: (error) => {

          console.error(
            'PROFILE: Update error',
            error
          );


          this.saving = false;


          this.errorMessage =
            error?.error?.message ||
            'Unable to update your profile.';


          this.cdr.detectChanges();

        }

      });

  }


  // ============================================================
  // VERIFICATION LABEL
  // ============================================================

  getVerificationLabel(): string {

    switch (
      this.profile.verificationStatus
      ) {

      case 'APPROVED':

        return 'Verified';


      case 'REJECTED':

        return 'Rejected';


      case 'PENDING':

      default:

        return 'Pending verification';

    }

  }

}
