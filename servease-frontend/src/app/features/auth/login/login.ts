import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import {
  Auth,
  LoginRequest
} from '../../../core/services/auth';

@Component({
  selector: 'app-login',
  standalone: true,

  imports: [
    CommonModule,
    FormsModule,
    RouterLink
  ],

  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {

  email = '';
  password = '';

  showPassword = false;
  loading = false;

  errorMessage = '';
  successMessage = '';

  constructor(
    private auth: Auth,
    private router: Router
  ) {}

  login(): void {

    // Clear previous messages
    this.errorMessage = '';
    this.successMessage = '';

    // Validate email
    if (!this.email.trim()) {
      this.errorMessage = 'Please enter your email.';
      return;
    }

    // Validate password
    if (!this.password) {
      this.errorMessage = 'Please enter your password.';
      return;
    }

    this.loading = true;

    const request: LoginRequest = {
      email: this.email.trim(),
      password: this.password
    };

    this.auth.login(request).subscribe({

      next: (response) => {

        console.log('Login response:', response);

        if (response.success && response.data?.token) {

          console.log('STEP 1: Login successful');

          // Save token and user information
          this.auth.saveAuthentication(response.data);

          console.log('STEP 2: Authentication saved');

          this.successMessage = 'Login successful!';

          this.loading = false;

          console.log('STEP 3: Loading stopped');

          // Navigate to home
          setTimeout(() => {

            const role = response.data.role;

            switch (role) {

              case 'CUSTOMER':
                this.router.navigate(['/home']);
                break;

              case 'PROVIDER':
                this.router.navigate(['/provider/dashboard']);
                break;

              case 'ADMIN':
                this.router.navigate(['/admin/dashboard']);
                break;

              default:
                this.auth.logout();
                this.errorMessage = 'Invalid user role.';
                break;
            }

          }, 500);

        } else {

          this.loading = false;

          this.errorMessage =
            response.message || 'Login failed.';
        }
      },

      error: (error) => {

        this.loading = false;

        console.error('Login error:', error);

        this.errorMessage =
          error?.error?.message ||
          'Unable to login. Please check your email and password.';
      }
    });
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }
}
