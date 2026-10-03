import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs/operators';

interface RegisterRequest {
  name: string;
  email: string;
  phone: string;
  password: string;
  role: string;
}

interface RegisterResponse {
  success?: boolean;
  message?: string;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink
  ],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {

  name = '';
  email = '';
  phone = '';
  password = '';
  confirmPassword = '';
  role = 'CUSTOMER';

  showPassword = false;
  showConfirmPassword = false;

  loading = false;

  errorMessage = '';
  successMessage = '';

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  register(): void {

    this.errorMessage = '';
    this.successMessage = '';

    if (
      !this.name.trim() ||
      !this.email.trim() ||
      !this.phone.trim() ||
      !this.password ||
      !this.confirmPassword
    ) {
      this.errorMessage = 'Please fill in all required fields.';
      return;
    }

    if (this.password.length < 6) {
      this.errorMessage =
        'Password must be at least 6 characters long.';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage =
        'Passwords do not match.';
      return;
    }

    if (!/^\d{10}$/.test(this.phone.trim())) {
      this.errorMessage =
        'Please enter a valid 10-digit phone number.';
      return;
    }

    const request: RegisterRequest = {
      name: this.name.trim(),
      email: this.email.trim(),
      phone: this.phone.trim(),
      password: this.password,
      role: this.role
    };

    this.loading = true;

    this.http
      .post<RegisterResponse>(
        '/api/auth/register',
        request
      )
      .pipe(
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe({

        next: (response) => {

          this.successMessage =
            response?.message ||
            'Account created successfully. Please sign in.';

          this.name = '';
          this.email = '';
          this.phone = '';
          this.password = '';
          this.confirmPassword = '';
          this.role = 'CUSTOMER';

          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 1200);
        },

        error: (error) => {

          this.errorMessage =
            error?.error?.message ||
            error?.message ||
            'Unable to create your account. Please try again.';
        }

      });
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword =
      !this.showConfirmPassword;
  }
}

