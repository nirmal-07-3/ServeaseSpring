import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { finalize } from 'rxjs';

import { Api } from '../../../core/services/api';
import { Auth } from '../../../core/services/auth';


interface UserProfile {
  id: number;
  name: string;
  email: string;
  phone: string;
  profileImage?: string;
  role: string;
  status: string;
  emailVerified: boolean;
  phoneVerified: boolean;
}


interface UpdateProfileRequest {
  name: string;
  phone: string;
  profileImage?: string;
}


interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}


interface ApiResponse<T> {
  success?: boolean;
  message?: string;
  data?: T;
}


@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './settings.html',
  styleUrls: ['./settings.css']
})
export class Settings implements OnInit {

  profile: UserProfile | null = null;

  profileForm = {
    name: '',
    phone: '',
    profileImage: ''
  };

  passwordForm: ChangePasswordRequest = {
    oldPassword: '',
    newPassword: '',
    confirmNewPassword: ''
  };

  loading = true;
  savingProfile = false;
  changingPassword = false;

  profileSuccess = '';
  profileError = '';

  passwordSuccess = '';
  passwordError = '';


  constructor(
    private api: Api,
    private auth: Auth,
    private cdr: ChangeDetectorRef
  ) {}


  ngOnInit(): void {
    this.loadProfile();
  }


  loadProfile(): void {

    this.loading = true;
    this.profileError = '';

    this.cdr.detectChanges();

    this.api
      .get<ApiResponse<UserProfile> | UserProfile>('/users/me')
      .pipe(
        finalize(() => {

          this.loading = false;

          this.cdr.detectChanges();

        })
      )
      .subscribe({

        next: (response) => {

          const data = this.extractData(response);

          if (!data) {

            this.profileError =
              'Unable to load your profile.';

            this.cdr.detectChanges();

            return;
          }

          this.profile = data;

          this.profileForm = {
            name: data.name || '',
            phone: data.phone || '',
            profileImage: data.profileImage || ''
          };

          this.cdr.detectChanges();
        },

        error: (error: HttpErrorResponse) => {

          this.profileError =
            this.getErrorMessage(
              error,
              'Unable to load your profile.'
            );

          this.cdr.detectChanges();
        }

      });
  }


  saveProfile(): void {

    this.profileSuccess = '';
    this.profileError = '';

    const name =
      this.profileForm.name.trim();

    const phone =
      this.profileForm.phone.trim();

    const profileImage =
      this.profileForm.profileImage.trim();


    if (!name) {

      this.profileError =
        'Name is required.';

      this.cdr.detectChanges();

      return;
    }


    if (name.length < 3) {

      this.profileError =
        'Name must contain at least 3 characters.';

      this.cdr.detectChanges();

      return;
    }


    if (!/^[6-9]\d{9}$/.test(phone)) {

      this.profileError =
        'Enter a valid 10-digit Indian phone number.';

      this.cdr.detectChanges();

      return;
    }


    const request: UpdateProfileRequest = {
      name,
      phone
    };


    if (profileImage) {

      request.profileImage =
        profileImage;

    }


    this.savingProfile = true;

    this.cdr.detectChanges();


    this.api
      .put<ApiResponse<UserProfile> | UserProfile>(
        '/users/me',
        request
      )
      .pipe(
        finalize(() => {

          this.savingProfile = false;

          this.cdr.detectChanges();

        })
      )
      .subscribe({

        next: (response) => {

          const data =
            this.extractData(response);


          if (!data) {

            this.profileError =
              'Profile was updated, but the response was invalid.';

            this.cdr.detectChanges();

            return;
          }


          this.profile = data;


          this.profileForm = {
            name: data.name || '',
            phone: data.phone || '',
            profileImage: data.profileImage || ''
          };


          this.auth.updateUserName(
            data.name
          );


          this.profileSuccess =
            'Profile updated successfully.';


          this.cdr.detectChanges();
        },


        error: (error: HttpErrorResponse) => {

          this.profileError =
            this.getErrorMessage(
              error,
              'Unable to update your profile.'
            );

          this.cdr.detectChanges();
        }

      });
  }


  changePassword(): void {

    this.passwordSuccess = '';
    this.passwordError = '';


    if (!this.passwordForm.oldPassword) {

      this.passwordError =
        'Enter your current password.';

      this.cdr.detectChanges();

      return;
    }


    if (!this.passwordForm.newPassword) {

      this.passwordError =
        'Enter a new password.';

      this.cdr.detectChanges();

      return;
    }


    if (this.passwordForm.newPassword.length < 8) {

      this.passwordError =
        'New password must contain at least 8 characters.';

      this.cdr.detectChanges();

      return;
    }


    if (!this.passwordForm.confirmNewPassword) {

      this.passwordError =
        'Confirm your new password.';

      this.cdr.detectChanges();

      return;
    }


    if (
      this.passwordForm.newPassword !==
      this.passwordForm.confirmNewPassword
    ) {

      this.passwordError =
        'New passwords do not match.';

      this.cdr.detectChanges();

      return;
    }


    this.changingPassword = true;

    this.cdr.detectChanges();


    this.api
      .put<void>(
        '/users/change-password',
        this.passwordForm
      )
      .pipe(
        finalize(() => {

          this.changingPassword = false;

          this.cdr.detectChanges();

        })
      )
      .subscribe({

        next: () => {

          this.passwordSuccess =
            'Password changed successfully.';


          this.passwordForm = {
            oldPassword: '',
            newPassword: '',
            confirmNewPassword: ''
          };


          this.cdr.detectChanges();
        },


        error: (error: HttpErrorResponse) => {

          this.passwordError =
            this.getErrorMessage(
              error,
              'Unable to change your password.'
            );

          this.cdr.detectChanges();
        }

      });
  }


  getInitials(): string {

    if (!this.profile?.name) {
      return 'U';
    }


    const parts =
      this.profile.name
        .trim()
        .split(/\s+/);


    if (parts.length === 1) {

      return parts[0]
        .charAt(0)
        .toUpperCase();
    }


    return (
      parts[0].charAt(0) +
      parts[parts.length - 1].charAt(0)
    ).toUpperCase();
  }


  clearProfileMessages(): void {

    this.profileSuccess = '';
    this.profileError = '';

    this.cdr.detectChanges();
  }


  clearPasswordMessages(): void {

    this.passwordSuccess = '';
    this.passwordError = '';

    this.cdr.detectChanges();
  }


  private extractData(
    response:
      ApiResponse<UserProfile> |
      UserProfile
  ): UserProfile | null {

    if (
      response &&
      'data' in response &&
      response.data
    ) {

      return response.data;
    }


    if (
      response &&
      'id' in response
    ) {

      return response;
    }


    return null;
  }


  private getErrorMessage(
    error: HttpErrorResponse,
    fallback: string
  ): string {

    if (
      typeof error.error === 'string' &&
      error.error.trim()
    ) {

      return error.error;
    }


    if (error.error?.message) {

      return error.error.message;
    }


    if (error.message) {

      return error.message;
    }


    return fallback;
  }
}
