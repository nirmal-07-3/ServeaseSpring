import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards/auth-guard';

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
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./shared/layouts/app-layout/app-layout')
        .then(m => m.AppLayout),
    children: [
      {
        path: 'home',
        canActivate: [roleGuard],
        data: { roles: ['CUSTOMER'] },
        loadComponent: () =>
          import('./features/customer/dashboard/dashboard')
            .then(m => m.Dashboard)
      },
      {
        path: 'services',
        canActivate: [roleGuard],
        data: { roles: ['CUSTOMER'] },
        loadComponent: () =>
          import('./features/customer/services/services')
            .then(m => m.Services)
      },
      {
        path: 'services/:categoryId',
        canActivate: [roleGuard],
        data: { roles: ['CUSTOMER'] },
        loadComponent: () =>
          import('./features/customer/category-services/category-services')
            .then(m => m.CategoryServices)
      },
      {
        path: 'services/:categoryId/:subCategoryId',
        canActivate: [roleGuard],
        data: { roles: ['CUSTOMER'] },
        loadComponent: () =>
          import('./features/customer/provider-services/provider-services')
            .then(m => m.ProviderServices)
      },
      {
        path: 'services/:categoryId/:subCategoryId/:serviceId',
        canActivate: [roleGuard],
        data: { roles: ['CUSTOMER'] },
        loadComponent: () =>
          import('./features/customer/service-details/service-details')
            .then(m => m.ServiceDetails)
      },
      {
        path: 'services/:categoryId/:subCategoryId/:serviceId/book',
        canActivate: [roleGuard],
        data: { roles: ['CUSTOMER'] },
        loadComponent: () =>
          import('./features/customer/booking/booking')
            .then(m => m.Booking)
      },
      {
        path: 'bookings',
        canActivate: [roleGuard],
        data: { roles: ['CUSTOMER'] },
        loadComponent: () =>
          import('./features/customer/my-bookings/my-bookings')
            .then(m => m.MyBookings)
      },
      {
        path: 'bookings/:id',
        canActivate: [roleGuard],
        data: { roles: ['CUSTOMER'] },
        loadComponent: () =>
          import('./features/customer/booking-details/booking-details')
            .then(m => m.BookingDetails)
      },
      {
        path: 'settings',
        canActivate: [roleGuard],
        data: { roles: ['CUSTOMER'] },
        loadComponent: () =>
          import('./features/customer/settings/settings')
            .then(m => m.Settings)
      },
      {
        path: 'profile',
        canActivate: [roleGuard],
        data: { roles: ['CUSTOMER'] },
        loadComponent: () =>
          import('./features/customer/profile/profile')
            .then(m => m.Profile)
      },
      {
        path: 'customer/messages',
        canActivate: [roleGuard],
        data: { roles: ['CUSTOMER'] },
        loadComponent: () =>
          import('./features/customer/messages/messages')
            .then(m => m.Messages)
      },
      {
        path: 'favorites',
        canActivate: [roleGuard],
        data: { roles: ['CUSTOMER'] },
        loadComponent: () =>
          import('./features/customer/favorites/favorites')
            .then(m => m.Favorites)
      },
      {
        path: 'service-search',
        canActivate: [roleGuard],
        data: { roles: ['CUSTOMER'] },
        loadComponent: () =>
          import('./features/customer/service-search/service-search')
            .then(m => m.ServiceSearch)
      },
      {
        path: 'admin/dashboard',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () =>
          import('./features/admin/dashboard/dashboard')
            .then(m => m.Dashboard)
      },
      {
        path: 'admin/providers',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () =>
          import('./features/admin/providers/providers')
            .then(m => m.Providers)
      },
      {
        path: 'admin/categories',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () =>
          import('./features/admin/categories/categories')
            .then(m => m.Categories)
      },
      {
        path: 'provider/dashboard',
        canActivate: [roleGuard],
        data: { roles: ['PROVIDER'] },
        loadComponent: () =>
          import('./features/provider/dashboard/dashboard')
            .then(m => m.Dashboard)
      },
      {
        path: 'provider/profile',
        canActivate: [roleGuard],
        data: { roles: ['PROVIDER'] },
        loadComponent: () =>
          import('./features/provider/profile/profile')
            .then(m => m.Profile)
      },
      {
        path: 'provider/availability',
        canActivate: [roleGuard],
        data: { roles: ['PROVIDER'] },
        loadComponent: () =>
          import('./features/provider/availability/availability')
            .then(m => m.Availability)
      },
      {
        path: 'provider/messages',
        canActivate: [roleGuard],
        data: { roles: ['PROVIDER'] },
        loadComponent: () =>
          import('./features/provider/messages/messages')
            .then(m => m.Messages)
      },
      {
        path: 'provider/services',
        canActivate: [roleGuard],
        data: { roles: ['PROVIDER'] },
        loadComponent: () =>
          import('./features/provider/services/services')
            .then(m => m.Services)
      },
      {
        path: 'provider/services/create',
        canActivate: [roleGuard],
        data: { roles: ['PROVIDER'] },
        loadComponent: () =>
          import('./features/provider/services/create-service/create-service')
            .then(m => m.CreateService)
      },
      {
        path: 'provider/services/edit/:id',
        canActivate: [roleGuard],
        data: { roles: ['PROVIDER'] },
        loadComponent: () =>
          import('./features/provider/services/edit-service/edit-service')
            .then(m => m.EditService)
      },
      {
        path: 'provider/bookings',
        canActivate: [roleGuard],
        data: { roles: ['PROVIDER'] },
        loadComponent: () =>
          import('./features/provider/bookings/bookings')
            .then(m => m.Bookings)
      },
      {
        path: 'provider/verification',
        canActivate: [roleGuard],
        data: { roles: ['PROVIDER'] },
        loadComponent: () =>
          import('./features/provider/verification/verification')
            .then(m => m.Verification)
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
