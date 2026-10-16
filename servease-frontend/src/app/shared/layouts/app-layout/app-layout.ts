import { Component } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

import { Sidebar } from '../../components/sidebar/sidebar';
import { Auth } from '../../../core/services/auth';
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, Sidebar],
  templateUrl: './app-layout.html',
  styleUrl: './app-layout.css'
})
export class AppLayout {
  sidebarOpen = false;
  userName = 'User';
  userRole = 'CUSTOMER';
  currentUrl = '';

  constructor(
    private auth: Auth,
    private router: Router
  ) {
    const user = this.auth.getUser();

    if (user?.name) {
      this.userName = user.name;
    }

    this.userRole = (this.auth.getRole() || 'CUSTOMER').toUpperCase();
    this.currentUrl = this.router.url;

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(event => {
        this.currentUrl = (event as NavigationEnd).urlAfterRedirects;
        this.closeSidebar();
      });
  }

  get isProviderRoute(): boolean {
    return this.currentUrl.startsWith('/provider/');
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
  }

  getUserInitials(): string {
    if (!this.userName.trim()) {
      return 'U';
    }

    const parts = this.userName.trim().split(/\s+/);

    if (parts.length === 1) {
      return parts[0].charAt(0).toUpperCase();
    }

    return (
      parts[0].charAt(0) +
      parts[parts.length - 1].charAt(0)
    ).toUpperCase();
  }
}
