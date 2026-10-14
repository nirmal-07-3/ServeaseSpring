import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
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

interface ApiResponse<T> {
  success?: boolean;
  message?: string;
  data?: T;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class Profile implements OnInit {
  profile: UserProfile | null = null;

  profileForm = {
    name: '',
    phone: '',
    profileImage: ''
  };

  loading = true;
  saving = false;

  successMessage = '';
  errorMessage = '';

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
    this.errorMessage = '';

    this.api.get<ApiResponse<UserProfile> | UserProfile>('/users/me')
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
            this.errorMessage = 'Unable to load your profile.';
            return;
          }

          this.profile = data;

          this.profileForm = {
            name: data.name || '',
            phone: data.phone || '',
            profileImage: data.profileImage || ''
          };
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = this.getErrorMessage(
            error,
            'Unable to load your profile.'
          );
        }
      });
  }

  saveProfile(): void {
    this.successMessage = '';
    this.errorMessage = '';

    const name = this.profileForm.name.trim();
    const phone = this.profileForm.phone.trim();
    const profileImage = this.profileForm.profileImage.trim();

    if (!name) {
      this.errorMessage = 'Name is required.';
      return;
    }

    if (name.length < 3) {
      this.errorMessage = 'Name must contain at least 3 characters.';
      return;
    }

    if (!/^[6-9]\d{9}$/.test(phone)) {
      this.errorMessage = 'Enter a valid 10-digit Indian phone number.';
      return;
    }

    const request: UpdateProfileRequest = {
      name,
      phone
    };

    if (profileImage) {
      request.profileImage = profileImage;
    }

    this.saving = true;

    this.api.put<ApiResponse<UserProfile> | UserProfile>(
      '/users/me',
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
          const data = this.extractData(response);

          if (!data) {
            this.errorMessage =
              'Profile was updated, but the response was invalid.';
            return;
          }

          this.profile = data;

          this.profileForm = {
            name: data.name || '',
            phone: data.phone || '',
            profileImage: data.profileImage || ''
          };

          this.auth.updateUserName(data.name);

          this.successMessage = 'Profile updated successfully.';
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = this.getErrorMessage(
            error,
            'Unable to update your profile.'
          );
        }
      });
  }

  getInitials(): string {
    if (!this.profile?.name) {
      return 'U';
    }

    const parts = this.profile.name.trim().split(/\s+/);

    if (parts.length === 1) {
      return parts[0].charAt(0).toUpperCase();
    }

    return (
      parts[0].charAt(0) +
      parts[parts.length - 1].charAt(0)
    ).toUpperCase();
  }

  hasProfileImage(): boolean {
    return !!this.profileForm.profileImage.trim();
  }

  clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }

  private extractData(
    response: ApiResponse<UserProfile> | UserProfile
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
