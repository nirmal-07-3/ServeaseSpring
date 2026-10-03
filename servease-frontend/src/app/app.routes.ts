
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';


export const routes: Routes = [

  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },

  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login')
        .then(m => m.Login)
  },

  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register')
        .then(m => m.Register)
  },

  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password')
        .then(m => m.ForgotPassword)
  },

  {
    path: 'verify-otp',
    loadComponent: () =>
      import('./features/auth/verify-otp/verify-otp')
        .then(m => m.VerifyOtp)
  },

  {
    path: 'reset-password',
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password')
        .then(m => m.ResetPassword)
  },

  {
    path: 'home',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/customer/dashboard/dashboard')
        .then(m => m.Dashboard)
  },

  {
    path: 'admin/dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/admin/dashboard/dashboard')
        .then(m => m.Dashboard)
  },

  {
    path: 'provider/dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/provider/dashboard/dashboard')
        .then(m => m.Dashboard)
  },
  {
    path: 'provider/profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/provider/profile/profile')
        .then(m => m.Profile)
  },
  {
    path: 'provider/availability',
    loadComponent: () =>
      import('./features/provider/availability/availability')
        .then(m => m.Availability),
    canActivate: [authGuard]
  },
  {
    path: 'provider/services',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/provider/services/services')
        .then(m => m.Services)
  },

  {
    path: 'provider/services/create',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/provider/services/create-service/create-service')
        .then(m => m.CreateService)
  },

  {
    path: 'provider/services/edit/:id',
    loadComponent: () =>
      import('./features/provider/services/edit-service/edit-service')
        .then(m => m.EditService)
  },

  {
    path: 'provider/bookings',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/provider/bookings/bookings')
        .then(m => m.Bookings)
  },

  {
    path: 'provider/verification',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/provider/verification/verification')
        .then(m => m.Verification)
  },


  {
    path: 'services',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/customer/services/services')
        .then(m => m.Services)
  },

  {
    path: 'services/:categoryId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/customer/category-services/category-services')
        .then(m => m.CategoryServices)
  },

  {
    path: 'services/:categoryId/:subCategoryId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/customer/provider-services/provider-services')
        .then(m => m.ProviderServices)
  },

  {
    path: 'services/:categoryId/:subCategoryId/:serviceId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/customer/service-details/service-details')
        .then(m => m.ServiceDetails)
  },

  {
    path: 'services/:categoryId/:subCategoryId/:serviceId/book',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/customer/booking/booking')
        .then(m => m.Booking)
  },

  {
    path: 'bookings',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/customer/my-bookings/my-bookings')
        .then(m => m.MyBookings)
  },

  {
    path: 'bookings/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/customer/booking-details/booking-details')
        .then(m => m.BookingDetails)
  },

  {
    path: 'admin/providers',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/admin/providers/providers')
        .then(m => m.Providers)
  },

  {
    path: 'admin/categories',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/admin/categories/categories')
        .then(m => m.Categories)
  },





  {
    path: '**',
    redirectTo: 'login'
  }

];
